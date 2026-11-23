package com.apishield.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerOverviewResponse {
    private long totalRequests;
    private long successfulRequests;
    private long errorCount;
    private double errorRate;
    private double averageLatencyMs;
    private int p95LatencyMs;
}
