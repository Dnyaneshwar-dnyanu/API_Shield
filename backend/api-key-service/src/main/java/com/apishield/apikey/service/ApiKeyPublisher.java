package com.apishield.apikey.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.ApiKeyRevokedEvent;
import com.apishield.common.event.EventEnvelope;
import com.apishield.apikey.entity.ApiKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishKeyRevoked(ApiKey apiKey, String reason) {
        try {
            ApiKeyRevokedEvent payload = ApiKeyRevokedEvent.builder()
                    .apiKeyId(apiKey.getId().toString())
                    .subscriptionId(apiKey.getSubscriptionId())
                    .developerId(apiKey.getDeveloperId())
                    .apiId(apiKey.getApiId())
                    .prefix(apiKey.getKeyPrefix())
                    .reason(reason != null ? reason : "Key revoked")
                    .revokedAt(Instant.now())
                    .build();

            EventEnvelope<ApiKeyRevokedEvent> envelope = EventEnvelope.of(
                    "apikey.revoked",
                    "api-key-service",
                    null,
                    apiKey.getId().toString(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.APIKEY_EVENTS, apiKey.getId().toString(), envelope);
            log.info("Published apikey.revoked event for key: {}", apiKey.getId());
        } catch (Exception e) {
            log.warn("Failed to publish apikey.revoked event for {}: {}", apiKey.getId(), e.getMessage());
        }
    }
}
