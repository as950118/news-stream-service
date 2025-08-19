package com.news.stream.dto;

/**
 * 시스템 정보 응답을 위한 DTO 클래스
 */
public record SystemInfoResponse(
    String javaVersion,
    String osName,
    String osVersion,
    long totalMemory,
    long freeMemory,
    long maxMemory,
    long currentTime
) {}
