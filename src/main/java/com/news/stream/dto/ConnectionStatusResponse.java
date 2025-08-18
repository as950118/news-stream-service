package com.news.stream.dto;

import java.time.LocalDateTime;

public record ConnectionStatusResponse(
    String customerId,
    String customerName,
    String connectionId,
    boolean isConnected,
    LocalDateTime connectedAt
) {}
