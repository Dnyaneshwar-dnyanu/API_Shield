package com.apishield.notification.service;

import com.apishield.notification.entity.NotificationTemplateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTemplateEngineTest {

    private NotificationTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new NotificationTemplateEngine();
    }

    @Test
    @DisplayName("Should render welcome email with developer name")
    void shouldRenderWelcomeEmail() {
        String body = engine.renderEmailBody(
                NotificationTemplateKey.WELCOME_EMAIL,
                Map.of("name", "Alice")
        );
        assertThat(body).contains("Hello Alice").contains("Welcome to APIShield");
    }

    @Test
    @DisplayName("Should render payment success email with amount and currency")
    void shouldRenderPaymentSuccessEmail() {
        String body = engine.renderEmailBody(
                NotificationTemplateKey.PAYMENT_SUCCESS,
                Map.of(
                        "amount", "49.00",
                        "currency", "USD",
                        "subscriptionId", "sub-123",
                        "paymentId", "pay-456",
                        "transactionReference", "tx-789"
                )
        );
        assertThat(body).contains("USD 49.00").contains("pay-456").contains("ACTIVE");
    }

    @Test
    @DisplayName("Should render payment failed email with reason")
    void shouldRenderPaymentFailedEmail() {
        String body = engine.renderEmailBody(
                NotificationTemplateKey.PAYMENT_FAILED,
                Map.of("subscriptionId", "sub-123", "reason", "Insufficient funds")
        );
        assertThat(body).contains("sub-123").contains("Insufficient funds");
    }

    @Test
    @DisplayName("Should render subscription activated email with quota and rate limit")
    void shouldRenderSubscriptionActivatedEmail() {
        String body = engine.renderEmailBody(
                NotificationTemplateKey.SUBSCRIPTION_ACTIVATED,
                Map.of(
                        "apiId", "weather-api",
                        "planId", "pro-plan",
                        "quotaMonthly", "50000",
                        "rateLimitRps", "100"
                )
        );
        assertThat(body).contains("weather-api").contains("50000").contains("100 RPS");
    }

    @Test
    @DisplayName("Should render key revoked security alert")
    void shouldRenderKeyRevokedEmail() {
        String body = engine.renderEmailBody(
                NotificationTemplateKey.KEY_REVOKED,
                Map.of("apiKeyId", "key-999", "reason", "Security rotation")
        );
        assertThat(body).contains("SECURITY ALERT").contains("key-999").contains("Security rotation");
    }
}
