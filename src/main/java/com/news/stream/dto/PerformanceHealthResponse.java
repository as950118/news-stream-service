package com.news.stream.dto;

/**
 * 성능 헬스체크 응답을 위한 DTO 클래스
 * 시스템의 성능 상태를 나타냅니다.
 */
public record PerformanceHealthResponse(
    String status,    // 상태 (HEALTHY, DEGRADED, UNHEALTHY)
    String message    // 상태 설명 메시지
) {}
