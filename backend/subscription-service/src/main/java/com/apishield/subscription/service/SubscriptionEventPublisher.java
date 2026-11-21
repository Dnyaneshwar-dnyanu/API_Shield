package com.apishield.subscription.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.EventEnvelope;
import com.apishield.common.event.SubscriptionActivatedEvent;
import com.apishield.subscription.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSubscriptionActivated(Subscription sub, int rateLimitRps, int quotaMonthly) {
        try {
            SubscriptionActivatedEvent payload = SubscriptionActivatedEvent.builder()
                    .subscriptionId(sub.getId().toString())
                    .developerId(sub.getDeveloperId())
                    .apiId(sub.getApiId())
                    .planId(sub.getPlanId())
                    .status(sub.getStatus().name())
                    .rateLimitRps(rateLimitRps)
                    .quotaMonthly(quotaMonthly)
                    .activatedAt(Instant.now())
                    .build();

            EventEnvelope<SubscriptionActivatedEvent> envelope = EventEnvelope.of(
                    "subscription.activated",
                    "subscription-service",
                    null,
                    sub.getId().toString(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.SUBSCRIPTION_EVENTS, sub.getId().toString(), envelope);
            log.info("Published subscription.activated event for subscription: {}", sub.getId());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka subscription.activated event for {}: {}", sub.getId(), e.getMessage());
        }
    }

    public void publishSubscriptionCancelled(Subscription sub) {
        try {
            Map<String, Object> payload = Map.of(
                    "subscriptionId", sub.getId().toString(),
                    "developerId", sub.getDeveloperId(),
                    "apiId", sub.getApiId(),
                    "status", sub.getStatus().name(),
                    "cancelledAt", sub.getCancelledAt() != null ? sub.getCancelledAt().toString() : Instant.now().toString()
            );

            EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                    "subscription.cancelled",
                    "subscription-service",
                    null,
                    sub.getId().toString(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.SUBSCRIPTION_EVENTS, sub.getId().toString(), envelope);
            log.info("Published subscription.cancelled event for subscription: {}", sub.getId());
        } catch (Exception e) {
            log.warn("Failed to publish Kafka subscription.cancelled event for {}: {}", sub.getId(), e.getMessage());
        }
    }
}
