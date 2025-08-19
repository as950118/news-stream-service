package com.news.stream.dto;

public record CustomerDto(
    String id,
    String name,
    String token,
    boolean isActive
) {}
