package com.news.stream.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

//@Component
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeCustomMetrics();
    }
    
    private void initializeCustomMetrics() {
        // 뉴스 처리 성공률 게이지
        Gauge.builder("news.processing.success.rate", 0.0, Double::valueOf)
            .description("뉴스 처리 성공률")
            .register(meterRegistry);
        
        // 평균 처리 시간 게이지
        Gauge.builder("news.processing.avg.time", 0.0, Double::valueOf)
            .description("평균 뉴스 처리 시간 (밀리초)")
            .register(meterRegistry);
        
        // 큐 처리 지연 시간 게이지
        Gauge.builder("queue.processing.delay", 0.0, Double::valueOf)
            .description("큐 처리 지연 시간 (밀리초)")
            .register(meterRegistry);
    }
    
    public void updateSuccessRate(double successRate) {
        Gauge.builder("news.processing.success.rate", successRate, Double::valueOf)
            .register(meterRegistry);
    }
    
    public void updateAverageProcessingTime(double avgTime) {
        Gauge.builder("news.processing.avg.time", avgTime, Double::valueOf)
            .register(meterRegistry);
    }
    
    public void updateQueueProcessingDelay(double delay) {
        Gauge.builder("queue.processing.delay", delay, Double::valueOf)
            .register(meterRegistry);
    }
    
    public void incrementNewsProcessed() {
        Counter.builder("news.total.processed")
            .description("총 처리된 뉴스 수")
            .register(meterRegistry)
            .increment();
    }
    
    public void incrementNewsFailed() {
        Counter.builder("news.total.failed")
            .description("총 처리 실패한 뉴스 수")
            .register(meterRegistry)
            .increment();
    }
    
    public void recordProcessingTime(long timeInMs) {
        Timer.builder("news.processing.duration")
            .description("뉴스 처리 소요 시간")
            .register(meterRegistry)
            .record(timeInMs, TimeUnit.MILLISECONDS);
    }
}
