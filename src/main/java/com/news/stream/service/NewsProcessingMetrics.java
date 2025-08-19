package com.news.stream.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 뉴스 처리 성능 모니터링
 * Micrometer를 사용하여 뉴스 처리 관련 메트릭 수집
 */
@Component
public class NewsProcessingMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public NewsProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }
    
    /**
     * 메트릭 초기화
     */
    private void initializeMetrics() {
        // 뉴스 처리 성공 카운터
        Counter.builder("news.processing.success")
            .description("성공적으로 처리된 뉴스 수")
            .register(meterRegistry);
        
        // 뉴스 처리 실패 카운터
        Counter.builder("news.processing.failure")
            .description("처리 실패한 뉴스 수")
            .register(meterRegistry);
        
        // 뉴스 처리 시간 타이머
        Timer.builder("news.processing.time")
            .description("뉴스 처리 소요 시간")
            .register(meterRegistry);
        
        // 배치 처리 크기 게이지
        Gauge.builder("news.batch.size", 0, Integer::valueOf)
            .description("배치 처리 크기")
            .register(meterRegistry);
        
        // 재시도 횟수 카운터
        Counter.builder("news.retry.count")
            .description("뉴스 재시도 횟수")
            .register(meterRegistry);
    }
    
    /**
     * 성공 메트릭 기록
     */
    public void recordSuccess() {
        Counter.builder("news.processing.success")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 실패 메트릭 기록
     */
    public void recordFailure() {
        Counter.builder("news.processing.failure")
            .register(meterRegistry)
            .increment();
    }
    
    /**
     * 처리 시간 메트릭 기록
     */
    public void recordProcessingTime(long timeInMs) {
        Timer.builder("news.processing.time")
            .register(meterRegistry)
            .record(timeInMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 재시도 메트릭 기록
     */
    public void recordRetry() {
        Counter.builder("news.retry.count")
            .register(meterRegistry)
            .increment();
    }
}
