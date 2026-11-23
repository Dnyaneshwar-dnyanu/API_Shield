package com.apishield.notification.service;

import com.apishield.notification.entity.NotificationTemplateKey;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationTemplateEngine {

    public String renderEmailBody(NotificationTemplateKey templateKey, Map<String, Object> params) {
        return switch (templateKey) {
            case WELCOME_EMAIL -> renderWelcomeEmail(params);
            case PAYMENT_SUCCESS -> renderPaymentSuccessEmail(params);
            case PAYMENT_FAILED -> renderPaymentFailedEmail(params);
            case SUBSCRIPTION_ACTIVATED -> renderSubscriptionActivatedEmail(params);
            case SUBSCRIPTION_CANCELLED -> renderSubscriptionCancelledEmail(params);
            case KEY_GENERATED -> renderKeyGeneratedEmail(params);
            case KEY_REVOKED -> renderKeyRevokedEmail(params);
            case DIRECT_ALERT -> renderDirectAlertEmail(params);
        };
    }

    private String renderWelcomeEmail(Map<String, Object> params) {
        String name = String.valueOf(params.getOrDefault("name", "Developer"));
        return String.format(
                "Hello %s,\n\n" +
                "Welcome to APIShield — The Enterprise API Management & Security Platform!\n\n" +
                "Your account is now active. You can browse published APIs, subscribe to tiers, " +
                "and generate cryptographically secured API keys.\n\n" +
                "Get started at: https://apishield.io/marketplace\n\n" +
                "— The APIShield Team",
                name
        );
    }

    private String renderPaymentSuccessEmail(Map<String, Object> params) {
        return String.format(
                "Payment Confirmation\n\n" +
                "Your payment of %s %s for subscription %s has been successfully processed.\n" +
                "Payment ID: %s\n" +
                "Reference: %s\n\n" +
                "Your subscription is now ACTIVE.\n\n" +
                "— APIShield Billing",
                params.getOrDefault("currency", "USD"),
                params.getOrDefault("amount", "0.00"),
                params.getOrDefault("subscriptionId", ""),
                params.getOrDefault("paymentId", ""),
                params.getOrDefault("transactionReference", "")
        );
    }

    private String renderPaymentFailedEmail(Map<String, Object> params) {
        return String.format(
                "Payment Failed Alert\n\n" +
                "We were unable to process your payment for subscription %s.\n" +
                "Reason: %s\n\n" +
                "Please update your payment method to prevent subscription suspension.\n\n" +
                "— APIShield Billing",
                params.getOrDefault("subscriptionId", ""),
                params.getOrDefault("reason", "Declined")
        );
    }

    private String renderSubscriptionActivatedEmail(Map<String, Object> params) {
        return String.format(
                "Subscription Activated\n\n" +
                "Your subscription to API (%s) under Plan (%s) is now ACTIVE.\n" +
                "Allocated Quota: %s requests/month\n" +
                "Rate Limit: %s RPS\n\n" +
                "You can now generate API keys to begin integrating.\n\n" +
                "— APIShield Subscriptions",
                params.getOrDefault("apiId", ""),
                params.getOrDefault("planId", ""),
                params.getOrDefault("quotaMonthly", "Unlimited"),
                params.getOrDefault("rateLimitRps", "Default")
        );
    }

    private String renderSubscriptionCancelledEmail(Map<String, Object> params) {
        return String.format(
                "Subscription Cancelled\n\n" +
                "Your subscription %s has been cancelled.\n" +
                "All associated API keys have been automatically revoked for security.\n\n" +
                "— APIShield Subscriptions",
                params.getOrDefault("subscriptionId", "")
        );
    }

    private String renderKeyGeneratedEmail(Map<String, Object> params) {
        return String.format(
                "New API Key Generated\n\n" +
                "A new API key with prefix '%s' was generated for your subscription %s.\n" +
                "If you did not authorize this action, please revoke the key immediately from your dashboard.\n\n" +
                "— APIShield Security",
                params.getOrDefault("prefix", ""),
                params.getOrDefault("subscriptionId", "")
        );
    }

    private String renderKeyRevokedEmail(Map<String, Object> params) {
        return String.format(
                "SECURITY ALERT: API Key Revoked\n\n" +
                "API key '%s' has been revoked.\n" +
                "Reason: %s\n" +
                "This key will immediately fail authentication at the Edge Gateway.\n\n" +
                "— APIShield Security",
                params.getOrDefault("apiKeyId", ""),
                params.getOrDefault("reason", "Manual revocation")
        );
    }

    private String renderDirectAlertEmail(Map<String, Object> params) {
        return String.format(
                "APIShield Notification\n\n" +
                "%s\n\n" +
                "— APIShield Notification Service",
                params.getOrDefault("message", "")
        );
    }
}
