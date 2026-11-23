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

class ApiKeyNotificationConsumerTest {

    private NotificationService notificationService;
    private ObjectMapper objectMapper;
    private ApiKeyNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        objectMapper = new ObjectMapper();
        consumer = new ApiKeyNotificationConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch security notice on apikey.revoked event")
    void shouldDispatchKeyRevokedSecurityAlert() throws Exception {
        Map<String, Object> payload = Map.of(
                "developerId", "dev-400",
                "apiKeyId", "key-888",
                "reason", "Compromised key rotation"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "apikey.revoked",
                "api-key-service",
                null,
                "key-888",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleApiKeyEvents(json);

        verify(notificationService).createAndSend(
                eq("dev-400"),
                isNull(),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.KEY_REVOKED),
                contains("SECURITY ALERT"),
                anyMap()
        );
    }
}
