package com.news.stream.dto;

import java.time.LocalDateTime;

/**
 * 뉴스 업데이트 DTO
 * WebSocket을 통해 뉴스 업데이트 정보를 전송할 때 사용합니다.
 * 
 * @author News Stream Service Team
 * @version 1.0.0
 */
public record NewsUpdateDto(
    String id,
    String title,
    String content,
    LocalDateTime publishedAt,
    String action
) {}
