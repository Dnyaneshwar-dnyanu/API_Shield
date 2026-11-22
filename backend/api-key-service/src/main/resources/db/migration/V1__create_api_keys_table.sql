-- ==============================================================================
-- APIShield API Key Service - Initial Flyway Migration
-- Schema: apishield_apikey
-- Engine: MySQL 8.0+ / InnoDB
-- ==============================================================================

CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    developer_id VARCHAR(36) NOT NULL,
    subscription_id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    rate_limit_rps INT NOT NULL DEFAULT 10,
    quota_monthly INT NOT NULL DEFAULT 10000,
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_keys_developer_id (developer_id),
    INDEX idx_api_keys_subscription_id (subscription_id),
    INDEX idx_api_keys_api_id (api_id),
    INDEX idx_api_keys_key_hash (key_hash),
    INDEX idx_api_keys_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
