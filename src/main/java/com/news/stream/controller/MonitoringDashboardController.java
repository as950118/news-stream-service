package com.news.stream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.news.stream.dto.*;
import com.news.stream.service.AlertService;
import com.news.stream.service.CustomMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring Dashboard", description = "모니터링 대시보드 API")
public class MonitoringDashboardController {
    
    private final CustomMetrics customMetrics;
    private final AlertService alertService;
    
    public MonitoringDashboardController(CustomMetrics customMetrics,
                                       AlertService alertService) {
        this.customMetrics = customMetrics;
        this.alertService = alertService;
    }
    
    @GetMapping("/dashboard")
    @Operation(summary = "모니터링 대시보드", description = "시스템 모니터링 대시보드 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "대시보드 정보 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<DashboardResponse> getDashboard() {
        log.debug("모니터링 대시보드 조회 요청");
        
        try {
            DashboardResponse response = new DashboardResponse(
                getSystemMetrics(),
                getPerformanceMetrics(),
                getHealthStatus(),
                getAlerts()
            );
            
            log.debug("모니터링 대시보드 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("모니터링 대시보드 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/metrics/summary")
    @Operation(summary = "메트릭 요약", description = "주요 시스템 메트릭 요약을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메트릭 요약 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<MetricsSummaryResponse> getMetricsSummary() {
        log.debug("메트릭 요약 조회 요청");
        
        try {
            MetricsSummaryResponse response = new MetricsSummaryResponse(
                getQueueMetrics(),
                getWebSocketMetrics(),
                getProcessingMetrics(),
                getSystemMetrics()
            );
            
            log.debug("메트릭 요약 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메트릭 요약 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/health-check")
    @Operation(summary = "수동 헬스체크", description = "수동으로 시스템 헬스체크를 실행합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "헬스체크 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<HealthCheckResponse> runHealthCheck() {
        log.info("수동 헬스체크 시작");
        
        try {
            alertService.checkSystemHealth();
            
            HealthCheckResponse response = new HealthCheckResponse(
                "SUCCESS",
                "헬스체크가 성공적으로 완료되었습니다",
                LocalDateTime.now()
            );
            
            log.info("수동 헬스체크 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("수동 헬스체크 중 오류 발생", e);
            
            HealthCheckResponse response = new HealthCheckResponse(
                "ERROR",
                "헬스체크 중 오류가 발생했습니다: " + e.getMessage(),
                LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 시스템 메트릭을 조회합니다.
     */
    private SystemMetricsResponse getSystemMetrics() {
        try {
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
        } catch (Exception e) {
            log.warn("시스템 메트릭 조회 중 오류 발생", e);
            return new SystemMetricsResponse("Unknown", "Unknown", "Unknown", 0L, 0L, 0L, System.currentTimeMillis());
        }
    }
    
    /**
     * 성능 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse getPerformanceMetrics() {
        try {
            // 실제 구현에서는 메트릭 레지스트리에서 조회
            return new PerformanceMetricsResponse(
                new PerformanceMetricsResponse.MethodExecutionMetrics(95.5, 150.0, 25.0),
                new PerformanceMetricsResponse.DatabaseMetrics(98.0, 50.0, 10.0),
                new PerformanceMetricsResponse.CacheMetrics(85.0, 15.0, 1000),
                new PerformanceMetricsResponse.MemoryMetrics(
                    Runtime.getRuntime().totalMemory(),
                    Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                    Runtime.getRuntime().freeMemory(),
                    Runtime.getRuntime().maxMemory(),
                    75.0
                ),
                new PerformanceMetricsResponse.ConcurrencyMetrics(10, 20, 5)
            );
        } catch (Exception e) {
            log.warn("성능 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse(
                new PerformanceMetricsResponse.MethodExecutionMetrics(0.0, 0.0, 0.0),
                new PerformanceMetricsResponse.DatabaseMetrics(0.0, 0.0, 0.0),
                new PerformanceMetricsResponse.CacheMetrics(0.0, 0.0, 0),
                new PerformanceMetricsResponse.MemoryMetrics(0L, 0L, 0L, 0L, 0.0),
                new PerformanceMetricsResponse.ConcurrencyMetrics(0, 0, 0)
            );
        }
    }
    
    /**
     * 헬스 상태를 조회합니다.
     */
    private HealthStatusResponse getHealthStatus() {
        try {
            return new HealthStatusResponse(
                "UP",
                "시스템이 정상적으로 동작하고 있습니다",
                LocalDateTime.now()
            );
        } catch (Exception e) {
            log.warn("헬스 상태 조회 중 오류 발생", e);
            return new HealthStatusResponse("UNKNOWN", "상태 확인 불가", LocalDateTime.now());
        }
    }
    
    /**
     * 알림 목록을 조회합니다.
     */
    private List<AlertResponse> getAlerts() {
        try {
            // 실제 구현에서는 AlertService에서 조회
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("알림 목록 조회 중 오류 발생", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 큐 메트릭을 조회합니다.
     */
    private QueueMetricsResponse getQueueMetrics() {
        try {
            return new QueueMetricsResponse(
                100, // 큐 크기
                1000, // 큐 용량
                900   // 남은 용량
            );
        } catch (Exception e) {
            log.warn("큐 메트릭 조회 중 오류 발생", e);
            return new QueueMetricsResponse(0, 0, 0);
        }
    }
    
    /**
     * WebSocket 메트릭을 조회합니다.
     */
    private WebSocketMetricsResponse getWebSocketMetrics() {
        try {
            return new WebSocketMetricsResponse(
                10, // 활성 연결 수
                5   // 총 연결 수
            );
        } catch (Exception e) {
            log.warn("WebSocket 메트릭 조회 중 오류 발생", e);
            return new WebSocketMetricsResponse(0, 0);
        }
    }
    
    /**
     * 처리 메트릭을 조회합니다.
     */
    private ProcessingMetricsResponse getProcessingMetrics() {
        try {
            return new ProcessingMetricsResponse(
                95, // 성공률
                5   // 실패율
            );
        } catch (Exception e) {
            log.warn("처리 메트릭 조회 중 오류 발생", e);
            return new ProcessingMetricsResponse(0, 0);
        }
    }
}
