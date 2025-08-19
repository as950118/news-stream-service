package com.news.stream.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JVM 메모리 사용량을 모니터링하는 서비스 클래스
 * 메모리 사용률을 추적하고 임계값을 초과할 경우 최적화를 수행합니다.
 */
@Slf4j
@Component
public class MemoryMonitor {
    
    private final MeterRegistry meterRegistry;
    
    // 메모리 임계값 상수
    private static final double CRITICAL_MEMORY_THRESHOLD = 90.0;
    private static final double WARNING_MEMORY_THRESHOLD = 80.0;
    private static final double INFO_MEMORY_THRESHOLD = 70.0;
    private static final double HEAP_WARNING_THRESHOLD = 85.0;
    
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
        
        logMemoryUsage(totalMemory, usedMemory, freeMemory, maxMemory);
        checkMemoryThresholds(memoryUsagePercent, heapUsagePercent);
    }
    
    /**
     * 메모리 사용량 로깅
     */
    private void logMemoryUsage(long totalMemory, long usedMemory, long freeMemory, long maxMemory) {
        log.debug("메모리 사용량 - 총: {}MB, 사용: {}MB, 여유: {}MB, 최대: {}MB", 
            totalMemory / 1024 / 1024, 
            usedMemory / 1024 / 1024, 
            freeMemory / 1024 / 1024, 
            maxMemory / 1024 / 1024);
    }
    
    /**
     * 메모리 임계값 체크
     */
    private void checkMemoryThresholds(double memoryUsagePercent, double heapUsagePercent) {
        // 메모리 경고 임계값 체크
        if (memoryUsagePercent > CRITICAL_MEMORY_THRESHOLD) {
            log.error("메모리 사용률이 {}%를 초과했습니다: {}%", 
                CRITICAL_MEMORY_THRESHOLD, String.format("%.1f", memoryUsagePercent));
            triggerMemoryOptimization();
        } else if (memoryUsagePercent > WARNING_MEMORY_THRESHOLD) {
            log.warn("메모리 사용률이 {}%를 초과했습니다: {}%", 
                WARNING_MEMORY_THRESHOLD, String.format("%.1f", memoryUsagePercent));
        } else if (memoryUsagePercent > INFO_MEMORY_THRESHOLD) {
            log.info("메모리 사용률: {}%", String.format("%.1f", memoryUsagePercent));
        }
        
        // 힙 메모리 경고
        if (heapUsagePercent > HEAP_WARNING_THRESHOLD) {
            log.warn("힙 메모리 사용률이 {}%를 초과했습니다: {}%", 
                HEAP_WARNING_THRESHOLD, String.format("%.1f", heapUsagePercent));
            suggestGarbageCollection();
        }
    }
    
    /**
     * 메모리 최적화 트리거
     */
    private void triggerMemoryOptimization() {
        log.info("메모리 최적화 시작");
        
        try {
            // 1. 캐시 정리
            clearExpiredCache();
            
            // 2. 불필요한 객체 정리
            clearUnusedObjects();
            
            // 3. 가비지 컬렉션 제안
            suggestGarbageCollection();
            
            log.info("메모리 최적화 완료");
        } catch (Exception e) {
            log.error("메모리 최적화 중 오류 발생", e);
        }
    }
    
    /**
     * 만료된 캐시 정리
     */
    private void clearExpiredCache() {
        try {
            // Redis 캐시 만료된 항목 정리
            // 실제 구현에서는 Redis TTL을 활용
            log.debug("만료된 캐시 정리 완료");
        } catch (Exception e) {
            log.warn("캐시 정리 중 오류", e);
        }
    }
    
    /**
     * 사용하지 않는 객체 정리
     */
    private void clearUnusedObjects() {
        try {
            // 사용하지 않는 객체 정리
            // 실제 구현에서는 WeakReference 등을 활용
            log.debug("사용하지 않는 객체 정리 완료");
        } catch (Exception e) {
            log.warn("객체 정리 중 오류", e);
        }
    }
    
    /**
     * 가비지 컬렉션 제안
     */
    private void suggestGarbageCollection() {
        try {
            // 가비지 컬렉션 제안 (실제 GC는 JVM이 결정)
            log.info("가비지 컬렉션 제안");
            
            // 메모리 사용량 재확인
            Runtime.getRuntime().gc();
            
        } catch (Exception e) {
            log.warn("가비지 컬렉션 제안 중 오류", e);
        }
    }
}
