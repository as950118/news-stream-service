package com.news.stream.repository;

import com.news.stream.model.NewsProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 뉴스 처리 상태 리포지토리
 */
@Repository
public interface NewsProcessingStatusRepository extends JpaRepository<NewsProcessingStatus, String> {
    
    /**
     * 상태로 뉴스 처리 상태 조회
     */
    List<NewsProcessingStatus> findByStatus(NewsProcessingStatus.ProcessingStatus status);
    
    /**
     * 특정 시간 이후에 생성된 뉴스 처리 상태 조회
     */
    List<NewsProcessingStatus> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 특정 시간 이후에 업데이트된 뉴스 처리 상태 조회
     */
    List<NewsProcessingStatus> findByUpdatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 재시도 횟수가 최대값 미만인 실패한 뉴스 조회
     */
    @Query("SELECT n FROM NewsProcessingStatus n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries")
    List<NewsProcessingStatus> findFailedNewsWithRetryLimit(@Param("maxRetries") int maxRetries);
    
    /**
     * 특정 상태의 뉴스 개수 조회
     */
    long countByStatus(NewsProcessingStatus.ProcessingStatus status);
    
    /**
     * 특정 시간 범위 내의 뉴스 처리 상태 조회
     */
    @Query("SELECT n FROM NewsProcessingStatus n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    List<NewsProcessingStatus> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);
}
