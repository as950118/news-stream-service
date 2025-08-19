package com.news.stream.dto;

/**
 * 큐 메트릭 응답을 위한 DTO 클래스
 */
public record QueueMetricsResponse(
    int size,
    int capacity,
    int remainingCapacity
) {}
