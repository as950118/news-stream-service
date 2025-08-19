package com.news.stream.dto;

/**
 * 시스템 상태 응답을 위한 DTO 클래스
 */
public record SystemStatusResponse(
    QueueStatusResponse queueStatus,
    WebSocketStatusResponse webSocketStatus,
    ProcessingStatusResponse processingStatus,
    SystemInfoResponse systemInfo
) {}
