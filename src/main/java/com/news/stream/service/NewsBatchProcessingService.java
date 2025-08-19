package com.news.stream.service;

import com.news.stream.model.TranslatedNews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 배치 처리 서비스
 * 대량의 뉴스를 배치 단위로 처리하여 성능을 최적화
 */
@Service
@Transactional
public class NewsBatchProcessingService {
    
    private final NewsStreamIntegrationService integrationService;
    private final TranslatedNewsService newsService;
    private final Logger logger = LoggerFactory.getLogger(NewsBatchProcessingService.class);
    
    @Value("${news.batch.size:100}")
    private int batchSize;
    
    @Value("${news.batch.delay:1000}")
    private long batchDelay;
    
    public NewsBatchProcessingService(NewsStreamIntegrationService integrationService,
                                      TranslatedNewsService newsService) {
        this.integrationService = integrationService;
        this.newsService = newsService;
    }
    
    /**
     * 배치 뉴스 처리
     */
    @Async("newsTaskExecutor")
    public void processBatchNews(List<String> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            logger.warn("처리할 뉴스 ID가 없습니다");
            return;
        }
        
        logger.info("배치 뉴스 처리 시작: {}개", newsIds.size());
        
        List<List<String>> batches = partitionList(newsIds, batchSize);
        
        for (List<String> batch : batches) {
            try {
                processBatch(batch);
                Thread.sleep(batchDelay); // 배치 간 지연
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("배치 처리 중 인터럽트 발생");
                break;
            } catch (Exception e) {
                logger.error("배치 처리 실패", e);
            }
        }
        
        logger.info("배치 뉴스 처리 완료: {}개", newsIds.size());
    }
    
    /**
     * 개별 배치 처리
     */
    private void processBatch(List<String> newsIds) {
        for (String newsId : newsIds) {
            try {
                integrationService.processNewsCreated(newsId);
            } catch (Exception e) {
                logger.error("뉴스 ID {} 처리 실패", newsId, e);
                // 개별 실패는 로깅만 하고 계속 진행
            }
        }
    }
    
    /**
     * 리스트를 배치 크기로 분할
     */
    private List<List<String>> partitionList(List<String> list, int size) {
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
    
    /**
     * 날짜 범위로 뉴스 배치 처리
     */
    @Async("newsTaskExecutor")
    public void processNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            logger.info("날짜 범위 뉴스 처리 시작: {} ~ {}", startDate, endDate);
            
            List<TranslatedNews> newsList = newsService.findByPublishedAtBetween(startDate, endDate);
            List<String> newsIds = newsList.stream()
                .map(TranslatedNews::getId)
                .collect(Collectors.toList());
            
            processBatchNews(newsIds);
            
        } catch (Exception e) {
            logger.error("날짜 범위 뉴스 처리 실패: {} ~ {}", startDate, endDate, e);
        }
    }
}
