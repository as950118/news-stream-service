package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class HealthStatusResponse {
    
    @JsonProperty("status")
    private final String status;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;
    
    public HealthStatusResponse(String status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
