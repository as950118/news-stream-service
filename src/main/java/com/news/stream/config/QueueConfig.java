package com.news.stream.config;

import com.news.stream.queue.LinkedBlockingMessageQueue;
import com.news.stream.queue.MessageQueue;
import com.news.stream.queue.NewsMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import org.springframework.core.task.TaskExecutor;

/**
 * 큐 관련 설정 클래스
 * 큐 용량, 컨슈머 스레드 수, 폴링 타임아웃 등을 설정합니다.
 * 프로파일에 따라 다른 큐 구현체를 사용할 수 있습니다.
 */
@Configuration
@EnableAsync
public class QueueConfig {
    
    @Value("${queue.capacity:1000}")
    private int queueCapacity;
    
    @Value("${queue.consumer-threads:2}")
    private int consumerThreads;
    
    @Value("${queue.poll-timeout:1000}")
    private long pollTimeout;
    
    /**
     * 로컬 메시지 큐 빈을 생성합니다.
     * local 프로파일에서 사용됩니다.
     */
    @Bean
    @Profile("local")
    public MessageQueue<NewsMessage> localMessageQueue() {
        return new LinkedBlockingMessageQueue(queueCapacity);
    }
    
    /**
     * 큐 컨슈머를 위한 TaskExecutor를 생성합니다.
     * 
     * @return TaskExecutor 빈
     */
    @Bean
    public TaskExecutor queueTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(consumerThreads);
        executor.setMaxPoolSize(consumerThreads * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("queue-consumer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
