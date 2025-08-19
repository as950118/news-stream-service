package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DashboardResponse {
    
    @JsonProperty("system_metrics")
    private final SystemMetricsResponse systemMetrics;
    
    @JsonProperty("performance_metrics")
    private final PerformanceMetricsResponse performanceMetrics;
    
    @JsonProperty("health_status")
    private final HealthStatusResponse healthStatus;
    
    @JsonProperty("alerts")
    private final java.util.List<AlertResponse> alerts;
    
    public DashboardResponse(SystemMetricsResponse systemMetrics,
                           PerformanceMetricsResponse performanceMetrics,
                           HealthStatusResponse healthStatus,
                           java.util.List<AlertResponse> alerts) {
        this.systemMetrics = systemMetrics;
        this.performanceMetrics = performanceMetrics;
        this.healthStatus = healthStatus;
        this.alerts = alerts;
    }
    
    public SystemMetricsResponse getSystemMetrics() {
        return systemMetrics;
    }
    
    public PerformanceMetricsResponse getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    public HealthStatusResponse getHealthStatus() {
        return healthStatus;
    }
    
    public java.util.List<AlertResponse> getAlerts() {
        return alerts;
    }
}
