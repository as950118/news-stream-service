package com.news.stream.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 뉴스 메시지 프로듀서
 * 뉴스 관련 이벤트를 메시지 큐에 발행합니다.
 */
@Component
public class NewsMessageProducer {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final Logger logger = LoggerFactory.getLogger(NewsMessageProducer.class);
    
    /**
     * 생성자
     * 
     * @param messageQueue 메시지 큐
     */
    public NewsMessageProducer(MessageQueue<NewsMessage> messageQueue) {
        this.messageQueue = messageQueue;
    }
    
    /**
     * 뉴스 생성 메시지를 큐에 발행합니다.
     * 
     * @param newsId 뉴스 ID
     */
    public void publishNewsCreated(String newsId) {
        try {
            NewsMessage message = NewsMessage.newsCreated(newsId);
            messageQueue.enqueue(message);
            logger.info("뉴스 생성 메시지가 큐에 추가되었습니다: {}", newsId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("뉴스 생성 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
        } catch (Exception e) {
            logger.error("뉴스 생성 메시지 큐잉 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 수정 메시지를 큐에 발행합니다.
     * 
     * @param newsId 뉴스 ID
     */
    public void publishNewsUpdated(String newsId) {
        try {
            NewsMessage message = NewsMessage.newsUpdated(newsId);
            messageQueue.enqueue(message);
            logger.info("뉴스 수정 메시지가 큐에 추가되었습니다: {}", newsId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("뉴스 수정 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
        } catch (Exception e) {
            logger.error("뉴스 수정 메시지 큐잉 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 삭제 메시지를 큐에 발행합니다.
     * 
     * @param newsId 뉴스 ID
     */
    public void publishNewsDeleted(String newsId) {
        try {
            NewsMessage message = NewsMessage.newsDeleted(newsId);
            messageQueue.enqueue(message);
            logger.info("뉴스 삭제 메시지가 큐에 추가되었습니다: {}", newsId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("뉴스 삭제 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
        } catch (Exception e) {
            logger.error("뉴스 삭제 메시지 큐잉 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * 테스트 메시지를 큐에 발행합니다.
     * 
     * @param newsId 테스트 뉴스 ID
     */
    public void publishTestMessage(String newsId) {
        try {
            NewsMessage message = NewsMessage.newsCreated(newsId);
            messageQueue.enqueue(message);
            logger.info("테스트 메시지가 큐에 추가되었습니다: {}", newsId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("테스트 메시지 큐잉 중 인터럽트 발생: {}", newsId, e);
        } catch (Exception e) {
            logger.error("테스트 메시지 큐잉 중 오류 발생: {}", newsId, e);
        }
    }
}
