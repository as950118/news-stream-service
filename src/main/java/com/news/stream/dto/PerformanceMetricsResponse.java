package com.news.stream.dto;

/**
 * 성능 메트릭 응답을 위한 DTO 클래스
 * 시스템의 전반적인 성능 지표를 포함합니다.
 */
public record PerformanceMetricsResponse(
    MethodExecutionMetrics methodExecution,
    DatabaseMetrics database,
    CacheMetrics cache,
    MemoryMetrics memory,
    ConcurrencyMetrics concurrency
) {
    
    /**
     * 메서드 실행 성능 메트릭
     */
    public record MethodExecutionMetrics(
        double successRate,      // 성공률 (%)
        double avgExecutionTime, // 평균 실행 시간 (ms)
        double p95ExecutionTime  // 95% 백분위 실행 시간 (ms)
    ) {}
    
    /**
     * 데이터베이스 성능 메트릭
     */
    public record DatabaseMetrics(
        double successRate,    // 성공률 (%)
        double avgQueryTime,   // 평균 쿼리 시간 (ms)
        double connectionPoolUsage // 연결 풀 사용률 (%)
    ) {}
    
    /**
     * 캐시 성능 메트릭
     */
    public record CacheMetrics(
        double hitRate,        // 캐시 히트율 (%)
        double missRate,       // 캐시 미스율 (%)
        int cachedItems        // 캐시된 항목 수
    ) {}
    
    /**
     * 메모리 사용량 메트릭
     */
    public record MemoryMetrics(
        long totalMemory,      // 총 메모리 (bytes)
        long usedMemory,       // 사용 중인 메모리 (bytes)
        long freeMemory,       // 여유 메모리 (bytes)
        long maxMemory,        // 최대 메모리 (bytes)
        double usagePercent    // 메모리 사용률 (%)
    ) {}
    
    /**
     * 동시성 처리 메트릭
     */
    public record ConcurrencyMetrics(
        int activeThreads,     // 활성 스레드 수
        int maxThreads,        // 최대 스레드 수
        int queuedTasks        // 대기 중인 작업 수
    ) {}
}
