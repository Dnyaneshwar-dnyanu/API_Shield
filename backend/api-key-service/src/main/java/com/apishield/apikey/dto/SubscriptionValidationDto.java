package com.apishield.apikey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionValidationDto {
    private boolean valid;
    private UUID subscriptionId;
    private String status;
    private int rateLimitRps;
    private int quotaMonthly;
    private String message;
}
