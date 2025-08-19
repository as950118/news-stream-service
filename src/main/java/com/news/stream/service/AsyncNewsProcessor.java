package com.news.stream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 비동기 뉴스 처리를 위한 서비스 클래스
 * 다양한 TaskExecutor를 활용하여 뉴스 처리를 비동기적으로 수행합니다.
 */
@Slf4j
@Service
public class AsyncNewsProcessor {
    
    private final NewsStreamIntegrationService streamService;
    private final NewsCacheService cacheService;
    
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
                log.debug("비동기 뉴스 처리 시작: {}", newsId);
                
                // 1. 뉴스 스트리밍 처리
                streamService.processNewsCreated(newsId);
                
                // 2. 캐시 업데이트
                updateNewsCache(newsId);
                
                log.debug("비동기 뉴스 처리 완료: {}", newsId);
                
            } catch (Exception e) {
                log.error("비동기 뉴스 처리 실패: {}", newsId, e);
            }
        });
    }
    
    /**
     * 배치 뉴스 비동기 처리 - 스트림 API 활용으로 성능 개선
     */
    @Async("newsOptimizationTaskExecutor")
    public CompletableFuture<List<String>> processBatchNewsAsync(List<String> newsIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("배치 뉴스 처리 시작: {}개", newsIds.size());
            
            List<String> processedIds = newsIds.parallelStream()
                .filter(newsId -> {
                    try {
                        streamService.processNewsCreated(newsId);
                        return true;
                    } catch (Exception e) {
                        log.error("뉴스 ID {} 처리 실패", newsId, e);
                        return false;
                    }
                })
                .collect(Collectors.toList());
            
            log.debug("배치 뉴스 처리 완료: {}개", processedIds.size());
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
                log.error("뉴스 캐시 업데이트 실패: {}", newsId, e);
            }
        });
    }
    
    /**
     * 뉴스 캐시 업데이트
     */
    private void updateNewsCache(String newsId) {
        try {
            // 실제 구현에서는 뉴스 서비스에서 조회하여 캐시 업데이트
            log.debug("뉴스 캐시 업데이트: {}", newsId);
        } catch (Exception e) {
            log.warn("뉴스 캐시 업데이트 중 오류: {}", newsId, e);
        }
    }
}
