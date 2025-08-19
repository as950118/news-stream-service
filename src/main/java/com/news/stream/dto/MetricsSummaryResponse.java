package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricsSummaryResponse {
    
    @JsonProperty("queue_metrics")
    private final QueueMetricsResponse queueMetrics;
    
    @JsonProperty("websocket_metrics")
    private final WebSocketMetricsResponse websocketMetrics;
    
    @JsonProperty("processing_metrics")
    private final ProcessingMetricsResponse processingMetrics;
    
    @JsonProperty("system_metrics")
    private final SystemMetricsResponse systemMetrics;
    
    public MetricsSummaryResponse(QueueMetricsResponse queueMetrics,
                                WebSocketMetricsResponse websocketMetrics,
                                ProcessingMetricsResponse processingMetrics,
                                SystemMetricsResponse systemMetrics) {
        this.queueMetrics = queueMetrics;
        this.websocketMetrics = websocketMetrics;
        this.processingMetrics = processingMetrics;
        this.systemMetrics = systemMetrics;
    }
    
    public QueueMetricsResponse getQueueMetrics() {
        return queueMetrics;
    }
    
    public WebSocketMetricsResponse getWebsocketMetrics() {
        return websocketMetrics;
    }
    
    public ProcessingMetricsResponse getProcessingMetrics() {
        return processingMetrics;
    }
    
    public SystemMetricsResponse getSystemMetrics() {
        return systemMetrics;
    }
}
