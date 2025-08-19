package com.news.stream.controller;

import com.news.stream.dto.*;
import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.queue.MessageQueue;
import com.news.stream.queue.NewsMessage;
import com.news.stream.service.NewsProcessingStatusService;
import com.news.stream.websocket.WebSocketConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Status", description = "시스템 상태 확인 API")
public class SystemStatusController {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final WebSocketConnectionManager connectionManager;
    private final NewsProcessingStatusService processingStatusService;
    
    public SystemStatusController(MessageQueue<NewsMessage> messageQueue,
                                WebSocketConnectionManager connectionManager,
                                NewsProcessingStatusService processingStatusService) {
        this.messageQueue = messageQueue;
        this.connectionManager = connectionManager;
        this.processingStatusService = processingStatusService;
    }
    
    @GetMapping("/status")
    @Operation(summary = "시스템 전체 상태 확인", description = "시스템의 전체적인 상태를 확인합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "시스템 상태 확인 성공")
    })
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        log.debug("시스템 전체 상태 확인 요청");
        
        try {
            SystemStatusResponse response = new SystemStatusResponse(
                getQueueStatus(),
                getWebSocketStatus(),
                getProcessingStatus(),
                getSystemInfo()
            );
            
            log.debug("시스템 전체 상태 확인 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시스템 상태 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "시스템 헬스체크", description = "시스템의 기본적인 헬스 상태를 확인합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "시스템 정상"),
        @ApiResponse(responseCode = "503", description = "시스템 비정상")
    })
    public ResponseEntity<HealthResponse> getHealth() {
        log.debug("시스템 헬스체크 요청");
        
        boolean isHealthy = checkSystemHealth();
        
        if (isHealthy) {
            HealthResponse response = new HealthResponse("UP", "시스템이 정상적으로 동작하고 있습니다");
            log.debug("시스템 헬스체크 완료: 정상");
            return ResponseEntity.ok(response);
        } else {
            HealthResponse response = new HealthResponse("DOWN", "시스템에 문제가 발생했습니다");
            log.warn("시스템 헬스체크 완료: 비정상");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "시스템 메트릭 조회", description = "시스템의 주요 메트릭을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메트릭 조회 성공")
    })
    public ResponseEntity<SystemMetricsResponse> getSystemMetrics() {
        log.debug("시스템 메트릭 조회 요청");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            SystemMetricsResponse response = new SystemMetricsResponse(
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                runtime.totalMemory(),
                runtime.freeMemory(),
                runtime.maxMemory(),
                System.currentTimeMillis()
            );
            
            log.debug("시스템 메트릭 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("시스템 메트릭 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 시스템 헬스 상태를 확인합니다.
     */
    private boolean checkSystemHealth() {
        try {
            // 큐 상태 확인
            boolean queueHealthy = messageQueue != null;
            
            // WebSocket 연결 상태 확인
            boolean websocketHealthy = connectionManager != null;
            
            // 처리 상태 서비스 확인
            boolean processingHealthy = processingStatusService != null;
            
            return queueHealthy && websocketHealthy && processingHealthy;
            
        } catch (Exception e) {
            log.error("시스템 헬스 체크 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 큐 상태 정보를 생성합니다.
     */
    private QueueStatusResponse getQueueStatus() {
        try {
            return new QueueStatusResponse(
                messageQueue.size(),
                messageQueue.isEmpty(),
                -1, // 용량 정보는 별도로 제공
                -1   // 남은 용량 정보는 별도로 제공
            );
        } catch (Exception e) {
            log.warn("큐 상태 조회 중 오류 발생", e);
            return new QueueStatusResponse(0, true, -1, -1);
        }
    }
    
    /**
     * WebSocket 연결 상태 정보를 생성합니다.
     */
    private WebSocketStatusResponse getWebSocketStatus() {
        try {
            return new WebSocketStatusResponse(
                connectionManager.getActiveSessionCount(),
                connectionManager.getActiveCustomerCount(),
                connectionManager.getActiveSessionIds(),
                connectionManager.getActiveCustomerIds()
            );
        } catch (Exception e) {
            log.warn("WebSocket 상태 조회 중 오류 발생", e);
            return new WebSocketStatusResponse(0, 0, List.of(), List.of());
        }
    }
    
    /**
     * 뉴스 처리 상태 정보를 생성합니다.
     */
    private ProcessingStatusResponse getProcessingStatus() {
        try {
            List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
            List<NewsProcessingStatus> retryNews = processingStatusService.findRetryNews();
            
            List<String> failedNewsIds = failedNews.stream()
                .map(NewsProcessingStatus::getNewsId)
                .toList();
            List<String> retryNewsIds = retryNews.stream()
                .map(NewsProcessingStatus::getNewsId)
                .toList();
            
            return new ProcessingStatusResponse(
                failedNewsIds.size(),
                retryNewsIds.size(),
                failedNewsIds,
                retryNewsIds
            );
        } catch (Exception e) {
            log.warn("처리 상태 조회 중 오류 발생", e);
            return new ProcessingStatusResponse(0, 0, List.of(), List.of());
        }
    }
    
    /**
     * 시스템 기본 정보를 생성합니다.
     */
    private SystemInfoResponse getSystemInfo() {
        try {
            Runtime runtime = Runtime.getRuntime();
            return new SystemInfoResponse(
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                runtime.totalMemory(),
                runtime.freeMemory(),
                runtime.maxMemory(),
                System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.warn("시스템 정보 조회 중 오류 발생", e);
            return new SystemInfoResponse("Unknown", "Unknown", "Unknown", 0L, 0L, 0L, System.currentTimeMillis());
        }
    }
}
