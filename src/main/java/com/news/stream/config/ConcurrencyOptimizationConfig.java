package com.news.stream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 동시성 처리를 최적화하는 설정 클래스
 * 다양한 작업 유형별로 전용 TaskExecutor를 구성합니다.
 */
@Configuration
public class ConcurrencyOptimizationConfig {
    
    @Value("${concurrency.core-pool-size:4}")
    private int corePoolSize;
    
    @Value("${concurrency.max-pool-size:8}")
    private int maxPoolSize;
    
    @Value("${concurrency.queue-capacity:100}")
    private int queueCapacity;
    
    /**
     * 뉴스 처리 전용 TaskExecutor (최적화용)
     */
    @Bean("newsOptimizationTaskExecutor")
    public ThreadPoolTaskExecutor newsOptimizationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("news-opt-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }
    
    /**
     * WebSocket 처리 전용 TaskExecutor
     */
    @Bean("websocketTaskExecutor")
    public ThreadPoolTaskExecutor websocketTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ws-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
    
    /**
     * 캐시 처리 전용 TaskExecutor
     */
    @Bean("cacheTaskExecutor")
    public ThreadPoolTaskExecutor cacheTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("cache-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
