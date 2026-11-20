-- V4: Create email verification tokens table and add suspension audit columns to users
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_tokens_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verification_tokens_expires_at ON email_verification_tokens(expires_at);

ALTER TABLE users ADD COLUMN suspension_reason VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN suspended_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN suspended_by VARCHAR(36) NULL;
