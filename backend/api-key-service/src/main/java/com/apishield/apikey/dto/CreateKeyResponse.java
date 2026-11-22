package com.apishield.apikey.dto;

import com.apishield.apikey.model.KeyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKeyResponse {
    private UUID id;
    private String name;
    private String apiKey; // Plaintext raw key - returned once!
    private String keyPrefix;
    private String apiId;
    private String subscriptionId;
    private KeyStatus status;
    private int rateLimitRps;
    private int quotaMonthly;
    private Instant createdAt;
}
