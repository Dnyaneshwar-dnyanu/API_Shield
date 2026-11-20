package com.apishield.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EventEnvelopeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should correctly serialize and deserialize EventEnvelope with UserRegisteredEvent")
    void shouldSerializeAndDeserializeEnvelope() throws Exception {
        UserRegisteredEvent payload = UserRegisteredEvent.builder()
                .userId("usr-123")
                .email("dev@apishield.com")
                .firstName("John")
                .lastName("Doe")
                .role("ROLE_DEVELOPER")
                .registeredAt(Instant.now())
                .build();

        EventEnvelope<UserRegisteredEvent> envelope = EventEnvelope.of(
                "user.registered",
                "user-service",
                "corr-456",
                "usr-123",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("user.registered").contains("dev@apishield.com");

        EventEnvelope<?> deserialized = objectMapper.readValue(json, EventEnvelope.class);
        assertThat(deserialized.getEventType()).isEqualTo("user.registered");
        assertThat(deserialized.getProducer()).isEqualTo("user-service");
        assertThat(deserialized.getCorrelationId()).isEqualTo("corr-456");
        assertThat(deserialized.getPartitionKey()).isEqualTo("usr-123");
        assertThat(deserialized.getPayload()).isNotNull();
    }
}
