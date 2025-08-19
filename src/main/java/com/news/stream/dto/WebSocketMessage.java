package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 래퍼 DTO
 * WebSocket을 통해 전송되는 모든 메시지의 공통 구조를 정의합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
public record WebSocketMessage<T>(
    String type,
    T payload,
    LocalDateTime timestamp
) {
    /**
     * 현재 시간으로 타임스탬프를 설정하여 메시지를 생성합니다.
     * 
     * @param type 메시지 타입
     * @param payload 메시지 페이로드
     * @return WebSocketMessage 인스턴스
     */
    public static <T> WebSocketMessage<T> of(String type, T payload) {
        return new WebSocketMessage<>(type, payload, LocalDateTime.now());
    }
}
