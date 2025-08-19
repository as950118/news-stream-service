package com.news.stream.dto;

import java.util.List;

/**
 * 페이징 응답을 위한 DTO 클래스
 * @param <T> 응답 데이터 타입
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
