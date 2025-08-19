package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * 성능 최적화 응답을 위한 DTO 클래스
 * 최적화 작업의 결과를 나타냅니다.
 */
public record OptimizationResponse(
    String status,           // 상태 (SUCCESS, ERROR)
    String message,          // 결과 메시지
    LocalDateTime timestamp  // 최적화 완료 시각
) {}
