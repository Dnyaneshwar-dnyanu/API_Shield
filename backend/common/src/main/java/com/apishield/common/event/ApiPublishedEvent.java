package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event payload published when an API is published or updated in the catalog.
 */
@Getter
@ToString
@Builder
public class ApiPublishedEvent {

    @JsonProperty("apiId")
    private final String apiId;

    @JsonProperty("ownerId")
    private final String ownerId;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("slug")
    private final String slug;

    @JsonProperty("category")
    private final String category;

    @JsonProperty("targetBackendUrl")
    private final String targetBackendUrl;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("publishedAt")
    private final Instant publishedAt;

    @JsonCreator
    public ApiPublishedEvent(
            @JsonProperty("apiId") String apiId,
            @JsonProperty("ownerId") String ownerId,
            @JsonProperty("name") String name,
            @JsonProperty("slug") String slug,
            @JsonProperty("category") String category,
            @JsonProperty("targetBackendUrl") String targetBackendUrl,
            @JsonProperty("status") String status,
            @JsonProperty("publishedAt") Instant publishedAt) {
        this.apiId = apiId;
        this.ownerId = ownerId;
        this.name = name;
        this.slug = slug;
        this.category = category;
        this.targetBackendUrl = targetBackendUrl;
        this.status = status;
        this.publishedAt = publishedAt != null ? publishedAt : Instant.now();
    }
}
