package com.apishield.apikey.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriptionValidationDto {
    @JsonAlias({"active", "valid"})
    private boolean valid;
    private UUID subscriptionId;
    private String status;
    private int rateLimitRps;
    private int quotaMonthly;
    private String message;
}
