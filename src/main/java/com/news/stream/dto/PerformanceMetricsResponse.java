package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PerformanceMetricsResponse {
    
    @JsonProperty("success_rate")
    private final double successRate;
    
    @JsonProperty("average_processing_time")
    private final double averageProcessingTime;
    
    @JsonProperty("queue_processing_delay")
    private final double queueProcessingDelay;
    
    public PerformanceMetricsResponse(double successRate, double averageProcessingTime, double queueProcessingDelay) {
        this.successRate = successRate;
        this.averageProcessingTime = averageProcessingTime;
        this.queueProcessingDelay = queueProcessingDelay;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public double getAverageProcessingTime() {
        return averageProcessingTime;
    }
    
    public double getQueueProcessingDelay() {
        return queueProcessingDelay;
    }
}
