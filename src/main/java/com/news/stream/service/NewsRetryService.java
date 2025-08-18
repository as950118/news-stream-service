package com.news.stream.service;

import com.news.stream.model.NewsProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 뉴스 재처리 서비스
 * 실패한 뉴스를 재처리하는 기능 제공
 */
@Service
public class NewsRetryService {
    
    private final NewsProcessingStatusService statusService;
    private final NewsStreamIntegrationService integrationService;
    private final Logger logger = LoggerFactory.getLogger(NewsRetryService.class);
    
    @Value("${news.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${news.retry.delay:5000}")
    private long retryDelay;
    
    public NewsRetryService(NewsProcessingStatusService statusService,
                             NewsStreamIntegrationService integrationService) {
        this.statusService = statusService;
        this.integrationService = integrationService;
    }
    
    /**
     * 실패한 뉴스 재처리 (1분마다 실행)
     */
    @Scheduled(fixedDelay = 60000)
    public void retryFailedNews() {
        try {
            logger.info("실패한 뉴스 재처리 시작");
            
            List<NewsProcessingStatus> failedNews = statusService.findFailedNews();
            List<NewsProcessingStatus> retryNews = statusService.findRetryNews();
            
            List<NewsProcessingStatus> allRetryableNews = new ArrayList<>();
            allRetryableNews.addAll(failedNews);
            allRetryableNews.addAll(retryNews);
            
            if (!allRetryableNews.isEmpty()) {
                for (NewsProcessingStatus status : allRetryableNews) {
                    if (status.getRetryCount() < maxRetryAttempts) {
                        retryNews(status);
                    } else {
                        logger.warn("최대 재시도 횟수 초과: {} ({}회)", 
                            status.getNewsId(), status.getRetryCount());
                    }
                }
            } else {
                logger.debug("재처리할 뉴스가 없습니다");
            }
            
        } catch (Exception e) {
            logger.error("실패한 뉴스 재처리 중 오류 발생", e);
        }
    }
    
    /**
     * 개별 뉴스 재처리
     */
    private void retryNews(NewsProcessingStatus status) {
        try {
            logger.info("뉴스 재처리 시작: {} ({}회째)", 
                status.getNewsId(), status.getRetryCount() + 1);
            
            // 재시도 횟수 증가
            statusService.incrementRetryCount(status.getNewsId());
            
            // 지연 후 재처리
            Thread.sleep(retryDelay);
            
            // 뉴스 재처리
            integrationService.processNewsCreated(status.getNewsId());
            
            logger.info("뉴스 재처리 완료: {}", status.getNewsId());
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("뉴스 재처리 중 인터럽트 발생: {}", status.getNewsId());
        } catch (Exception e) {
            logger.error("뉴스 재처리 실패: {}", status.getNewsId(), e);
        }
    }
}
