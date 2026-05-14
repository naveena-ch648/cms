-- V25: Two-Factor Authentication

CREATE TABLE user_two_factor (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    method       ENUM('TOTP','EMAIL') NOT NULL DEFAULT 'EMAIL',
    totp_secret  VARCHAR(64),
    is_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    backup_codes TEXT,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_two_factor UNIQUE (user_id),
    CONSTRAINT fk_two_factor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
