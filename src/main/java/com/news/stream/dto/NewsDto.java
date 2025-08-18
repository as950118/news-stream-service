package com.news.stream.dto;

import java.time.LocalDateTime;

public record NewsDto(
    String id,
    String title,
    String content,
    LocalDateTime publishedAt
) {}
