package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class AlertResponse {
    
    @JsonProperty("alert_type")
    private final String alertType;
    
    @JsonProperty("message")
    private final String message;
    
    @JsonProperty("severity")
    private final String severity;
    
    @JsonProperty("timestamp")
    private final LocalDateTime timestamp;
    
    public AlertResponse(String alertType, String message, String severity, LocalDateTime timestamp) {
        this.alertType = alertType;
        this.message = message;
        this.severity = severity;
        this.timestamp = timestamp;
    }
    
    public String getAlertType() {
        return alertType;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getSeverity() {
        return severity;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
