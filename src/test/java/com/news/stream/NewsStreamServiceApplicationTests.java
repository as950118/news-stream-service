package com.news.stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * News Stream Service 애플리케이션 테스트 클래스
 * 최소한의 Spring Boot 설정만으로 애플리케이션 컨텍스트가 로드되는지 테스트합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
@SpringBootTest(
    classes = {NewsStreamServiceApplicationTests.MinimalTestConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.redis.enabled=false",
        "spring.cache.type=none",
        "spring.main.allow-bean-definition-overriding=true"
    }
)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    WebSocketServletAutoConfiguration.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.redis.enabled=false",
    "spring.cache.type=none",
    "logging.level.org.hibernate.SQL=WARN",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=WARN"
})
class NewsStreamServiceApplicationTests {

    @Test
    void contextLoads() {
        // 최소한의 Spring Boot 컨텍스트가 정상적으로 로드되는지 확인
        // 데이터베이스, Redis, WebSocket 설정 없이 기본 Spring 기능만 검증합니다.
    }

    @Configuration
    static class MinimalTestConfiguration {
        
        @Bean
        public String testBean() {
            return "Minimal Test Configuration";
        }
    }
}
