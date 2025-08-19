package com.news.stream;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.Executor;

/**
 * 통합 테스트 환경 설정 클래스
 * 백그라운드 스레드들과 리소스 관리를 최적화합니다.
 */
@TestConfiguration
@EnableAsync
@EnableScheduling
@TestPropertySource(properties = {
    "spring.task.execution.pool.core-size=1",
    "spring.task.execution.pool.max-size=2",
    "spring.task.execution.pool.queue-capacity=10",
    "spring.task.execution.thread-name-prefix=integration-test-",
    "spring.task.scheduling.pool.size=1",
    "spring.task.scheduling.thread-name-prefix=integration-test-scheduler-"
})
public class IntegrationTestConfig {
    
    /**
     * 통합 테스트용 비동기 작업 실행기
     * 스레드 풀 크기를 제한하여 리소스 사용량을 최소화합니다.
     */
    @Bean
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("integration-test-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
    
    /**
     * 통합 테스트용 스케줄링 작업 실행기
     * 단일 스레드로 제한하여 테스트 환경에서의 안정성을 높입니다.
     */
    @Bean
    @Primary
    public Executor schedulingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(5);
        executor.setThreadNamePrefix("integration-test-scheduler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
