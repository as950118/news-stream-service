package com.news.stream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StructuredLogging {
    
    public void logNewsProcessing(String newsId, String status, long processingTime) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("newsId", newsId);
        logData.put("status", status);
        logData.put("processingTime", processingTime);
        logData.put("timestamp", LocalDateTime.now());
        
        if ("SUCCESS".equals(status)) {
            log.info("뉴스 처리 완료: {}", logData);
        } else if ("FAILED".equals(status)) {
            log.error("뉴스 처리 실패: {}", logData);
        } else {
            log.warn("뉴스 처리 경고: {}", logData);
        }
    }
    
    public void logQueueOperation(String operation, String messageId, int queueSize) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("messageId", messageId);
        logData.put("queueSize", queueSize);
        logData.put("timestamp", LocalDateTime.now());
        
        log.info("큐 작업 수행: {}", logData);
    }
    
    public void logWebSocketEvent(String eventType, String sessionId, String customerId) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("eventType", eventType);
        logData.put("sessionId", sessionId);
        logData.put("customerId", customerId);
        logData.put("timestamp", LocalDateTime.now());
        
        log.info("WebSocket 이벤트: {}", logData);
    }
    
    public void logSystemHealth(String component, String status, Map<String, Object> details) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("component", component);
        logData.put("status", status);
        logData.put("details", details);
        logData.put("timestamp", LocalDateTime.now());
        
        if ("UP".equals(status)) {
            log.debug("시스템 컴포넌트 상태: {}", logData);
        } else if ("WARNING".equals(status)) {
            log.warn("시스템 컴포넌트 경고: {}", logData);
        } else {
            log.error("시스템 컴포넌트 오류: {}", logData);
        }
    }
    
    // WebSocket 관련 로깅
    public void logWebSocketConnectionFailed(String sessionId, String reason, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("reason", reason);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("WebSocket 연결 실패: {}", logData, exception);
    }
    
    public void logWebSocketConnectionFailed(String sessionId, String reason, Throwable throwable) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("reason", reason);
        logData.put("exceptionType", throwable.getClass().getSimpleName());
        logData.put("exceptionMessage", throwable.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("WebSocket 연결 실패: {}", logData, throwable);
    }
    
    public void logWebSocketMessageSendFailed(String sessionId, String customerId, String messageType, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("customerId", customerId);
        logData.put("messageType", messageType);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("WebSocket 메시지 전송 실패: {}", logData, exception);
    }
    
    public void logWebSocketMessageSendFailed(String sessionId, String customerId, String messageType, Throwable throwable) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("customerId", customerId);
        logData.put("messageType", messageType);
        logData.put("exceptionType", throwable.getClass().getSimpleName());
        logData.put("exceptionMessage", throwable.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("WebSocket 메시지 전송 실패: {}", logData, throwable);
    }
    
    public void logWebSocketAuthenticationFailed(String sessionId, String reason, String customerId) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("reason", reason);
        logData.put("customerId", customerId);
        logData.put("timestamp", LocalDateTime.now());
        
        log.warn("WebSocket 인증 실패: {}", logData);
    }
    
    public void logWebSocketConnectionAttempt(String sessionId, String customerId, String status, String details) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("sessionId", sessionId);
        logData.put("customerId", customerId);
        logData.put("status", status);
        logData.put("details", details);
        logData.put("timestamp", LocalDateTime.now());
        
        if ("SUCCESS".equals(status)) {
            log.info("WebSocket 연결 시도 성공: {}", logData);
        } else if ("FAILED".equals(status)) {
            log.warn("WebSocket 연결 시도 실패: {}", logData);
        } else {
            log.debug("WebSocket 연결 시도: {}", logData);
        }
    }
    
    // 데이터베이스 관련 로깅
    public void logDatabaseQueryFailed(String operation, String entityType, String entityId, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("entityType", entityType);
        logData.put("entityId", entityId);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("데이터베이스 쿼리 실패: {}", logData, exception);
    }
    
    public void logDatabaseConnectionFailed(String operation, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("데이터베이스 연결 실패: {}", logData, exception);
    }
    
    public void logNewsNotFound(String newsId, String operation) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("newsId", newsId);
        logData.put("operation", operation);
        logData.put("timestamp", LocalDateTime.now());
        
        log.warn("뉴스를 찾을 수 없음: {}", logData);
    }
    
    // 큐 관련 로깅
    public void logQueueMessageProcessingFailed(String messageId, String messageType, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("messageId", messageId);
        logData.put("messageType", messageType);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("큐 메시지 처리 실패: {}", logData, exception);
    }
    
    public void logQueueConnectionFailed(String operation, Exception exception) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("operation", operation);
        logData.put("exceptionType", exception.getClass().getSimpleName());
        logData.put("exceptionMessage", exception.getMessage());
        logData.put("timestamp", LocalDateTime.now());
        
        log.error("큐 연결 실패: {}", logData, exception);
    }
}
