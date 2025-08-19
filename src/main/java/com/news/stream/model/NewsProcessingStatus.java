package com.news.stream.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

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
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "affected_customers")
    private String affectedCustomers; // JSON 형태로 고객사 정보 저장
    
    @Column(name = "failed_customer_count")
    private int failedCustomerCount = 0;
    
    @Column(name = "total_customer_count")
    private int totalCustomerCount = 0;
    
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        RETRY,
        DEAD_LETTER
    }
    
    public enum FailureReason {
        NETWORK_ERROR,
        CUSTOMER_OFFLINE,
        AUTHENTICATION_FAILED,
        MESSAGE_TOO_LARGE,
        RATE_LIMIT_EXCEEDED,
        VALIDATION_ERROR,
        PROCESSING_ERROR,
        MAX_RETRY_EXCEEDED,
        UNKNOWN_ERROR
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
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getAffectedCustomers() {
        return affectedCustomers;
    }
    
    public void setAffectedCustomers(String affectedCustomers) {
        this.affectedCustomers = affectedCustomers;
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getFailedCustomerCount() {
        return failedCustomerCount;
    }
    
    public void setFailedCustomerCount(int failedCustomerCount) {
        this.failedCustomerCount = failedCustomerCount;
        this.updatedAt = LocalDateTime.now();
    }
    
    public int getTotalCustomerCount() {
        return totalCustomerCount;
    }
    
    public void setTotalCustomerCount(int totalCustomerCount) {
        this.totalCustomerCount = totalCustomerCount;
        this.updatedAt = LocalDateTime.now();
    }
    
    public LocalDateTime getLastFailureAt() {
        return lastFailureAt;
    }
    
    public void setLastFailureAt(LocalDateTime lastFailureAt) {
        this.lastFailureAt = lastFailureAt;
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
    
    // equals와 hashCode
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
                ", failureReason='" + failureReason + '\'' +
                ", failedCustomerCount=" + failedCustomerCount +
                ", totalCustomerCount=" + totalCustomerCount +
                ", lastFailureAt=" + lastFailureAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
