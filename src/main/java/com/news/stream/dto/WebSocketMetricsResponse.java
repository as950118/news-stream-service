package com.news.stream.dto;

/**
 * WebSocket 메트릭 응답을 위한 DTO 클래스
 */
public record WebSocketMetricsResponse(
    int activeSessionCount,
    int activeCustomerCount
) {}
