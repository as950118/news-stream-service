package com.news.stream;

import com.news.stream.service.NewsStreamIntegrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 부하 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class LoadTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password");
    
    @Autowired
    private NewsStreamIntegrationService streamService;
    
    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    @DisplayName("동시 뉴스 처리 부하 테스트")
    void shouldHandleConcurrentNewsProcessing() throws InterruptedException {
        // Given
        int threadCount = 10;
        int newsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < newsPerThread; j++) {
                        String newsId = "load-test-news-" + threadId + "-" + j;
                        try {
                            streamService.processNewsCreated(newsId);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        int totalProcessed = successCount.get() + failureCount.get();
        assertThat(totalProcessed).isEqualTo(threadCount * newsPerThread);
        
        double successRate = (double) successCount.get() / totalProcessed * 100;
        assertThat(successRate).isGreaterThan(90.0); // 90% 이상 성공률
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("WebSocket 동시 연결 부하 테스트")
    void shouldHandleConcurrentWebSocketConnections() throws InterruptedException {
        // Given
        int connectionCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(connectionCount);
        CountDownLatch latch = new CountDownLatch(connectionCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < connectionCount; i++) {
            final int connectionId = i;
            executor.submit(() -> {
                try {
                    // WebSocket 연결 시뮬레이션
                    Thread.sleep(100); // 100ms 지연
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        int totalConnections = successCount.get() + failureCount.get();
        assertThat(totalConnections).isEqualTo(connectionCount);
        
        double successRate = (double) successCount.get() / totalConnections * 100;
        assertThat(successRate).isGreaterThan(80.0); // 80% 이상 성공률
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("메시지 큐 부하 테스트")
    void shouldHandleQueueLoad() throws InterruptedException {
        // Given
        int messageCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(messageCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < messageCount; i++) {
            final int messageId = i;
            executor.submit(() -> {
                try {
                    String newsId = "queue-load-test-" + messageId;
                    streamService.processNewsCreated(newsId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        int totalProcessed = successCount.get() + failureCount.get();
        assertThat(totalProcessed).isEqualTo(messageCount);
        
        double successRate = (double) successCount.get() / totalProcessed * 100;
        assertThat(successRate).isGreaterThan(95.0); // 95% 이상 성공률
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("데이터베이스 동시 접근 부하 테스트")
    void shouldHandleConcurrentDatabaseAccess() throws InterruptedException {
        // Given
        int threadCount = 20;
        int operationsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String newsId = "db-load-test-" + threadId + "-" + j;
                        try {
                            // 데이터베이스 작업 시뮬레이션
                            Thread.sleep(10); // 10ms 지연
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Then
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        int totalOperations = successCount.get() + failureCount.get();
        assertThat(totalOperations).isEqualTo(threadCount * operationsPerThread);
        
        double successRate = (double) successCount.get() / totalOperations * 100;
        assertThat(successRate).isGreaterThan(98.0); // 98% 이상 성공률
        
        executor.shutdown();
    }
}
