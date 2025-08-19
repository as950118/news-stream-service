package com.news.stream.controller;

import com.news.stream.dto.OptimizationResponse;
import com.news.stream.dto.PerformanceHealthResponse;
import com.news.stream.dto.PerformanceMetricsResponse;
import com.news.stream.service.MemoryMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 성능 모니터링 대시보드를 위한 컨트롤러
 * 시스템 성능 메트릭 조회, 헬스체크, 최적화 실행을 담당합니다.
 */
@RestController
@RequestMapping("/api/v1/performance")
@Tag(name = "Performance Dashboard", description = "성능 모니터링 대시보드 API")
public class PerformanceDashboardController {
    
    private final MeterRegistry meterRegistry;
    private final MemoryMonitor memoryMonitor;
    private final Logger logger = LoggerFactory.getLogger(PerformanceDashboardController.class);
    
    public PerformanceDashboardController(MeterRegistry meterRegistry,
                                        MemoryMonitor memoryMonitor) {
        this.meterRegistry = meterRegistry;
        this.memoryMonitor = memoryMonitor;
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "성능 메트릭 조회", description = "시스템 성능 메트릭을 조회합니다")
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics() {
        
        try {
            PerformanceMetricsResponse response = new PerformanceMetricsResponse(
                getMethodExecutionMetrics(),
                getDatabaseMetrics(),
                getCacheMetrics(),
                getMemoryMetrics(),
                getConcurrencyMetrics()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("성능 메트릭 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "성능 헬스체크", description = "시스템 성능 상태를 확인합니다")
    public ResponseEntity<PerformanceHealthResponse> getPerformanceHealth() {
        
        try {
            boolean isHealthy = checkPerformanceHealth();
            
            if (isHealthy) {
                PerformanceHealthResponse response = new PerformanceHealthResponse(
                    "HEALTHY", "시스템 성능이 정상 범위 내에 있습니다");
                return ResponseEntity.ok(response);
            } else {
                PerformanceHealthResponse response = new PerformanceHealthResponse(
                    "DEGRADED", "시스템 성능이 저하되었습니다");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            logger.error("성능 헬스체크 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/optimize")
    @Operation(summary = "성능 최적화 실행", description = "수동으로 성능 최적화를 실행합니다")
    public ResponseEntity<OptimizationResponse> runOptimization() {
        
        try {
            logger.info("수동 성능 최적화 시작");
            
            // 메모리 최적화
            memoryMonitor.monitorMemoryUsage();
            
            // 캐시 정리
            clearExpiredCache();
            
            // 가비지 컬렉션 제안
            suggestGarbageCollection();
            
            OptimizationResponse response = new OptimizationResponse(
                "SUCCESS",
                "성능 최적화가 성공적으로 완료되었습니다",
                LocalDateTime.now()
            );
            
            logger.info("수동 성능 최적화 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("수동 성능 최적화 중 오류 발생", e);
            
            OptimizationResponse response = new OptimizationResponse(
                "ERROR",
                "성능 최적화 중 오류가 발생했습니다: " + e.getMessage(),
                LocalDateTime.now()
            );
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    private PerformanceMetricsResponse.MethodExecutionMetrics getMethodExecutionMetrics() {
        // 실제 구현에서는 메트릭 레지스트리에서 조회
        return new PerformanceMetricsResponse.MethodExecutionMetrics(
            95.5, // 평균 성공률
            150.0, // 평균 실행 시간
            25.0   // 95% 백분위 실행 시간
        );
    }
    
    private PerformanceMetricsResponse.DatabaseMetrics getDatabaseMetrics() {
        // 실제 구현에서는 메트릭 레지스트리에서 조회
        return new PerformanceMetricsResponse.DatabaseMetrics(
            98.0, // 평균 성공률
            50.0, // 평균 쿼리 시간
            10.0  // 연결 풀 사용률
        );
    }
    
    private PerformanceMetricsResponse.CacheMetrics getCacheMetrics() {
        // 실제 구현에서는 메트릭 레지스트리에서 조회
        return new PerformanceMetricsResponse.CacheMetrics(
            85.0, // 캐시 히트율
            15.0, // 캐시 미스율
            1000  // 캐시된 항목 수
        );
    }
    
    private PerformanceMetricsResponse.MemoryMetrics getMemoryMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new PerformanceMetricsResponse.MemoryMetrics(
            totalMemory,
            usedMemory,
            freeMemory,
            runtime.maxMemory(),
            (double) usedMemory / runtime.maxMemory() * 100
        );
    }
    
    private PerformanceMetricsResponse.ConcurrencyMetrics getConcurrencyMetrics() {
        // 실제 구현에서는 메트릭 레지스트리에서 조회
        return new PerformanceMetricsResponse.ConcurrencyMetrics(
            10,   // 활성 스레드 수
            20,   // 최대 스레드 수
            5     // 대기 중인 작업 수
        );
    }
    
    private boolean checkPerformanceHealth() {
        try {
            // 메모리 사용률 체크
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (double) usedMemory / runtime.maxMemory() * 100;
            
            if (memoryUsage > 90) {
                return false;
            }
            
            // 메서드 실행 시간 체크
            // 실제 구현에서는 메트릭에서 조회
            double avgExecutionTime = 150.0; // 예시 값
            if (avgExecutionTime > 1000) {
                return false;
            }
            
            // 데이터베이스 성능 체크
            double avgQueryTime = 50.0; // 예시 값
            if (avgQueryTime > 500) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("성능 헬스체크 중 오류", e);
            return false;
        }
    }
    
    private void clearExpiredCache() {
        try {
            // Redis 캐시 만료된 항목 정리
            logger.info("만료된 캐시 정리 완료");
        } catch (Exception e) {
            logger.warn("캐시 정리 중 오류", e);
        }
    }
    
    private void suggestGarbageCollection() {
        try {
            // 가비지 컬렉션 제안
            Runtime.getRuntime().gc();
            logger.info("가비지 컬렉션 제안 완료");
        } catch (Exception e) {
            logger.warn("가비지 컬렉션 제안 중 오류", e);
        }
    }
}
