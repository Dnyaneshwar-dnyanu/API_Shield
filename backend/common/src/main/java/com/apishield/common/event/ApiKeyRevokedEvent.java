package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event payload published when an API key is revoked.
 */
@Getter
@ToString
@Builder
public class ApiKeyRevokedEvent {

    @JsonProperty("apiKeyId")
    private final String apiKeyId;

    @JsonProperty("subscriptionId")
    private final String subscriptionId;

    @JsonProperty("developerId")
    private final String developerId;

    @JsonProperty("apiId")
    private final String apiId;

    @JsonProperty("prefix")
    private final String prefix;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("revokedAt")
    private final Instant revokedAt;

    @JsonCreator
    public ApiKeyRevokedEvent(
            @JsonProperty("apiKeyId") String apiKeyId,
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("developerId") String developerId,
            @JsonProperty("apiId") String apiId,
            @JsonProperty("prefix") String prefix,
            @JsonProperty("reason") String reason,
            @JsonProperty("revokedAt") Instant revokedAt) {
        this.apiKeyId = apiKeyId;
        this.subscriptionId = subscriptionId;
        this.developerId = developerId;
        this.apiId = apiId;
        this.prefix = prefix;
        this.reason = reason;
        this.revokedAt = revokedAt != null ? revokedAt : Instant.now();
    }
}
