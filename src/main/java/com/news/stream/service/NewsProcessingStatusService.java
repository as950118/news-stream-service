package com.news.stream.service;

import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.repository.NewsProcessingStatusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 뉴스 처리 상태 서비스
 * 뉴스의 처리 상태를 관리하고 추적
 */
@Service
@Transactional
public class NewsProcessingStatusService {
    
    private final NewsProcessingStatusRepository statusRepository;
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(NewsProcessingStatusService.class);
    
    public NewsProcessingStatusService(NewsProcessingStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 뉴스를 PENDING 상태로 설정
     */
    public void markAsPending(String newsId) {
        NewsProcessingStatus status = new NewsProcessingStatus();
        status.setNewsId(newsId);
        status.setStatus(NewsProcessingStatus.ProcessingStatus.PENDING);
        status.setCreatedAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());
        
        statusRepository.save(status);
        logger.debug("뉴스 처리 상태를 PENDING으로 설정: {}", newsId);
    }
    
    /**
     * 뉴스를 PROCESSING 상태로 설정
     */
    public void markAsProcessing(String newsId) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent()) {
            NewsProcessingStatus status = statusOpt.get();
            status.setStatus(NewsProcessingStatus.ProcessingStatus.PROCESSING);
            status.setProcessingStartedAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            
            statusRepository.save(status);
            logger.debug("뉴스 처리 상태를 PROCESSING으로 설정: {}", newsId);
        }
    }
    
    /**
     * 뉴스를 COMPLETED 상태로 설정
     */
    public void markAsCompleted(String newsId) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent()) {
            NewsProcessingStatus status = statusOpt.get();
            status.setStatus(NewsProcessingStatus.ProcessingStatus.COMPLETED);
            status.setProcessingCompletedAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            
            statusRepository.save(status);
            logger.debug("뉴스 처리 상태를 COMPLETED로 설정: {}", newsId);
        }
    }
    
    /**
     * 뉴스를 FAILED 상태로 설정 (고객사별 실패 정보 포함)
     */
    public void markAsFailed(String newsId, String errorMessage, String failureReason, 
                           Map<String, String> failedCustomers, int totalCustomerCount) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent()) {
            NewsProcessingStatus status = statusOpt.get();
            status.setStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
            status.setErrorMessage(errorMessage);
            status.setFailureReason(failureReason);
            status.setFailedCustomerCount(failedCustomers.size());
            status.setTotalCustomerCount(totalCustomerCount);
            status.setLastFailureAt(LocalDateTime.now());
            status.setUpdatedAt(LocalDateTime.now());
            
            // 고객사별 실패 정보를 JSON으로 저장
            try {
                String affectedCustomersJson = objectMapper.writeValueAsString(failedCustomers);
                status.setAffectedCustomers(affectedCustomersJson);
            } catch (JsonProcessingException e) {
                logger.error("고객사별 실패 정보 JSON 변환 실패: {}", newsId, e);
                status.setAffectedCustomers("{}");
            }
            
            statusRepository.save(status);
            logger.warn("뉴스 처리 상태를 FAILED로 설정: {} - {} ({}명 실패)", 
                newsId, failureReason, failedCustomers.size());
        }
    }
    
    /**
     * 기존 markAsFailed 메서드 (호환성 유지)
     */
    public void markAsFailed(String newsId, String errorMessage) {
        markAsFailed(newsId, errorMessage, "UNKNOWN_ERROR", Map.of(), 0);
    }
    
    /**
     * 뉴스 재시도 횟수 증가
     */
    public void incrementRetryCount(String newsId) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent()) {
            NewsProcessingStatus status = statusOpt.get();
            status.setRetryCount(status.getRetryCount() + 1);
            status.setStatus(NewsProcessingStatus.ProcessingStatus.RETRY);
            status.setUpdatedAt(LocalDateTime.now());
            
            statusRepository.save(status);
            logger.debug("뉴스 재시도 횟수 증가: {} ({}회)", newsId, status.getRetryCount());
        }
    }
    
    /**
     * 실패한 뉴스 조회
     */
    @Transactional(readOnly = true)
    public List<NewsProcessingStatus> findFailedNews() {
        try {
            return statusRepository.findByStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
        } catch (Exception e) {
            logger.error("실패한 뉴스 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 재시도 중인 뉴스 조회
     */
    public List<NewsProcessingStatus> findRetryNews() {
        return statusRepository.findByStatus(NewsProcessingStatus.ProcessingStatus.RETRY);
    }
    
    /**
     * 특정 상태의 뉴스 개수 조회
     */
    public long countByStatus(NewsProcessingStatus.ProcessingStatus status) {
        return statusRepository.countByStatus(status);
    }
    
    /**
     * 뉴스 ID로 처리 상태 조회
     */
    public Optional<NewsProcessingStatus> findByNewsId(String newsId) {
        return statusRepository.findById(newsId);
    }
    
    /**
     * 특정 기간 이후의 뉴스 처리 상태 조회
     */
    public List<NewsProcessingStatus> findByCreatedAtAfter(LocalDateTime dateTime) {
        return statusRepository.findByCreatedAtAfter(dateTime);
    }
    
    /**
     * 뉴스 ID로 처리 상태 삭제
     */
    public void deleteByNewsId(String newsId) {
        try {
            statusRepository.deleteById(newsId);
            logger.debug("뉴스 처리 상태 삭제: {}", newsId);
        } catch (Exception e) {
            logger.error("뉴스 처리 상태 삭제 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * Dead Letter Queue에 추가 (최대 재시도 횟수 초과 시)
     */
    public void moveToDeadLetterQueue(String newsId, String failureReason, int retryCount, String errorMessage) {
        try {
            Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
            if (statusOpt.isPresent()) {
                NewsProcessingStatus status = statusOpt.get();
                status.setStatus(NewsProcessingStatus.ProcessingStatus.DEAD_LETTER);
                status.setErrorMessage(errorMessage);
                status.setFailureReason(failureReason);
                status.setRetryCount(retryCount);
                status.setLastFailureAt(LocalDateTime.now());
                status.setUpdatedAt(LocalDateTime.now());
                
                statusRepository.save(status);
                logger.warn("뉴스가 Dead Letter Queue로 이동됨: {} - {}", newsId, failureReason);
            }
        } catch (Exception e) {
            logger.error("Dead Letter Queue 이동 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * Dead Letter Queue의 모든 뉴스 조회
     */
    public List<NewsProcessingStatus> getDeadLetterNews() {
        return statusRepository.findByStatus(NewsProcessingStatus.ProcessingStatus.DEAD_LETTER);
    }
    
    /**
     * Dead Letter Queue 통계 조회
     */
    public DeadLetterQueueStats getDeadLetterQueueStats() {
        long deadLetterCount = statusRepository.countByStatus(NewsProcessingStatus.ProcessingStatus.DEAD_LETTER);
        long failedCount = statusRepository.countByStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
        long retryCount = statusRepository.countByStatus(NewsProcessingStatus.ProcessingStatus.RETRY);
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long recentCount = statusRepository.countByLastFailedAtBetween(last24Hours, LocalDateTime.now());
        
        return new DeadLetterQueueStats(
            deadLetterCount,
            failedCount,
            retryCount,
            recentCount
        );
    }
    
    /**
     * 고객사별 실패 정보 조회
     */
    public Map<String, String> getFailedCustomerDetails(String newsId) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent() && statusOpt.get().getAffectedCustomers() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> failedCustomers = objectMapper.readValue(
                    statusOpt.get().getAffectedCustomers(), Map.class);
                return failedCustomers;
            } catch (Exception e) {
                logger.error("고객사별 실패 정보 파싱 실패: {}", newsId, e);
            }
        }
        return Map.of();
    }
    
    /**
     * Dead Letter Queue 통계 정보
     */
    public static class DeadLetterQueueStats {
        private final long deadLetterCount;
        private final long failedCount;
        private final long retryCount;
        private final long recentCount;
        
        public DeadLetterQueueStats(long deadLetterCount, long failedCount, 
                                  long retryCount, long recentCount) {
            this.deadLetterCount = deadLetterCount;
            this.failedCount = failedCount;
            this.retryCount = retryCount;
            this.recentCount = recentCount;
        }
        
        // Getter methods
        public long getDeadLetterCount() { return deadLetterCount; }
        public long getFailedCount() { return failedCount; }
        public long getRetryCount() { return retryCount; }
        public long getRecentCount() { return recentCount; }
        public long getTotalCount() { return deadLetterCount; }
    }
}
