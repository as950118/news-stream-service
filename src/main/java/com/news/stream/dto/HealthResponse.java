package com.news.stream.dto;

/**
 * 헬스체크 응답을 위한 DTO 클래스
 */
public record HealthResponse(
    String status,
    String message
) {}
