package com.news.stream.controller;

import com.news.stream.dto.QueueStatusResponse;
import com.news.stream.queue.LinkedBlockingMessageQueue;
import com.news.stream.queue.MessageQueue;
import com.news.stream.queue.NewsMessage;
import com.news.stream.queue.NewsMessageProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 큐 상태 모니터링 및 제어를 위한 컨트롤러
 * 큐의 상태 확인, 큐 비우기, 테스트 메시지 전송 등의 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Queue", description = "메시지 큐 관리 API")
public class QueueController {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final NewsMessageProducer messageProducer;
    
    /**
     * 생성자
     * 
     * @param messageQueue 메시지 큐
     * @param messageProducer 메시지 프로듀서
     */
    public QueueController(MessageQueue<NewsMessage> messageQueue,
                         NewsMessageProducer messageProducer) {
        this.messageQueue = messageQueue;
        this.messageProducer = messageProducer;
    }
    
    /**
     * 큐의 현재 상태를 조회합니다.
     * 
     * @return 큐 상태 정보
     */
    @GetMapping("/status")
    @Operation(summary = "큐 상태 조회", description = "메시지 큐의 현재 상태를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "큐 상태 조회 성공")
    })
    public ResponseEntity<QueueStatusResponse> getQueueStatus() {
        log.debug("큐 상태 조회 요청");
        
        QueueStatusResponse response = createQueueStatusResponse();
        
        log.debug("큐 상태 조회 완료: 크기={}, 비어있음={}", response.size(), response.isEmpty());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 큐의 모든 메시지를 제거합니다.
     * 
     * @return 성공 응답
     */
    @PostMapping("/clear")
    @Operation(summary = "큐 비우기", description = "메시지 큐의 모든 메시지를 제거합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "큐 비우기 성공")
    })
    public ResponseEntity<Void> clearQueue() {
        log.info("큐 비우기 요청");
        
        int queueSize = messageQueue.size();
        messageQueue.clear();
        
        log.info("큐 비우기 완료: 제거된 메시지 {}개", queueSize);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 테스트 메시지를 큐에 전송합니다.
     * 
     * @return 성공 응답
     */
    @PostMapping("/test-message")
    @Operation(summary = "테스트 메시지 전송", description = "테스트용 메시지를 큐에 전송합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "테스트 메시지 전송 성공")
    })
    public ResponseEntity<Void> sendTestMessage() {
        String testNewsId = "test-" + System.currentTimeMillis();
        
        log.info("테스트 메시지 전송 요청: {}", testNewsId);
        
        messageProducer.publishTestMessage(testNewsId);
        
        log.info("테스트 메시지 전송 완료: {}", testNewsId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 뉴스 생성 테스트 메시지를 큐에 전송합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 성공 응답
     */
    @PostMapping("/test-message/news-created/{newsId}")
    @Operation(summary = "뉴스 생성 테스트 메시지 전송", description = "뉴스 생성 이벤트를 시뮬레이션하는 테스트 메시지를 전송합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "테스트 메시지 전송 성공")
    })
    public ResponseEntity<Void> sendNewsCreatedTestMessage(
        @Parameter(description = "뉴스 ID", required = true)
        @PathVariable String newsId) {
        
        log.info("뉴스 생성 테스트 메시지 전송 요청: {}", newsId);
        
        messageProducer.publishNewsCreated(newsId);
        
        log.info("뉴스 생성 테스트 메시지 전송 완료: {}", newsId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 뉴스 수정 테스트 메시지를 큐에 전송합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 성공 응답
     */
    @PostMapping("/test-message/news-updated/{newsId}")
    public ResponseEntity<Void> sendNewsUpdatedTestMessage(@PathVariable String newsId) {
        messageProducer.publishNewsUpdated(newsId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 뉴스 삭제 테스트 메시지를 큐에 전송합니다.
     * 
     * @param newsId 뉴스 ID
     * @return 성공 응답
     */
    @PostMapping("/test-message/news-deleted/{newsId}")
    public ResponseEntity<Void> sendNewsDeletedTestMessage(@PathVariable String newsId) {
        messageProducer.publishNewsDeleted(newsId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 큐의 통계 정보를 조회합니다.
     * 
     * @return 큐 통계 정보
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getQueueStats() {
        if (messageQueue instanceof LinkedBlockingMessageQueue) {
            LinkedBlockingMessageQueue linkedQueue = (LinkedBlockingMessageQueue) messageQueue;
            
            var stats = new Object() {
                public final int size = messageQueue.size();
                public final boolean isEmpty = messageQueue.isEmpty();
                public final int capacity = linkedQueue.getCapacity();
                public final int remainingCapacity = linkedQueue.getRemainingCapacity();
                public final boolean isFull = linkedQueue.isFull();
                public final double utilization = (double) (capacity - remainingCapacity) / capacity;
            };
            
            return ResponseEntity.ok(stats);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("이 큐 구현체는 통계 정보를 지원하지 않습니다");
        }
    }

    /**
     * 큐 상태 응답을 생성하는 공통 메서드
     */
    private QueueStatusResponse createQueueStatusResponse() {
        if (messageQueue instanceof LinkedBlockingMessageQueue) {
            LinkedBlockingMessageQueue linkedQueue = (LinkedBlockingMessageQueue) messageQueue;
            return new QueueStatusResponse(
                messageQueue.size(),
                messageQueue.isEmpty(),
                linkedQueue.getCapacity(),
                linkedQueue.getRemainingCapacity()
            );
        } else {
            // 기본 구현체인 경우
            return new QueueStatusResponse(
                messageQueue.size(),
                messageQueue.isEmpty(),
                -1, // 용량 정보 없음
                -1   // 남은 용량 정보 없음
            );
        }
    }
}
