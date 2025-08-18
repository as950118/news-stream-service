package com.news.stream.service;

import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.repository.NewsProcessingStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 처리 상태 서비스
 * 뉴스의 처리 상태를 관리하고 추적
 */
@Service
@Transactional
public class NewsProcessingStatusService {
    
    private final NewsProcessingStatusRepository statusRepository;
    private final Logger logger = LoggerFactory.getLogger(NewsProcessingStatusService.class);
    
    public NewsProcessingStatusService(NewsProcessingStatusRepository statusRepository) {
        this.statusRepository = statusRepository;
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
     * 뉴스를 FAILED 상태로 설정
     */
    public void markAsFailed(String newsId, String errorMessage) {
        Optional<NewsProcessingStatus> statusOpt = statusRepository.findById(newsId);
        if (statusOpt.isPresent()) {
            NewsProcessingStatus status = statusOpt.get();
            status.setStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
            status.setErrorMessage(errorMessage);
            status.setUpdatedAt(LocalDateTime.now());
            
            statusRepository.save(status);
            logger.warn("뉴스 처리 상태를 FAILED로 설정: {} - {}", newsId, errorMessage);
        }
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
    public List<NewsProcessingStatus> findFailedNews() {
        return statusRepository.findByStatus(NewsProcessingStatus.ProcessingStatus.FAILED);
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
     * 뉴스 처리 상태 조회
     */
    public Optional<NewsProcessingStatus> findByNewsId(String newsId) {
        return statusRepository.findById(newsId);
    }
    
    /**
     * 특정 시간 이후에 생성된 뉴스 처리 상태 조회
     */
    public List<NewsProcessingStatus> findByCreatedAtAfter(LocalDateTime dateTime) {
        return statusRepository.findByCreatedAtAfter(dateTime);
    }
}
