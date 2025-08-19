package com.news.stream.dto;

import java.util.List;

/**
 * 처리 상태 응답을 위한 DTO 클래스
 */
public record ProcessingStatusResponse(
    int failedCount,
    int retryCount,
    List<String> failedNewsIds,
    List<String> retryNewsIds
) {}
