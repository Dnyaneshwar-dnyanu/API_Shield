package com.apishield.common.constant;

/**
 * Canonical Apache Kafka topic names for all APIShield domain events and telemetry streams.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    /**
     * User lifecycle events (user.registered, user.updated, user.suspended).
     */
    public static final String USER_EVENTS = "apishield.user.events";

    /**
     * API catalog and plan events (api.published, api.updated, api.deprecated).
     */
    public static final String REGISTRY_EVENTS = "apishield.registry.events";

    /**
     * Developer subscription lifecycle events (subscription.activated, subscription.cancelled, subscription.expired).
     */
    public static final String SUBSCRIPTION_EVENTS = "apishield.subscription.events";

    /**
     * Billing and payment events (payment.completed, payment.failed).
     */
    public static final String PAYMENT_EVENTS = "apishield.payment.events";

    /**
     * API key lifecycle events (apikey.generated, apikey.revoked).
     */
    public static final String APIKEY_EVENTS = "apishield.apikey.events";

    /**
     * High-volume Gateway runtime telemetry log stream.
     */
    public static final String GATEWAY_ANALYTICS = "apishield.gateway.analytics";

    /**
     * Direct transactional notification dispatch events.
     */
    public static final String NOTIFICATION_EVENTS = "apishield.notification.events";
}
