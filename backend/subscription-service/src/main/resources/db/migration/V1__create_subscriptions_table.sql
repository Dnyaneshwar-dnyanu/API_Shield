-- ==============================================================================
-- APIShield Subscription Service — V1 Database Schema Migration
-- ==============================================================================

CREATE TABLE IF NOT EXISTS subscriptions (
    id VARCHAR(36) NOT NULL,
    developer_id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(36) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_subscriptions PRIMARY KEY (id)
);

CREATE INDEX idx_subscriptions_developer_id ON subscriptions (developer_id);
CREATE INDEX idx_subscriptions_api_id ON subscriptions (api_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_dev_api ON subscriptions (developer_id, api_id);
