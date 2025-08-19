package com.news.stream.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

import jakarta.annotation.PostConstruct;

/**
 * 메모리 최적화를 위한 설정 클래스
 * JVM 메모리 모니터링 및 최적화 설정을 담당합니다.
 */
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy
@EnableAsync
public class MemoryOptimizationConfig {
    
    // Spring Boot Actuator가 자동으로 JVM 메트릭을 제공합니다
    // 별도의 Bean 설정이 필요하지 않습니다
    
    @PostConstruct
    public void configureMemorySettings() {
        // JVM 메모리 설정 최적화
        Runtime runtime = Runtime.getRuntime();
        
        // 가상 스레드 풀 크기 설정
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "2");
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "100");
        
        // GC 최적화 설정
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", 
            String.valueOf(Runtime.getRuntime().availableProcessors()));
        
        // 로깅 최적화
        System.setProperty("logback.configurationFile", "logback-optimized.xml");
    }
}
