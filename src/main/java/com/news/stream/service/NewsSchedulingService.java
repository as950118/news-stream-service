package com.news.stream.service;

import com.news.stream.model.TranslatedNews;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스 스케줄링 서비스
 * 정기적으로 뉴스를 처리하는 스케줄링 기능 제공
 */
@Service
public class NewsSchedulingService {
    
    private final NewsBatchProcessingService batchService;
    private final TranslatedNewsService newsService;
    private final Logger logger = LoggerFactory.getLogger(NewsSchedulingService.class);
    
    @Value("${news.schedule.initial-delay:60000}")
    private long initialDelay;
    
    @Value("${news.schedule.fixed-delay:300000}")
    private long fixedDelay;
    
    public NewsSchedulingService(NewsBatchProcessingService batchService,
                                 TranslatedNewsService newsService) {
        this.batchService = batchService;
        this.newsService = newsService;
    }
    
    /**
     * 대기 중인 뉴스 처리 (1분 후 시작, 5분마다 실행)
     */
    @Scheduled(initialDelay = 60000, fixedDelay = 300000)
    public void processPendingNews() {
        try {
            logger.info("대기 중인 뉴스 처리 시작");
            
            // 최근 1시간 내에 생성되었지만 아직 처리되지 않은 뉴스 조회
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<TranslatedNews> pendingNews = newsService.findByPublishedAtAfter(oneHourAgo);
            
            if (!pendingNews.isEmpty()) {
                List<String> newsIds = pendingNews.stream()
                    .map(TranslatedNews::getId)
                    .collect(Collectors.toList());
                
                batchService.processBatchNews(newsIds);
            } else {
                logger.debug("처리할 대기 뉴스가 없습니다");
            }
            
        } catch (Exception e) {
            logger.error("대기 뉴스 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 시간별 뉴스 처리 (매시간 정각에 실행)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void processHourlyNews() {
        try {
            logger.info("시간별 뉴스 처리 시작");
            
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            LocalDateTime now = LocalDateTime.now();
            
            batchService.processNewsByDateRange(oneHourAgo, now);
            
        } catch (Exception e) {
            logger.error("시간별 뉴스 처리 중 오류 발생", e);
        }
    }
    
    /**
     * 일별 뉴스 처리 (매일 자정에 실행)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processDailyNews() {
        try {
            logger.info("일별 뉴스 처리 시작");
            
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime today = LocalDateTime.now();
            
            batchService.processNewsByDateRange(yesterday, today);
            
        } catch (Exception e) {
            logger.error("일별 뉴스 처리 중 오류 발생", e);
        }
    }
}
