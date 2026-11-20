package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Standard event metadata descriptor.
 */
@Getter
@ToString
@Builder
public class EventMetadata {

    @JsonProperty("eventId")
    private final String eventId;

    @JsonProperty("eventType")
    private final String eventType;

    @JsonProperty("producer")
    private final String producer;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonCreator
    public EventMetadata(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("producer") String producer,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("correlationId") String correlationId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.producer = producer;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.correlationId = correlationId;
    }
}
