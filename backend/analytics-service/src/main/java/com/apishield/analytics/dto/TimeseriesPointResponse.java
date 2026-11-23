package com.apishield.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeseriesPointResponse {
    private String timestamp;
    private long totalRequests;
    private long successfulRequests;
    private long clientErrors;
    private long serverErrors;
    private int avgLatencyMs;
}
