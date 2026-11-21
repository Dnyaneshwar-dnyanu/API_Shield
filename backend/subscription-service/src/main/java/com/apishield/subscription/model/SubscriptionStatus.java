package com.apishield.subscription.model;

/**
 * Lifecycle states of an API subscription.
 */
public enum SubscriptionStatus {
    PENDING,
    ACTIVE,
    CANCELLED,
    EXPIRED,
    SUSPENDED
}
