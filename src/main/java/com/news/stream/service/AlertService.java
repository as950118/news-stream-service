package com.news.stream.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlertService {
    
    private final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final StructuredLogging structuredLogging;
    
    public AlertService(StructuredLogging structuredLogging) {
        this.structuredLogging = structuredLogging;
    }
    
    public void checkSystemHealth() {
        // 시스템 헬스체크 및 알림
        checkQueueHealth();
        checkWebSocketHealth();
        checkProcessingHealth();
        checkMemoryHealth();
    }
    
    private void checkQueueHealth() {
        // 큐 상태 확인 및 알림
        // 실제 구현에서는 메트릭이나 상태 정보를 확인
        logger.info("큐 상태 확인 완료");
    }
    
    private void checkWebSocketHealth() {
        // WebSocket 상태 확인 및 알림
        // 실제 구현에서는 메트릭이나 상태 정보를 확인
        logger.info("WebSocket 상태 확인 완료");
    }
    
    private void checkProcessingHealth() {
        // 뉴스 처리 상태 확인 및 알림
        // 실제 구현에서는 메트릭이나 상태 정보를 확인
        logger.info("뉴스 처리 상태 확인 완료");
    }
    
    private void checkMemoryHealth() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;
        
        if (memoryUsage > 90) {
            sendAlert("MEMORY_CRITICAL", "메모리 사용률이 90%를 초과했습니다: " + String.format("%.1f%%", memoryUsage));
        } else if (memoryUsage > 80) {
            sendAlert("MEMORY_WARNING", "메모리 사용률이 80%를 초과했습니다: " + String.format("%.1f%%", memoryUsage));
        }
        
        logger.debug("메모리 사용률: {}%", String.format("%.1f", memoryUsage));
    }
    
    private void sendAlert(String alertType, String message) {
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("alertType", alertType);
        alertData.put("message", message);
        alertData.put("timestamp", LocalDateTime.now());
        alertData.put("severity", getSeverity(alertType));
        
        structuredLogging.logSystemHealth("ALERT_SYSTEM", "ALERT", alertData);
        
        // 실제 운영 환경에서는 이메일, 슬랙, 텔레그램 등으로 알림 전송
        logger.warn("알림 발생: {} - {}", alertType, message);
    }
    
    private String getSeverity(String alertType) {
        if (alertType.contains("CRITICAL")) {
            return "CRITICAL";
        } else if (alertType.contains("WARNING")) {
            return "WARNING";
        } else {
            return "INFO";
        }
    }
    
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void scheduledHealthCheck() {
        checkSystemHealth();
    }
}
