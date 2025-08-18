package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * 큐 상태 응답 DTO
 * 큐의 현재 상태 정보를 담습니다.
 * 
 * @param size 현재 큐에 있는 메시지 수
 * @param isEmpty 큐가 비어있는지 여부
 * @param capacity 큐의 전체 용량
 * @param remainingCapacity 큐의 남은 용량
 * @param utilization 큐 사용률 (0.0 ~ 1.0)
 * @param timestamp 응답 생성 시각
 */
public record QueueStatusResponse(
    int size,
    boolean isEmpty,
    int capacity,
    int remainingCapacity,
    double utilization,
    LocalDateTime timestamp
) {
    
    /**
     * 기본 생성자
     */
    public QueueStatusResponse {
        if (capacity < 0) {
            throw new IllegalArgumentException("큐 용량은 음수일 수 없습니다");
        }
        if (remainingCapacity < 0) {
            throw new IllegalArgumentException("남은 용량은 음수일 수 없습니다");
        }
        if (utilization < 0.0 || utilization > 1.0) {
            throw new IllegalArgumentException("사용률은 0.0 ~ 1.0 사이여야 합니다");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("타임스탬프는 null일 수 없습니다");
        }
    }
    
    /**
     * 큐 상태 응답을 생성합니다.
     * 
     * @param size 현재 큐에 있는 메시지 수
     * @param isEmpty 큐가 비어있는지 여부
     * @param capacity 큐의 전체 용량
     * @param remainingCapacity 큐의 남은 용량
     * @return QueueStatusResponse
     */
    public static QueueStatusResponse of(int size, boolean isEmpty, int capacity, int remainingCapacity) {
        double utilization = capacity == 0 ? 0.0 : (double) (capacity - remainingCapacity) / capacity;
        return new QueueStatusResponse(size, isEmpty, capacity, remainingCapacity, utilization, LocalDateTime.now());
    }
}
