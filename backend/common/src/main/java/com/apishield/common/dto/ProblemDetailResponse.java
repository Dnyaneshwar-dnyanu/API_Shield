package com.apishield.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Map;

/**
 * Standard RFC 7807 Problem Details representation for HTTP error responses.
 */
@Getter
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetailResponse {

    @JsonProperty("type")
    private final String type;

    @JsonProperty("title")
    private final String title;

    @JsonProperty("status")
    private final int status;

    @JsonProperty("detail")
    private final String detail;

    @JsonProperty("instance")
    private final String instance;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("errors")
    private final Map<String, String> errors;

    @JsonCreator
    public ProblemDetailResponse(
            @JsonProperty("type") String type,
            @JsonProperty("title") String title,
            @JsonProperty("status") int status,
            @JsonProperty("detail") String detail,
            @JsonProperty("instance") String instance,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("errors") Map<String, String> errors) {
        this.type = type != null ? type : "about:blank";
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.correlationId = correlationId;
        this.errors = errors;
    }

    public static ProblemDetailResponse of(int status, String title, String detail, String instance, String correlationId) {
        return ProblemDetailResponse.builder()
                .type("about:blank")
                .status(status)
                .title(title)
                .detail(detail)
                .instance(instance)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    public static ProblemDetailResponse validationError(String detail, String instance, String correlationId, Map<String, String> errors) {
        return ProblemDetailResponse.builder()
                .type("https://apishield.com/errors/validation-failed")
                .status(400)
                .title("Bad Request")
                .detail(detail)
                .instance(instance)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}
