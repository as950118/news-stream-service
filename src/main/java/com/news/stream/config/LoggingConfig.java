package com.news.stream.config;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;

@Configuration
public class LoggingConfig {
    
    @Bean
    public LoggingSystem loggingSystem() {
        return LoggingSystem.get(ClassLoader.getSystemClassLoader());
    }
    
    @PostConstruct
    public void configureLogging() {
        // 로그 레벨 동적 설정
        LoggingSystem.get(ClassLoader.getSystemClassLoader())
            .setLogLevel("com.news.stream", LogLevel.INFO);
        
        LoggingSystem.get(ClassLoader.getSystemClassLoader())
            .setLogLevel("org.springframework.web", LogLevel.WARN);
        
        LoggingSystem.get(ClassLoader.getSystemClassLoader())
            .setLogLevel("org.hibernate.SQL", LogLevel.DEBUG);
    }
}
