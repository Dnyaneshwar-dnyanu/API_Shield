package com.apishield.registry.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.ApiPublishedEvent;
import com.apishield.common.event.EventEnvelope;
import com.apishield.registry.entity.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishApiPublished(Api api) {
        try {
            ApiPublishedEvent payload = ApiPublishedEvent.builder()
                    .apiId(api.getId().toString())
                    .ownerId(api.getOwnerId())
                    .name(api.getName())
                    .slug(api.getSlug())
                    .category(api.getCategory())
                    .targetBackendUrl(api.getTargetBackendUrl())
                    .status(api.getStatus().name())
                    .publishedAt(Instant.now())
                    .build();

            EventEnvelope<ApiPublishedEvent> envelope = EventEnvelope.of(
                    "api.published",
                    "api-registry-service",
                    null,
                    api.getId().toString(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.REGISTRY_EVENTS, api.getId().toString(), envelope);
            log.info("Published api.published event for API slug: {}", api.getSlug());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka event for API: {}. Reason: {}", api.getSlug(), e.getMessage());
        }
    }

    public void publishApiDeprecated(Api api) {
        try {
            ApiPublishedEvent payload = ApiPublishedEvent.builder()
                    .apiId(api.getId().toString())
                    .ownerId(api.getOwnerId())
                    .name(api.getName())
                    .slug(api.getSlug())
                    .category(api.getCategory())
                    .targetBackendUrl(api.getTargetBackendUrl())
                    .status(api.getStatus().name())
                    .publishedAt(Instant.now())
                    .build();

            EventEnvelope<ApiPublishedEvent> envelope = EventEnvelope.of(
                    "api.deprecated",
                    "api-registry-service",
                    null,
                    api.getId().toString(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.REGISTRY_EVENTS, api.getId().toString(), envelope);
            log.info("Published api.deprecated event for API slug: {}", api.getSlug());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka deprecation event for API: {}. Reason: {}", api.getSlug(), e.getMessage());
        }
    }
}
