package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * 뉴스 삭제 DTO
 * WebSocket을 통해 뉴스 삭제 정보를 전송할 때 사용합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
public record NewsDeletionDto(
    String id,
    String action,
    LocalDateTime deletedAt
) {}
