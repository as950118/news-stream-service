package com.news.stream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 비동기 뉴스 처리를 위한 서비스 클래스
 * 다양한 TaskExecutor를 활용하여 뉴스 처리를 비동기적으로 수행합니다.
 */
@Service
public class AsyncNewsProcessor {
    
    private final NewsStreamIntegrationService streamService;
    private final NewsCacheService cacheService;
    private final Logger logger = LoggerFactory.getLogger(AsyncNewsProcessor.class);
    
    public AsyncNewsProcessor(NewsStreamIntegrationService streamService,
                             NewsCacheService cacheService) {
        this.streamService = streamService;
        this.cacheService = cacheService;
    }
    
    /**
     * 단일 뉴스 비동기 처리
     */
    @Async("newsOptimizationTaskExecutor")
    public CompletableFuture<Void> processNewsAsync(String newsId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.debug("비동기 뉴스 처리 시작: {}", newsId);
                
                // 1. 뉴스 스트리밍 처리
                streamService.processNewsCreated(newsId);
                
                // 2. 캐시 업데이트
                updateNewsCache(newsId);
                
                logger.debug("비동기 뉴스 처리 완료: {}", newsId);
                
            } catch (Exception e) {
                logger.error("비동기 뉴스 처리 실패: {}", newsId, e);
            }
        });
    }
    
    /**
     * 배치 뉴스 비동기 처리
     */
    @Async("newsOptimizationTaskExecutor")
    public CompletableFuture<List<String>> processBatchNewsAsync(List<String> newsIds) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> processedIds = new ArrayList<>();
            
            try {
                logger.debug("배치 뉴스 처리 시작: {}개", newsIds.size());
                
                for (String newsId : newsIds) {
                    try {
                        streamService.processNewsCreated(newsId);
                        processedIds.add(newsId);
                    } catch (Exception e) {
                        logger.error("뉴스 ID {} 처리 실패", newsId, e);
                    }
                }
                
                logger.debug("배치 뉴스 처리 완료: {}개", processedIds.size());
                
            } catch (Exception e) {
                logger.error("배치 뉴스 처리 중 오류", e);
            }
            
            return processedIds;
        });
    }
    
    /**
     * 뉴스 캐시 비동기 업데이트
     */
    @Async("cacheTaskExecutor")
    public CompletableFuture<Void> updateNewsCacheAsync(String newsId) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateNewsCache(newsId);
            } catch (Exception e) {
                logger.error("뉴스 캐시 업데이트 실패: {}", newsId, e);
            }
        });
    }
    
    /**
     * 뉴스 캐시 업데이트
     */
    private void updateNewsCache(String newsId) {
        try {
            // 실제 구현에서는 뉴스 서비스에서 조회하여 캐시 업데이트
            logger.debug("뉴스 캐시 업데이트: {}", newsId);
        } catch (Exception e) {
            logger.warn("뉴스 캐시 업데이트 중 오류: {}", newsId, e);
        }
    }
}
