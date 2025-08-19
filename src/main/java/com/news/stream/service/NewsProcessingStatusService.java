package com.news.stream.service;

import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.repository.NewsProcessingStatusRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@Transactional
public class NewsProcessingStatusService {
    
    private final NewsProcessingStatusRepository statusRepository;
    private final ObjectMapper objectMapper;
    
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
        log.debug("뉴스 처리 상태를 PENDING으로 설정: {}", newsId);
    }
    
    /**
     * 뉴스를 PROCESSING 상태로 설정
     */
    public void markAsProcessing(String newsId) {
        updateStatus(newsId, NewsProcessingStatus.ProcessingStatus.PROCESSING, 
                    status -> status.setProcessingStartedAt(LocalDateTime.now()));
        log.debug("뉴스 처리 상태를 PROCESSING으로 설정: {}", newsId);
    }
    
    /**
     * 뉴스를 COMPLETED 상태로 설정
     */
    public void markAsCompleted(String newsId) {
        updateStatus(newsId, NewsProcessingStatus.ProcessingStatus.COMPLETED,
                    status -> status.setProcessingCompletedAt(LocalDateTime.now()));
        log.debug("뉴스 처리 상태를 COMPLETED로 설정: {}", newsId);
    }
    
    /**
     * 뉴스를 FAILED 상태로 설정 (고객사별 실패 정보 포함)
     */
    public void markAsFailed(String newsId, String errorMessage, String failureReason, 
                           Map<String, String> failedCustomers, int totalCustomerCount) {
        updateStatus(newsId, NewsProcessingStatus.ProcessingStatus.FAILED, status -> {
            status.setErrorMessage(errorMessage);
            status.setFailureReason(failureReason);
            status.setFailedCustomerCount(failedCustomers.size());
            status.setTotalCustomerCount(totalCustomerCount);
            status.setLastFailureAt(LocalDateTime.now());
            
            // 고객사별 실패 정보를 JSON으로 저장
            try {
                String affectedCustomersJson = objectMapper.writeValueAsString(failedCustomers);
                status.setAffectedCustomers(affectedCustomersJson);
            } catch (JsonProcessingException e) {
                log.error("고객사별 실패 정보 JSON 변환 실패: {}", newsId, e);
                status.setAffectedCustomers("{}");
            }
        });
        
        log.warn("뉴스 처리 상태를 FAILED로 설정: {} - {} ({}명 실패)", 
            newsId, failureReason, failedCustomers.size());
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
        updateStatus(newsId, NewsProcessingStatus.ProcessingStatus.RETRY, 
                    status -> status.setRetryCount(status.getRetryCount() + 1));
        log.debug("뉴스 재시도 횟수 증가: {}", newsId);
    }
    
    /**
     * 상태 업데이트를 위한 공통 메서드
     */
    private void updateStatus(String newsId, NewsProcessingStatus.ProcessingStatus newStatus, 
                             StatusUpdater updater) {
        statusRepository.findById(newsId).ifPresent(status -> {
            status.setStatus(newStatus);
            status.setUpdatedAt(LocalDateTime.now());
            updater.update(status);
            statusRepository.save(status);
        });
    }
    
    /**
     * 실패한 뉴스 조회
     */
    @Transactional(readOnly = true)
    public List<NewsProcessingStatus> findFailedNews() {
        try {
            return statusRepository.findByStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
        } catch (Exception e) {
            log.error("실패한 뉴스 조회 중 오류 발생", e);
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
     * 특정 상태의 뉴스 수를 조회합니다.
     * 
     * @param status 조회할 상태
     * @return 뉴스 수
     */
    public long countByStatus(NewsProcessingStatus.ProcessingStatus status) {
        return statusRepository.countByStatus(status);
    }
    
    /**
     * 특정 상태의 뉴스 목록을 조회합니다.
     * 
     * @param status 조회할 상태
     * @return 뉴스 목록
     */
    public List<NewsProcessingStatus> findByStatus(NewsProcessingStatus.ProcessingStatus status) {
        return statusRepository.findByStatus(status);
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
            log.debug("뉴스 처리 상태 삭제: {}", newsId);
        } catch (Exception e) {
            log.error("뉴스 처리 상태 삭제 중 오류 발생: {}", newsId, e);
        }
    }
    
    /**
     * Dead Letter Queue에 추가 (최대 재시도 횟수 초과 시)
     */
    public void moveToDeadLetterQueue(String newsId, String failureReason, int retryCount, String errorMessage) {
        try {
            updateStatus(newsId, NewsProcessingStatus.ProcessingStatus.DEAD_LETTER, status -> {
                status.setErrorMessage(errorMessage);
                status.setFailureReason(failureReason);
                status.setRetryCount(retryCount);
                status.setLastFailureAt(LocalDateTime.now());
            });
            
            log.warn("뉴스가 Dead Letter Queue로 이동됨: {} - {}", newsId, failureReason);
        } catch (Exception e) {
            log.error("Dead Letter Queue 이동 중 오류 발생: {}", newsId, e);
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
        return statusRepository.findById(newsId)
            .filter(status -> status.getAffectedCustomers() != null)
            .map(status -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> failedCustomers = objectMapper.readValue(
                        status.getAffectedCustomers(), Map.class);
                    return failedCustomers;
                } catch (Exception e) {
                    log.error("고객사별 실패 정보 파싱 실패: {}", newsId, e);
                    return Map.<String, String>of();
                }
            })
            .orElse(Map.of());
    }
    
    /**
     * 상태 업데이트를 위한 함수형 인터페이스
     */
    @FunctionalInterface
    private interface StatusUpdater {
        void update(NewsProcessingStatus status);
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
