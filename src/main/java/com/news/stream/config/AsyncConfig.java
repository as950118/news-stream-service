package com.news.stream.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 작업 설정
 * 뉴스 배치 처리 및 스케줄링을 위한 TaskExecutor 구성
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Value("${news.batch.consumer-threads:2}")
    private int newsConsumerThreads;
    
    @Value("${news.batch.queue-capacity:100}")
    private int newsQueueCapacity;
    
    /**
     * 뉴스 작업용 TaskExecutor
     */
    @Bean(name = "newsTaskExecutor")
    public Executor newsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(newsConsumerThreads);
        executor.setMaxPoolSize(newsConsumerThreads * 2);
        executor.setQueueCapacity(newsQueueCapacity);
        executor.setThreadNamePrefix("news-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
