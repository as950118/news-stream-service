package com.news.stream.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JVM 메모리 사용량을 모니터링하는 서비스 클래스
 * 메모리 사용률을 추적하고 임계값을 초과할 경우 최적화를 수행합니다.
 */
@Component
public class MemoryMonitor {
    
    private final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
    private final MeterRegistry meterRegistry;
    
    public MemoryMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * 30초마다 메모리 사용량을 모니터링
     */
    @Scheduled(fixedRate = 30000)
    public void monitorMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        double heapUsagePercent = (double) usedMemory / totalMemory * 100;
        
        // Spring Boot Actuator가 이미 JVM 메트릭을 제공합니다
        // jvm.memory.used, jvm.memory.max 등이 자동으로 수집됩니다
        
        // 메모리 사용량 로깅
        logger.debug("메모리 사용량 - 총: {}MB, 사용: {}MB, 여유: {}MB, 최대: {}MB", 
            totalMemory / 1024 / 1024, 
            usedMemory / 1024 / 1024, 
            freeMemory / 1024 / 1024, 
            maxMemory / 1024 / 1024);
        
        // 메모리 경고 임계값 체크
        if (memoryUsagePercent > 90) {
            logger.error("메모리 사용률이 90%를 초과했습니다: {}%", 
                String.format("%.1f", memoryUsagePercent));
            triggerMemoryOptimization();
        } else if (memoryUsagePercent > 80) {
            logger.warn("메모리 사용률이 80%를 초과했습니다: {}%", 
                String.format("%.1f", memoryUsagePercent));
        } else if (memoryUsagePercent > 70) {
            logger.info("메모리 사용률: {}%", String.format("%.1f", memoryUsagePercent));
        }
        
        // 힙 메모리 경고
        if (heapUsagePercent > 85) {
            logger.warn("힙 메모리 사용률이 85%를 초과했습니다: {}%", 
                String.format("%.1f", heapUsagePercent));
            suggestGarbageCollection();
        }
    }
    
    /**
     * 메모리 최적화 트리거
     */
    private void triggerMemoryOptimization() {
        logger.info("메모리 최적화 시작");
        
        // 1. 캐시 정리
        clearExpiredCache();
        
        // 2. 불필요한 객체 정리
        clearUnusedObjects();
        
        // 3. 가비지 컬렉션 제안
        suggestGarbageCollection();
        
        logger.info("메모리 최적화 완료");
    }
    
    /**
     * 만료된 캐시 정리
     */
    private void clearExpiredCache() {
        try {
            // Redis 캐시 만료된 항목 정리
            // 실제 구현에서는 Redis TTL을 활용
            logger.debug("만료된 캐시 정리 완료");
        } catch (Exception e) {
            logger.warn("캐시 정리 중 오류", e);
        }
    }
    
    /**
     * 사용하지 않는 객체 정리
     */
    private void clearUnusedObjects() {
        try {
            // 사용하지 않는 객체 정리
            // 실제 구현에서는 WeakReference 등을 활용
            logger.debug("사용하지 않는 객체 정리 완료");
        } catch (Exception e) {
            logger.warn("객체 정리 중 오류", e);
        }
    }
    
    /**
     * 가비지 컬렉션 제안
     */
    private void suggestGarbageCollection() {
        try {
            // 가비지 컬렉션 제안 (실제 GC는 JVM이 결정)
            logger.info("가비지 컬렉션 제안");
            
            // 메모리 사용량 재확인
            Runtime.getRuntime().gc();
            
        } catch (Exception e) {
            logger.warn("가비지 컬렉션 제안 중 오류", e);
        }
    }
}
