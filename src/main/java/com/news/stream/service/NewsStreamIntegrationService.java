package com.news.stream.service;

import com.news.stream.exception.NewsProcessingException;
import com.news.stream.queue.NewsMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 뉴스 스트림 통합 서비스
 * 메시지 큐와 WebSocket을 연동하여 실시간 뉴스 전송 파이프라인을 구현
 */
@Service
@Transactional
public class NewsStreamIntegrationService {
    
    private final NewsMessageProducer messageProducer;
    private final NewsStreamService streamService;
    private final TranslatedNewsService newsService;
    private final Logger logger = LoggerFactory.getLogger(NewsStreamIntegrationService.class);
    
    public NewsStreamIntegrationService(NewsMessageProducer messageProducer,
                                        NewsStreamService streamService,
                                        TranslatedNewsService newsService) {
        this.messageProducer = messageProducer;
        this.streamService = streamService;
        this.newsService = newsService;
    }
    
    /**
     * 뉴스 생성 처리
     */
    public void processNewsCreated(String newsId) {
        try {
            logger.info("뉴스 생성 처리 시작: {}", newsId);
            
            // 1. 메시지 큐에 뉴스 생성 이벤트 발행
            messageProducer.publishNewsCreated(newsId);
            
            // 2. 즉시 스트리밍 (선택적)
            streamNewsImmediately(newsId);
            
            logger.info("뉴스 생성 처리 완료: {}", newsId);
        } catch (Exception e) {
            logger.error("뉴스 생성 처리 실패: {}", newsId, e);
            throw new NewsProcessingException("뉴스 생성 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 뉴스 수정 처리
     */
    public void processNewsUpdated(String newsId) {
        try {
            logger.info("뉴스 수정 처리 시작: {}", newsId);
            
            // 1. 메시지 큐에 뉴스 수정 이벤트 발행
            messageProducer.publishNewsUpdated(newsId);
            
            // 2. 즉시 스트리밍 (선택적)
            streamNewsUpdateImmediately(newsId);
            
            logger.info("뉴스 수정 처리 완료: {}", newsId);
        } catch (Exception e) {
            logger.error("뉴스 수정 처리 실패: {}", newsId, e);
            throw new NewsProcessingException("뉴스 수정 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 뉴스 삭제 처리
     */
    public void processNewsDeleted(String newsId) {
        try {
            logger.info("뉴스 삭제 처리 시작: {}", newsId);
            
            // 1. 메시지 큐에 뉴스 삭제 이벤트 발행
            messageProducer.publishNewsDeleted(newsId);
            
            // 2. 즉시 스트리밍 (선택적)
            streamNewsDeletionImmediately(newsId);
            
            logger.info("뉴스 삭제 처리 완료: {}", newsId);
        } catch (Exception e) {
            logger.error("뉴스 삭제 처리 실패: {}", newsId, e);
            throw new NewsProcessingException("뉴스 삭제 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 뉴스 즉시 스트리밍
     */
    private void streamNewsImmediately(String newsId) {
        try {
            streamService.broadcastNews(newsId);
        } catch (Exception e) {
            logger.warn("즉시 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 업데이트 즉시 스트리밍
     */
    private void streamNewsUpdateImmediately(String newsId) {
        try {
            streamService.broadcastNewsUpdate(newsId);
        } catch (Exception e) {
            logger.warn("즉시 업데이트 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
        }
    }
    
    /**
     * 뉴스 삭제 즉시 스트리밍
     */
    private void streamNewsDeletionImmediately(String newsId) {
        try {
            streamService.broadcastNewsDeletion(newsId);
        } catch (Exception e) {
            logger.warn("즉시 삭제 스트리밍 실패 (큐 처리로 대체): {}", newsId, e);
        }
    }
}
