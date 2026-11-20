-- ==============================================================================
-- APIShield — Multi-Schema Database Initialization Script
-- Database-per-Service Architecture (Logical Schema Separation)
-- ==============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. Create Logical Schemas for each Microservice
-- ------------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `apishield_user` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_registry` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_subscription` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_apikey` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_payment` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_notification` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `apishield_analytics` 
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ------------------------------------------------------------------------------
-- 2. Create Service-Specific Dedicated Users (Least Privilege)
-- ------------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'user_svc'@'%' IDENTIFIED BY 'user_svc_password';
GRANT ALL PRIVILEGES ON `apishield_user`.* TO 'user_svc'@'%';

CREATE USER IF NOT EXISTS 'registry_svc'@'%' IDENTIFIED BY 'registry_svc_password';
GRANT ALL PRIVILEGES ON `apishield_registry`.* TO 'registry_svc'@'%';

CREATE USER IF NOT EXISTS 'subscription_svc'@'%' IDENTIFIED BY 'sub_svc_password';
GRANT ALL PRIVILEGES ON `apishield_subscription`.* TO 'subscription_svc'@'%';

CREATE USER IF NOT EXISTS 'apikey_svc'@'%' IDENTIFIED BY 'apikey_svc_password';
GRANT ALL PRIVILEGES ON `apishield_apikey`.* TO 'apikey_svc'@'%';

CREATE USER IF NOT EXISTS 'payment_svc'@'%' IDENTIFIED BY 'payment_svc_password';
GRANT ALL PRIVILEGES ON `apishield_payment`.* TO 'payment_svc'@'%';

CREATE USER IF NOT EXISTS 'notification_svc'@'%' IDENTIFIED BY 'notif_svc_password';
GRANT ALL PRIVILEGES ON `apishield_notification`.* TO 'notification_svc'@'%';

CREATE USER IF NOT EXISTS 'analytics_svc'@'%' IDENTIFIED BY 'analytics_svc_password';
GRANT ALL PRIVILEGES ON `apishield_analytics`.* TO 'analytics_svc'@'%';

-- ------------------------------------------------------------------------------
-- 3. Create General Platform Dev User (Access across all schemas for local testing)
-- ------------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'apishield'@'%' IDENTIFIED BY 'apishield_dev_secret';
GRANT ALL PRIVILEGES ON `apishield_user`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_registry`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_subscription`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_apikey`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_payment`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_notification`.* TO 'apishield'@'%';
GRANT ALL PRIVILEGES ON `apishield_analytics`.* TO 'apishield'@'%';

FLUSH PRIVILEGES;
SET FOREIGN_KEY_CHECKS = 1;
