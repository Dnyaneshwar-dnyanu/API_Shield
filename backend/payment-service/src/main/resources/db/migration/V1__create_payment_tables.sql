-- ==============================================================================
-- APIShield Payment Service - Initial Flyway Migration
-- Schema: apishield_payment
-- Engine: MySQL 8.0+ / InnoDB
-- ==============================================================================

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    subscription_id VARCHAR(36) NOT NULL,
    developer_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(36) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'RAZORPAY',
    order_id VARCHAR(64) NOT NULL UNIQUE,
    payment_id VARCHAR(64) UNIQUE,
    signature VARCHAR(256),
    amount_cents INT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payments_subscription_id (subscription_id),
    INDEX idx_payments_developer_id (developer_id),
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id VARCHAR(36) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL,
    developer_id VARCHAR(36) NOT NULL,
    invoice_number VARCHAR(64) NOT NULL UNIQUE,
    amount_cents INT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL,
    billing_period_start TIMESTAMP NULL,
    billing_period_end TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_invoices_payment_id (payment_id),
    INDEX idx_invoices_developer_id (developer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS webhook_logs (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(64) NOT NULL,
    payload LONGTEXT NOT NULL,
    signature VARCHAR(256),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_webhook_logs_event_id (event_id),
    INDEX idx_webhook_logs_processed (processed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
