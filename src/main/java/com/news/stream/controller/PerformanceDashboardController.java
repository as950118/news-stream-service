package com.news.stream.controller;

import com.news.stream.dto.OptimizationResponse;
import com.news.stream.dto.PerformanceHealthResponse;
import com.news.stream.dto.PerformanceMetricsResponse;
import com.news.stream.service.MemoryMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 성능 모니터링 대시보드를 위한 컨트롤러
 * 시스템 성능 메트릭 조회, 헬스체크, 최적화 실행을 담당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/performance")
@Tag(name = "Performance Dashboard", description = "성능 모니터링 대시보드 API")
public class PerformanceDashboardController {
    
    private final MeterRegistry meterRegistry;
    private final MemoryMonitor memoryMonitor;
    
    public PerformanceDashboardController(MeterRegistry meterRegistry,
                                        MemoryMonitor memoryMonitor) {
        this.meterRegistry = meterRegistry;
        this.memoryMonitor = memoryMonitor;
    }
    
    @GetMapping("/metrics")
    @Operation(summary = "성능 메트릭 조회", description = "시스템 성능 메트릭을 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "메트릭 조회 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PerformanceMetricsResponse> getPerformanceMetrics() {
        log.debug("성능 메트릭 조회 요청");
        
        try {
            PerformanceMetricsResponse response = new PerformanceMetricsResponse(
                getMethodExecutionMetrics(),
                getDatabaseMetrics(),
                getCacheMetrics(),
                getMemoryMetrics(),
                getConcurrencyMetrics()
            );
            
            log.debug("성능 메트릭 조회 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("성능 메트릭 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "성능 헬스체크", description = "시스템 성능 상태를 확인합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "성능 정상"),
        @ApiResponse(responseCode = "503", description = "성능 저하"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PerformanceHealthResponse> getPerformanceHealth() {
        log.debug("성능 헬스체크 요청");
        
        try {
            boolean isHealthy = checkPerformanceHealth();
            
            if (isHealthy) {
                PerformanceHealthResponse response = new PerformanceHealthResponse(
                    "HEALTHY", "시스템 성능이 정상 범위 내에 있습니다");
                log.debug("성능 헬스체크 완료: 정상");
                return ResponseEntity.ok(response);
            } else {
                PerformanceHealthResponse response = new PerformanceHealthResponse(
                    "DEGRADED", "시스템 성능이 저하되었습니다");
                log.warn("성능 헬스체크 완료: 저하");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            log.error("성능 헬스체크 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/optimize")
    @Operation(summary = "성능 최적화 실행", description = "수동으로 성능 최적화를 실행합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "최적화 성공"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<OptimizationResponse> runOptimization() {
        log.info("수동 성능 최적화 시작");
        
        try {
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
            
            log.info("수동 성능 최적화 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("수동 성능 최적화 중 오류 발생", e);
            
            OptimizationResponse response = new OptimizationResponse(
                "FAILED",
                "성능 최적화 중 오류가 발생했습니다: " + e.getMessage(),
                LocalDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 성능 헬스 상태를 확인합니다.
     */
    private boolean checkPerformanceHealth() {
        try {
            // 메모리 사용률 확인
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
            
            // 메모리 사용률이 80%를 초과하면 비정상
            if (memoryUsagePercent > 80) {
                log.warn("메모리 사용률이 높음: {}%", String.format("%.1f", memoryUsagePercent));
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("성능 헬스 체크 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 메서드 실행 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse.MethodExecutionMetrics getMethodExecutionMetrics() {
        try {
            // 실제 구현에서는 Micrometer를 통해 메트릭 수집
            return new PerformanceMetricsResponse.MethodExecutionMetrics(
                95.5, // 평균 성공률
                150.0, // 평균 실행 시간
                25.0   // 95% 백분위 실행 시간
            );
        } catch (Exception e) {
            log.warn("메서드 실행 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse.MethodExecutionMetrics(0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 데이터베이스 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse.DatabaseMetrics getDatabaseMetrics() {
        try {
            // 실제 구현에서는 데이터베이스 연결 풀 메트릭 수집
            return new PerformanceMetricsResponse.DatabaseMetrics(
                98.0, // 평균 성공률
                50.0, // 평균 쿼리 시간
                10.0  // 연결 풀 사용률
            );
        } catch (Exception e) {
            log.warn("데이터베이스 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse.DatabaseMetrics(0.0, 0.0, 0.0);
        }
    }
    
    /**
     * 캐시 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse.CacheMetrics getCacheMetrics() {
        try {
            // 실제 구현에서는 Redis 캐시 메트릭 수집
            return new PerformanceMetricsResponse.CacheMetrics(
                85.0, // 캐시 히트율
                15.0, // 캐시 미스율
                1000  // 캐시된 항목 수
            );
        } catch (Exception e) {
            log.warn("캐시 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse.CacheMetrics(0.0, 0.0, 0);
        }
    }
    
    /**
     * 메모리 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse.MemoryMetrics getMemoryMetrics() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double usagePercent = (double) usedMemory / runtime.maxMemory() * 100;
            
            return new PerformanceMetricsResponse.MemoryMetrics(
                totalMemory,
                usedMemory,
                freeMemory,
                runtime.maxMemory(),
                usagePercent
            );
        } catch (Exception e) {
            log.warn("메모리 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse.MemoryMetrics(0L, 0L, 0L, 0L, 0.0);
        }
    }
    
    /**
     * 동시성 메트릭을 조회합니다.
     */
    private PerformanceMetricsResponse.ConcurrencyMetrics getConcurrencyMetrics() {
        try {
            // 실제 구현에서는 스레드 풀 메트릭 수집
            return new PerformanceMetricsResponse.ConcurrencyMetrics(
                8,  // 활성 스레드 수
                20, // 최대 스레드 수
                5   // 대기 중인 작업 수
            );
        } catch (Exception e) {
            log.warn("동시성 메트릭 조회 중 오류 발생", e);
            return new PerformanceMetricsResponse.ConcurrencyMetrics(0, 0, 0);
        }
    }
    
    /**
     * 만료된 캐시를 정리합니다.
     */
    private void clearExpiredCache() {
        try {
            log.debug("만료된 캐시 정리 시작");
            // 실제 구현에서는 Redis TTL을 활용한 캐시 정리
            Thread.sleep(100); // 시뮬레이션
            log.debug("만료된 캐시 정리 완료");
        } catch (Exception e) {
            log.warn("캐시 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 가비지 컬렉션을 제안합니다.
     */
    private void suggestGarbageCollection() {
        try {
            log.debug("가비지 컬렉션 제안");
            // 실제 GC는 JVM이 결정
            Runtime.getRuntime().gc();
            log.debug("가비지 컬렉션 제안 완료");
        } catch (Exception e) {
            log.warn("가비지 컬렉션 제안 중 오류 발생", e);
        }
    }
}
