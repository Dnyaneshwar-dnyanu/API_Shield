package com.apishield.notification.consumer;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.EventEnvelope;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.SUBSCRIPTION_EVENTS, groupId = "notification-service-group")
    public void handleSubscriptionEvents(String message) {
        try {
            log.info("Received subscription event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("subscription.activated".equalsIgnoreCase(eventType) || "SUBSCRIPTION_ACTIVATED".equalsIgnoreCase(eventType)) {
                handleSubscriptionActivated(envelope);
            } else if ("subscription.cancelled".equalsIgnoreCase(eventType) || "SUBSCRIPTION_CANCELLED".equalsIgnoreCase(eventType)) {
                handleSubscriptionCancelled(envelope);
            } else {
                log.debug("Ignoring non-actionable subscription event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing subscription event in notification-service: {}", e.getMessage());
        }
    }

    private void handleSubscriptionActivated(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String developerId = (String) map.get("developerId");
            String subscriptionId = (String) map.get("subscriptionId");
            String apiId = (String) map.get("apiId");
            String planId = (String) map.get("planId");
            Number rateLimitRps = (Number) map.get("rateLimitRps");
            Number quotaMonthly = (Number) map.get("quotaMonthly");

            if (developerId != null) {
                log.info("Dispatching subscription activated notice for developer: {}, subscription: {}", developerId, subscriptionId);
                notificationService.createAndSend(
                        developerId,
                        null,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.SUBSCRIPTION_ACTIVATED,
                        "APIShield — Subscription Activated",
                        Map.of(
                                "subscriptionId", subscriptionId != null ? subscriptionId : "",
                                "apiId", apiId != null ? apiId : "",
                                "planId", planId != null ? planId : "",
                                "rateLimitRps", rateLimitRps != null ? rateLimitRps.toString() : "Default",
                                "quotaMonthly", quotaMonthly != null ? quotaMonthly.toString() : "Unlimited"
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to process subscription.activated notification: {}", e.getMessage());
        }
    }

    private void handleSubscriptionCancelled(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String developerId = (String) map.get("developerId");
            String subscriptionId = (String) map.get("subscriptionId");

            if (developerId != null) {
                log.info("Dispatching subscription cancelled notice for developer: {}, subscription: {}", developerId, subscriptionId);
                notificationService.createAndSend(
                        developerId,
                        null,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.SUBSCRIPTION_CANCELLED,
                        "APIShield — Subscription Cancelled",
                        Map.of("subscriptionId", subscriptionId != null ? subscriptionId : "")
                );
            }
        } catch (Exception e) {
            log.error("Failed to process subscription.cancelled notification: {}", e.getMessage());
        }
    }
}
