package com.news.stream.controller;

import com.news.stream.dto.*;
import com.news.stream.model.NewsProcessingStatus;
import com.news.stream.queue.MessageQueue;
import com.news.stream.queue.NewsMessage;
import com.news.stream.service.NewsProcessingStatusService;
import com.news.stream.websocket.WebSocketConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System Status", description = "시스템 상태 확인 API")
public class SystemStatusController {
    
    private final MessageQueue<NewsMessage> messageQueue;
    private final WebSocketConnectionManager connectionManager;
    private final NewsProcessingStatusService processingStatusService;
    private final Logger logger = LoggerFactory.getLogger(SystemStatusController.class);
    
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
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시스템 상태 확인 성공")
    })
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        
        try {
            SystemStatusResponse response = new SystemStatusResponse(
                getQueueStatus(),
                getWebSocketStatus(),
                getProcessingStatus(),
                getSystemInfo()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("시스템 상태 확인 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "시스템 헬스체크", description = "시스템의 기본적인 헬스 상태를 확인합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시스템 정상"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "시스템 비정상")
    })
    public ResponseEntity<HealthResponse> getHealth() {
        
        boolean isHealthy = checkSystemHealth();
        
        if (isHealthy) {
            HealthResponse response = new HealthResponse("UP", "시스템이 정상적으로 동작하고 있습니다");
            return ResponseEntity.ok(response);
        } else {
            HealthResponse response = new HealthResponse("DOWN", "시스템에 문제가 발생했습니다");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "시스템 메트릭 조회", description = "시스템의 주요 메트릭을 조회합니다")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메트릭 조회 성공")
    })
    public ResponseEntity<SystemMetricsResponse> getSystemMetrics() {
        
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
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("시스템 메트릭 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private QueueStatusResponse getQueueStatus() {
        return new QueueStatusResponse(
            messageQueue.size(),
            messageQueue.isEmpty(),
            1000, // 기본 큐 용량
            1000 - messageQueue.size() // 남은 용량
        );
    }
    
    private WebSocketStatusResponse getWebSocketStatus() {
        return new WebSocketStatusResponse(
            connectionManager.getActiveSessionCount(),
            connectionManager.getActiveCustomerCount(),
            connectionManager.getActiveSessionIds(),
            connectionManager.getActiveCustomerIds()
        );
    }
    
    private ProcessingStatusResponse getProcessingStatus() {
        List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
        List<NewsProcessingStatus> retryNews = processingStatusService.findRetryNews();
        
        return new ProcessingStatusResponse(
            failedNews.size(),
            retryNews.size(),
            failedNews.stream().map(NewsProcessingStatus::getNewsId).collect(Collectors.toList()),
            retryNews.stream().map(NewsProcessingStatus::getNewsId).collect(Collectors.toList())
        );
    }
    
    private SystemInfoResponse getSystemInfo() {
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
    }
    
    private boolean checkSystemHealth() {
        try {
            // 큐 상태 확인
            if (messageQueue.size() > 900) { // 1000 * 0.9
                return false;
            }
            
            // WebSocket 연결 상태 확인
            if (connectionManager.getActiveSessionCount() > 1000) {
                return false;
            }
            
            // 처리 실패 뉴스 수 확인
            List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
            if (failedNews.size() > 100) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("시스템 헬스체크 중 오류 발생", e);
            return false;
        }
    }
    
    private QueueMetricsResponse getQueueMetrics() {
        return new QueueMetricsResponse(
            messageQueue.size(),
            1000, // 기본 큐 용량
            1000 - messageQueue.size() // 남은 용량
        );
    }
    
    private WebSocketMetricsResponse getWebSocketMetrics() {
        return new WebSocketMetricsResponse(
            connectionManager.getActiveSessionCount(),
            connectionManager.getActiveCustomerCount()
        );
    }
    
    private ProcessingMetricsResponse getProcessingMetrics() {
        List<NewsProcessingStatus> failedNews = processingStatusService.findFailedNews();
        List<NewsProcessingStatus> retryNews = processingStatusService.findRetryNews();
        
        return new ProcessingMetricsResponse(
            failedNews.size(),
            retryNews.size()
        );
    }
}
