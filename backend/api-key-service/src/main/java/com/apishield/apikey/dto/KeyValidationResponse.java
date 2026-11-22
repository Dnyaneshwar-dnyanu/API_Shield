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
public class KeyValidationResponse {
    private boolean valid;
    private UUID keyId;
    private String developerId;
    private String subscriptionId;
    private String apiId;
    private int rateLimitRps;
    private int quotaMonthly;
    private String status;
    private String message;
}
