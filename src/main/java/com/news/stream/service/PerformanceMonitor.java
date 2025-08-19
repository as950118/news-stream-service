package com.news.stream.service;

import com.news.stream.util.Monitored;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class PerformanceMonitor {
    
    private final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    private final CustomMetrics customMetrics;
    
    public PerformanceMonitor(CustomMetrics customMetrics) {
        this.customMetrics = customMetrics;
    }
    
    @Around("@annotation(monitored)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // 성공 메트릭 기록
            customMetrics.recordProcessingTime(executionTime);
            customMetrics.incrementNewsProcessed();
            
            // 성능 로깅
            if (executionTime > 1000) {
                logger.warn("성능 경고: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
            } else {
                logger.debug("성능 정보: {}.{} 실행 시간 {}ms", className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // 실패 메트릭 기록
            customMetrics.incrementNewsFailed();
            
            logger.error("성능 오류: {}.{} 실행 시간 {}ms, 오류: {}", 
                className, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
    
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void updatePerformanceMetrics() {
        try {
            // 성공률 계산 (예시)
            double successRate = calculateSuccessRate();
            customMetrics.updateSuccessRate(successRate);
            
            // 평균 처리 시간 계산 (예시)
            double avgProcessingTime = calculateAverageProcessingTime();
            customMetrics.updateAverageProcessingTime(avgProcessingTime);
            
            // 큐 처리 지연 시간 계산 (예시)
            double queueDelay = calculateQueueProcessingDelay();
            customMetrics.updateQueueProcessingDelay(queueDelay);
            
        } catch (Exception e) {
            logger.error("성능 메트릭 업데이트 중 오류 발생", e);
        }
    }
    
    private double calculateSuccessRate() {
        // 실제 구현에서는 데이터베이스나 메트릭에서 계산
        return 95.5; // 예시 값
    }
    
    private double calculateAverageProcessingTime() {
        // 실제 구현에서는 데이터베이스나 메트릭에서 계산
        return 150.0; // 예시 값
    }
    
    private double calculateQueueProcessingDelay() {
        // 실제 구현에서는 데이터베이스나 메트릭에서 계산
        return 25.0; // 예시 값
    }
}
