package com.news.stream.dto;

import java.util.List;

/**
 * WebSocket 상태 응답을 위한 DTO 클래스
 */
public record WebSocketStatusResponse(
    int activeSessionCount,
    int activeCustomerCount,
    List<String> activeSessionIds,
    List<String> activeCustomerIds
) {}
