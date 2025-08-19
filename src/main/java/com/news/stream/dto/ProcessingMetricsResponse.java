package com.news.stream.dto;

/**
 * 처리 메트릭 응답을 위한 DTO 클래스
 */
public record ProcessingMetricsResponse(
    int failedCount,
    int retryCount
) {}
