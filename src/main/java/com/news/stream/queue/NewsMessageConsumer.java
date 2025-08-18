package com.news.stream.queue;

import com.news.stream.service.NewsStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 뉴스 메시지 컨슈머
 * 메시지 큐에서 뉴스 메시지를 소비하고 처리합니다.
 */
@Component
public class NewsMessageConsumer {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final NewsStreamService newsStreamService;
    private final Logger logger = LoggerFactory.getLogger(NewsMessageConsumer.class);
    
    @Value("${queue.poll-timeout:1000}")
    private long pollTimeout;
    
    /**
     * 생성자
     * 
     * @param messageQueue 메시지 큐
     * @param newsStreamService 뉴스 스트림 서비스
     */
    public NewsMessageConsumer(MessageQueue<NewsMessage> messageQueue, NewsStreamService newsStreamService) {
        this.messageQueue = messageQueue;
        this.newsStreamService = newsStreamService;
    }
    
    /**
     * 메시지 소비를 시작합니다.
     * 비동기로 실행되며, 큐에서 메시지를 지속적으로 폴링합니다.
     */
    @Async("queueTaskExecutor")
    public void startConsuming() {
        logger.info("뉴스 메시지 컨슈머가 시작되었습니다");
        
        while (!Thread.currentThread().isInterrupted()) {
            try {
                NewsMessage message = messageQueue.dequeue(pollTimeout, TimeUnit.MILLISECONDS);
                if (message != null) {
                    processMessage(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("뉴스 메시지 컨슈머가 인터럽트되었습니다");
                break;
            } catch (Exception e) {
                logger.error("메시지 처리 중 오류 발생", e);
            }
        }
        
        logger.info("뉴스 메시지 컨슈머가 종료되었습니다");
    }
    
    /**
     * 메시지를 처리합니다.
     * 
     * @param message 처리할 메시지
     */
    private void processMessage(NewsMessage message) {
        try {
            logger.debug("메시지 처리 시작: {}", message);
            
            switch (message.type()) {
                case NEWS_CREATED:
                    handleNewsCreated(message);
                    break;
                case NEWS_UPDATED:
                    handleNewsUpdated(message);
                    break;
                case NEWS_DELETED:
                    handleNewsDeleted(message);
                    break;
                default:
                    logger.warn("알 수 없는 메시지 타입: {}", message.type());
            }
            
            logger.debug("메시지 처리 완료: {}", message.newsId());
        } catch (Exception e) {
            logger.error("메시지 처리 실패: {}", message, e);
        }
    }
    
    /**
     * 뉴스 생성 메시지를 처리합니다.
     * 
     * @param message 뉴스 생성 메시지
     */
    private void handleNewsCreated(NewsMessage message) {
        logger.info("뉴스 생성 처리: {}", message.newsId());
        try {
            newsStreamService.broadcastNews(message.newsId());
            logger.info("뉴스 생성 브로드캐스트 완료: {}", message.newsId());
        } catch (Exception e) {
            logger.error("뉴스 생성 브로드캐스트 실패: {}", message.newsId(), e);
        }
    }
    
    /**
     * 뉴스 수정 메시지를 처리합니다.
     * 
     * @param message 뉴스 수정 메시지
     */
    private void handleNewsUpdated(NewsMessage message) {
        logger.info("뉴스 수정 처리: {}", message.newsId());
        try {
            newsStreamService.broadcastNewsUpdate(message.newsId());
            logger.info("뉴스 수정 브로드캐스트 완료: {}", message.newsId());
        } catch (Exception e) {
            logger.error("뉴스 수정 브로드캐스트 실패: {}", message.newsId(), e);
        }
    }
    
    /**
     * 뉴스 삭제 메시지를 처리합니다.
     * 
     * @param message 뉴스 삭제 메시지
     */
    private void handleNewsDeleted(NewsMessage message) {
        logger.info("뉴스 삭제 처리: {}", message.newsId());
        try {
            newsStreamService.broadcastNewsDeletion(message.newsId());
            logger.info("뉴스 삭제 브로드캐스트 완료: {}", message.newsId());
        } catch (Exception e) {
            logger.error("뉴스 삭제 브로드캐스트 실패: {}", message.newsId(), e);
        }
    }
    
    /**
     * 컨슈머 초기화 시 메시지 소비를 시작합니다.
     */
    @PostConstruct
    public void init() {
        startConsuming();
    }
}
