package com.news.stream.dto;

/**
 * 큐 상태 응답을 위한 DTO 클래스
 */
public record QueueStatusResponse(
    int size,
    boolean isEmpty,
    int capacity,
    int remainingCapacity
) {}
