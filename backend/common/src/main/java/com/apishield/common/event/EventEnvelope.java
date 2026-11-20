package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Standardized wrapper envelope for all Apache Kafka domain events in APIShield.
 * Guarantees uniform metadata, distributed tracing, and consistent deserialization.
 *
 * @param <T> Domain event payload type
 */
@Getter
@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope<T> {

    @JsonProperty("eventId")
    private final String eventId;

    @JsonProperty("eventType")
    private final String eventType;

    @JsonProperty("timestamp")
    private final long timestamp;

    @JsonProperty("producer")
    private final String producer;

    @JsonProperty("correlationId")
    private final String correlationId;

    @JsonProperty("partitionKey")
    private final String partitionKey;

    @JsonProperty("payload")
    private final T payload;

    @JsonCreator
    public EventEnvelope(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("producer") String producer,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("partitionKey") String partitionKey,
            @JsonProperty("payload") T payload) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = timestamp > 0 ? timestamp : Instant.now().toEpochMilli();
        this.producer = producer;
        this.correlationId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        this.partitionKey = partitionKey;
        this.payload = payload;
    }

    /**
     * Convenience factory method for constructing an event envelope.
     */
    public static <T> EventEnvelope<T> of(String eventType, String producer, String correlationId, String partitionKey, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .timestamp(Instant.now().toEpochMilli())
                .producer(producer)
                .correlationId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .partitionKey(partitionKey)
                .payload(payload)
                .build();
    }
}
