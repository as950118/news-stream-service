package com.news.stream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class StructuredLogging {
    
    private final Logger logger = LoggerFactory.getLogger(StructuredLogging.class);
    
    public void logNewsProcessing(String newsId, String status, long processingTime) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("newsId", newsId);
        logData.put("status", status);
        logData.put("processingTime", processingTime);
        logData.put("timestamp", LocalDateTime.now());
        
        if ("SUCCESS".equals(status)) {
            logger.info("뉴스 처리 완료: {}", logData);
        } else if ("FAILED".equals(status)) {
            logger.error("뉴스 처리 실패: {}", logData);
        } else {
            logger.warn("뉴스 처리 경고: {}", logData);
        }
    }
    
    public void logQueueOperation(String operation, String messageId, int queueSize) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("messageId", messageId);
        logData.put("queueSize", queueSize);
        logData.put("timestamp", LocalDateTime.now());
        
        logger.info("큐 작업 수행: {}", logData);
    }
    
    public void logWebSocketEvent(String eventType, String sessionId, String customerId) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("eventType", eventType);
        logData.put("sessionId", sessionId);
        logData.put("customerId", customerId);
        logData.put("timestamp", LocalDateTime.now());
        
        logger.info("WebSocket 이벤트: {}", logData);
    }
    
    public void logSystemHealth(String component, String status, Map<String, Object> details) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("component", component);
        logData.put("status", status);
        logData.put("details", details);
        logData.put("timestamp", LocalDateTime.now());
        
        if ("UP".equals(status)) {
            logger.debug("시스템 컴포넌트 상태: {}", logData);
        } else if ("WARNING".equals(status)) {
            logger.warn("시스템 컴포넌트 경고: {}", logData);
        } else {
            logger.error("시스템 컴포넌트 오류: {}", logData);
        }
    }
}
