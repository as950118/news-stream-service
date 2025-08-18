package com.news.stream.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 뉴스 처리 상태 엔티티
 * 뉴스의 처리 상태를 추적하고 관리
 */
@Entity
@Table(name = "NEWS_PROCESSING_STATUS")
public class NewsProcessingStatus {
    
    @Id
    private String newsId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status;
    
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;
    
    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;
    
    @Column(name = "retry_count")
    private int retryCount = 0;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRY
    }
    
    // 기본 생성자
    public NewsProcessingStatus() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 생성자
    public NewsProcessingStatus(String newsId, ProcessingStatus status) {
        this();
        this.newsId = newsId;
        this.status = status;
    }
    
    // Getter와 Setter
    public String getNewsId() {
        return newsId;
    }
    
    public void setNewsId(String newsId) {
        this.newsId = newsId;
    }
    
    public ProcessingStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessingStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }
    
    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getProcessingCompletedAt() {
        return processingCompletedAt;
    }
    
    public void setProcessingCompletedAt(LocalDateTime processingCompletedAt) {
        this.processingCompletedAt = processingCompletedAt;
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        NewsProcessingStatus that = (NewsProcessingStatus) o;
        return newsId != null ? newsId.equals(that.newsId) : that.newsId == null;
    }
    
    @Override
    public int hashCode() {
        return newsId != null ? newsId.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "NewsProcessingStatus{" +
                "newsId='" + newsId + '\'' +
                ", status=" + status +
                ", processingStartedAt=" + processingStartedAt +
                ", processingCompletedAt=" + processingCompletedAt +
                ", retryCount=" + retryCount +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
