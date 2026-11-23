package com.apishield.notification.consumer;

import com.apishield.common.event.EventEnvelope;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

class SubscriptionEventConsumerTest {

    private NotificationService notificationService;
    private ObjectMapper objectMapper;
    private SubscriptionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        objectMapper = new ObjectMapper();
        consumer = new SubscriptionEventConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch subscription activated notice on subscription.activated event")
    void shouldDispatchActivatedNotice() throws Exception {
        Map<String, Object> payload = Map.of(
                "developerId", "dev-300",
                "subscriptionId", "sub-500",
                "apiId", "api-weather",
                "planId", "plan-starter",
                "rateLimitRps", 10,
                "quotaMonthly", 1000
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "subscription.activated",
                "subscription-service",
                null,
                "sub-500",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleSubscriptionEvents(json);

        verify(notificationService).createAndSend(
                eq("dev-300"),
                isNull(),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.SUBSCRIPTION_ACTIVATED),
                contains("Subscription Activated"),
                anyMap()
        );
    }

    @Test
    @DisplayName("Should dispatch subscription cancelled notice on subscription.cancelled event")
    void shouldDispatchCancelledNotice() throws Exception {
        Map<String, Object> payload = Map.of(
                "developerId", "dev-300",
                "subscriptionId", "sub-500"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "subscription.cancelled",
                "subscription-service",
                null,
                "sub-500",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleSubscriptionEvents(json);

        verify(notificationService).createAndSend(
                eq("dev-300"),
                isNull(),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.SUBSCRIPTION_CANCELLED),
                contains("Subscription Cancelled"),
                anyMap()
        );
    }
}
