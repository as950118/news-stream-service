package com.news.stream.dto;

/**
 * 시스템 메트릭 응답을 위한 DTO 클래스
 */
public record SystemMetricsResponse(
    QueueMetricsResponse queueMetrics,
    WebSocketMetricsResponse webSocketMetrics,
    ProcessingMetricsResponse processingMetrics
) {}
