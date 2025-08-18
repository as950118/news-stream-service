package com.news.stream.dto;

import java.util.List;

/**
 * WebSocket 상태 응답 DTO
 * WebSocket 연결 상태 정보를 반환할 때 사용합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
public record WebSocketStatusResponse(
    int activeSessionCount,
    int activeCustomerCount,
    List<String> activeSessionIds,
    List<String> activeCustomerIds
) {}
