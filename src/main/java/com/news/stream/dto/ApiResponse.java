package com.news.stream.dto;

/**
 * API 응답을 위한 DTO 클래스
 * @param <T> 응답 데이터 타입
 */
public record ApiResponse<T>(
    String status,
    String message,
    T data
) {}
