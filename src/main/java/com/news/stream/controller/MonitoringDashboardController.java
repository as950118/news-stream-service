package com.news.stream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.news.stream.dto.*;
import com.news.stream.service.AlertService;
import com.news.stream.service.CustomMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//@RestController
//@RequestMapping("/api/v1/monitoring")
//@Tag(name = "Monitoring Dashboard", description = "모니터링 대시보드 API")
public class MonitoringDashboardController {
    
    private final CustomMetrics customMetrics;
    private final AlertService alertService;
    private final Logger logger = LoggerFactory.getLogger(MonitoringDashboardController.class);
    
    public MonitoringDashboardController(CustomMetrics customMetrics,
                                       AlertService alertService) {
        this.customMetrics = customMetrics;
        this.alertService = alertService;
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "모니터링 대시보드", description = "시스템 모니터링 대시보드 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "대시보드 정보 조회 성공")
    })
    public ResponseEntity<DashboardResponse> getDashboard() {
        
        try {
            DashboardResponse response = new DashboardResponse(
                getSystemMetrics(),
                getPerformanceMetrics(),
                getHealthStatus(),
                getAlerts()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모니터링 대시보드 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/metrics/summary")
    @Operation(summary = "메트릭 요약", description = "주요 시스템 메트릭 요약을 조회합니다")
    public ResponseEntity<MetricsSummaryResponse> getMetricsSummary() {
        
        try {
            MetricsSummaryResponse response = new MetricsSummaryResponse(
                getQueueMetrics(),
                getWebSocketMetrics(),
                getProcessingMetrics(),
                getSystemMetrics()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("메트릭 요약 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/health-check")
    @Operation(summary = "수동 헬스체크", description = "수동으로 시스템 헬스체크를 실행합니다")
    public ResponseEntity<HealthCheckResponse> runHealthCheck() {
        
        try {
            alertService.checkSystemHealth();
            
            HealthCheckResponse response = new HealthCheckResponse(
                "SUCCESS",
                "헬스체크가 성공적으로 완료되었습니다",
                LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("수동 헬스체크 중 오류 발생", e);
            
            HealthCheckResponse response = new HealthCheckResponse(
                "ERROR",
                "헬스체크 중 오류가 발생했습니다: " + e.getMessage(),
                LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    private SystemMetricsResponse getSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        
        return new SystemMetricsResponse(
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            System.currentTimeMillis()
        );
    }
    
    private PerformanceMetricsResponse getPerformanceMetrics() {
        // 실제 구현에서는 메트릭 레지스트리에서 조회
        return new PerformanceMetricsResponse(
            95.5, // 성공률
            150.0, // 평균 처리 시간
            25.0   // 큐 처리 지연
        );
    }
    
    private HealthStatusResponse getHealthStatus() {
        // 실제 구현에서는 헬스 인디케이터에서 조회
        return new HealthStatusResponse(
            "UP",
            "시스템이 정상적으로 동작하고 있습니다",
            LocalDateTime.now()
        );
    }
    
    private List<AlertResponse> getAlerts() {
        // 실제 구현에서는 알림 서비스에서 조회
        return new ArrayList<>();
    }
    
    private QueueMetricsResponse getQueueMetrics() {
        // 실제 구현에서는 큐 서비스에서 조회
        return new QueueMetricsResponse(0, 1000, 1000);
    }
    
    private WebSocketMetricsResponse getWebSocketMetrics() {
        // 실제 구현에서는 WebSocket 서비스에서 조회
        return new WebSocketMetricsResponse(0, 0);
    }
    
    private ProcessingMetricsResponse getProcessingMetrics() {
        // 실제 구현에서는 처리 상태 서비스에서 조회
        return new ProcessingMetricsResponse(0, 0);
    }
}
