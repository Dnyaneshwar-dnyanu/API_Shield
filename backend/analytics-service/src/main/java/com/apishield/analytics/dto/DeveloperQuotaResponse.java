package com.apishield.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperQuotaResponse {
    private String developerId;
    private long currentMonthUsage;
    private String period;
}
