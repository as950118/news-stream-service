package com.news.stream;

import com.news.stream.service.CustomMetrics;
import com.news.stream.service.NewsProcessingStatusService;
import com.news.stream.service.StructuredLogging;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * 테스트 환경에서 Redis 설정을 비활성화하는 설정 클래스
 */
@TestConfiguration
public class TestRedisConfig {
    
    /**
     * TranslatedNewsService에서 필요한 의존성들을 Mock Bean으로 제공
     */
    @Bean
    @Primary
    public CustomMetrics customMetrics() {
        return mock(CustomMetrics.class);
    }
    
    @Bean
    @Primary
    public StructuredLogging structuredLogging() {
        return mock(StructuredLogging.class);
    }
    
    @Bean
    @Primary
    public NewsProcessingStatusService statusService() {
        return mock(NewsProcessingStatusService.class);
    }
}
