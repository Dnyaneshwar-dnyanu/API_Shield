package com.apishield.apikey.service;

import com.apishield.apikey.config.TestApiKeyConfig;
import com.apishield.apikey.entity.ApiKey;
import com.apishield.apikey.model.KeyStatus;
import com.apishield.apikey.repository.ApiKeyRepository;
import com.apishield.common.event.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestApiKeyConfig.class)
class SubscriptionEventConsumerTest {

    @Autowired
    private SubscriptionEventConsumer subscriptionEventConsumer;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private final String subscriptionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should automatically revoke active keys when subscription.cancelled event is consumed")
    void shouldRevokeKeysOnSubscriptionCancelled() throws Exception {
        ApiKey key1 = ApiKey.builder()
                .name("Key 1")
                .developerId(UUID.randomUUID().toString())
                .subscriptionId(subscriptionId)
                .apiId(UUID.randomUUID().toString())
                .keyPrefix("ash_live_prefix1")
                .keyHash("hash1_1234567890abcdef1234567890abcdef")
                .status(KeyStatus.ACTIVE)
                .build();

        ApiKey key2 = ApiKey.builder()
                .name("Key 2")
                .developerId(UUID.randomUUID().toString())
                .subscriptionId(subscriptionId)
                .apiId(UUID.randomUUID().toString())
                .keyPrefix("ash_live_prefix2")
                .keyHash("hash2_1234567890abcdef1234567890abcdef")
                .status(KeyStatus.ACTIVE)
                .build();

        apiKeyRepository.save(key1);
        apiKeyRepository.save(key2);

        Map<String, Object> payload = Map.of(
                "subscriptionId", subscriptionId,
                "reason", "Developer requested termination"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "subscription.cancelled",
                "subscription-service",
                null,
                subscriptionId,
                payload
        );

        String message = objectMapper.writeValueAsString(envelope);

        // Consume event
        subscriptionEventConsumer.handleSubscriptionEvents(message);

        // Verify all keys for this subscription transitioned to REVOKED
        ApiKey updated1 = apiKeyRepository.findById(key1.getId()).orElseThrow();
        ApiKey updated2 = apiKeyRepository.findById(key2.getId()).orElseThrow();

        assertThat(updated1.getStatus()).isEqualTo(KeyStatus.REVOKED);
        assertThat(updated2.getStatus()).isEqualTo(KeyStatus.REVOKED);
    }
}
