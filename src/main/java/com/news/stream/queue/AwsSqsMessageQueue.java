package com.news.stream.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * AWS SQS 기반 메시지 큐 구현체
 * 프로덕션 환경에서 사용할 수 있는 확장 가능한 메시지 큐
 */
@Component
@Profile("aws-sqs")
public class AwsSqsMessageQueue implements MessageQueue<NewsMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(AwsSqsMessageQueue.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    private final int maxReceiveCount;
    
    public AwsSqsMessageQueue(
            SqsClient sqsClient,
            @Value("${queue.aws.sqs.queue-url}") String queueUrl,
            @Value("${queue.aws.sqs.max-receive-count:3}") int maxReceiveCount) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.maxReceiveCount = maxReceiveCount;
        this.objectMapper = new ObjectMapper();
        
        // LocalDateTime 직렬화 지원
        this.objectMapper.findAndRegisterModules();
    }
    
    @Override
    public void enqueue(NewsMessage message) throws InterruptedException {
        try {
            String messageBody = objectMapper.writeValueAsString(message);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(createMessageAttributes(message))
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(request);
            logger.debug("메시지가 SQS에 전송되었습니다: messageId={}, newsId={}", 
                    response.messageId(), message.newsId());
                    
        } catch (JsonProcessingException e) {
            logger.error("메시지 직렬화 실패: {}", message, e);
            throw new RuntimeException("메시지 직렬화 실패", e);
        } catch (SqsException e) {
            logger.error("SQS 메시지 전송 실패: {}", message, e);
            throw new RuntimeException("SQS 메시지 전송 실패", e);
        }
    }
    
    @Override
    public NewsMessage dequeue() throws InterruptedException {
        return dequeue(0, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public NewsMessage dequeue(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds((int) unit.toSeconds(timeout))
                    .messageAttributeNames("All")
                    .build();
            
            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            
            if (response.messages().isEmpty()) {
                return null;
            }
            
            Message sqsMessage = response.messages().get(0);
            NewsMessage newsMessage = deserializeMessage(sqsMessage);
            
            // 메시지 삭제 (처리 완료)
            deleteMessage(sqsMessage);
            
            return newsMessage;
            
        } catch (SqsException e) {
            logger.error("SQS 메시지 수신 실패", e);
            throw new RuntimeException("SQS 메시지 수신 실패", e);
        }
    }
    
    @Override
    public int size() {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();
            
            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
            String countStr = response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
            return countStr != null ? Integer.parseInt(countStr) : 0;
            
        } catch (SqsException e) {
            logger.error("SQS 큐 크기 조회 실패", e);
            return 0;
        }
    }
    
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
    
    @Override
    public void clear() {
        // SQS에서는 개별 메시지 삭제만 가능하므로 모든 메시지를 폴링하여 삭제
        try {
            while (true) {
                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(0)
                        .build();
                
                ReceiveMessageResponse response = sqsClient.receiveMessage(request);
                if (response.messages().isEmpty()) {
                    break;
                }
                
                // 모든 메시지 삭제
                for (Message message : response.messages()) {
                    deleteMessage(message);
                }
            }
            logger.info("SQS 큐가 비워졌습니다");
        } catch (SqsException e) {
            logger.error("SQS 큐 비우기 실패", e);
        }
    }
    
    @Override
    public int getCapacity() {
        // SQS는 무제한 용량이므로 -1 반환
        return -1;
    }
    
    /**
     * 메시지 속성을 생성합니다.
     */
    private java.util.Map<String, MessageAttributeValue> createMessageAttributes(NewsMessage message) {
        java.util.Map<String, MessageAttributeValue> attributes = new java.util.HashMap<>();
        
        attributes.put("newsId", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(message.newsId())
                .build());
        
        attributes.put("messageType", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(message.type().name())
                .build());
        
        attributes.put("timestamp", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(message.timestamp().format(TIMESTAMP_FORMATTER))
                .build());
        
        return attributes;
    }
    
    /**
     * SQS 메시지를 NewsMessage로 역직렬화합니다.
     */
    private NewsMessage deserializeMessage(Message sqsMessage) {
        try {
            return objectMapper.readValue(sqsMessage.body(), NewsMessage.class);
        } catch (JsonProcessingException e) {
            logger.error("메시지 역직렬화 실패: {}", sqsMessage.body(), e);
            throw new RuntimeException("메시지 역직렬화 실패", e);
        }
    }
    
    /**
     * SQS 메시지를 삭제합니다.
     */
    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();
            
            sqsClient.deleteMessage(deleteRequest);
            logger.debug("SQS 메시지가 삭제되었습니다: messageId={}", message.messageId());
            
        } catch (SqsException e) {
            logger.error("SQS 메시지 삭제 실패: messageId={}", message.messageId(), e);
        }
    }
}
