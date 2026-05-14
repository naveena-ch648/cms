-- V24: Email Digest Preferences

CREATE TABLE user_email_preferences (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    digest_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    digest_frequency        ENUM('DAILY','WEEKLY') NOT NULL DEFAULT 'WEEKLY',
    include_shared_files    BOOLEAN NOT NULL DEFAULT TRUE,
    include_pending_approvals BOOLEAN NOT NULL DEFAULT TRUE,
    include_storage_usage   BOOLEAN NOT NULL DEFAULT TRUE,
    include_recent_activity BOOLEAN NOT NULL DEFAULT TRUE,
    last_digest_sent_at     DATETIME(6),
    updated_at              DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_email_pref UNIQUE (user_id),
    CONSTRAINT fk_email_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed default preferences for all existing active users
INSERT INTO user_email_preferences (user_id, digest_enabled, digest_frequency)
SELECT id, TRUE, 'WEEKLY' FROM users WHERE status = 'ACTIVE';
