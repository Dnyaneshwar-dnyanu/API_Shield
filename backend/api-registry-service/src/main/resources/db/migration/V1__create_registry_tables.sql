-- ==============================================================================
-- APIShield API Registry Service — V1 Database Schema Migration
-- ==============================================================================

CREATE TABLE IF NOT EXISTS apis (
    id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    target_backend_url VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    version_label VARCHAR(20) NOT NULL DEFAULT 'v1',
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_apis PRIMARY KEY (id),
    CONSTRAINT uq_apis_slug UNIQUE (slug)
);

CREATE INDEX idx_apis_owner_id ON apis (owner_id);
CREATE INDEX idx_apis_status ON apis (status);
CREATE INDEX idx_apis_category ON apis (category);
CREATE INDEX idx_apis_slug ON apis (slug);

CREATE TABLE IF NOT EXISTS api_endpoints (
    id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    path_pattern VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_api_endpoints PRIMARY KEY (id),
    CONSTRAINT fk_api_endpoints_api_id FOREIGN KEY (api_id) REFERENCES apis (id) ON DELETE CASCADE
);

CREATE INDEX idx_endpoints_api_id ON api_endpoints (api_id);

CREATE TABLE IF NOT EXISTS api_plans (
    id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    price_cents INT NOT NULL DEFAULT 0,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    rate_limit_rps INT NOT NULL DEFAULT 10,
    quota_monthly INT NOT NULL DEFAULT 10000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_api_plans PRIMARY KEY (id),
    CONSTRAINT fk_api_plans_api_id FOREIGN KEY (api_id) REFERENCES apis (id) ON DELETE CASCADE
);

CREATE INDEX idx_plans_api_id ON api_plans (api_id);
