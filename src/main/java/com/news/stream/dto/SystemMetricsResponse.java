package com.news.stream.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 시스템 메트릭 응답을 위한 DTO 클래스
 */
public class SystemMetricsResponse {
    
    @JsonProperty("java_version")
    private final String javaVersion;
    
    @JsonProperty("os_name")
    private final String osName;
    
    @JsonProperty("os_version")
    private final String osVersion;
    
    @JsonProperty("total_memory")
    private final long totalMemory;
    
    @JsonProperty("free_memory")
    private final long freeMemory;
    
    @JsonProperty("max_memory")
    private final long maxMemory;
    
    @JsonProperty("timestamp")
    private final long timestamp;
    
    public SystemMetricsResponse(String javaVersion, String osName, String osVersion,
                               long totalMemory, long freeMemory, long maxMemory, long timestamp) {
        this.javaVersion = javaVersion;
        this.osName = osName;
        this.osVersion = osVersion;
        this.totalMemory = totalMemory;
        this.freeMemory = freeMemory;
        this.maxMemory = maxMemory;
        this.timestamp = timestamp;
    }
    
    public String getJavaVersion() {
        return javaVersion;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public long getTotalMemory() {
        return totalMemory;
    }
    
    public long getFreeMemory() {
        return freeMemory;
    }
    
    public long getMaxMemory() {
        return maxMemory;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
