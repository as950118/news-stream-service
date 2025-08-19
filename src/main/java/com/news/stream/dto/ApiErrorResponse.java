package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * API 에러 응답을 위한 DTO 클래스
 */
public record ApiErrorResponse(
    String errorCode,
    String message,
    String details,
    LocalDateTime timestamp,
    String path
) {}
