CREATE TABLE IF NOT EXISTS `notifications` (
    `id` VARCHAR(36) NOT NULL,
    `recipient_user_id` VARCHAR(36) NOT NULL,
    `recipient_email` VARCHAR(255) NULL,
    `channel` VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    `template_key` VARCHAR(100) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content_payload` TEXT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `is_read` BOOLEAN NOT NULL DEFAULT FALSE,
    `sent_at` TIMESTAMP NULL,
    `retry_count` INT NOT NULL DEFAULT 0,
    `error_message` VARCHAR(1000) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_notif_recipient` (`recipient_user_id`),
    INDEX `idx_notif_status` (`status`),
    INDEX `idx_notif_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
