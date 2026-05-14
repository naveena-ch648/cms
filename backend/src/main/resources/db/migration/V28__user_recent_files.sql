-- V25: Per-user recent file tracking
-- Each user gets their own recent file history (max 10 entries enforced in service layer).
-- UPSERT via unique key on (user_id, file_id): on re-access, last_accessed_at is updated in place.

CREATE TABLE user_recent_files (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    file_id         BIGINT NOT NULL,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY idx_urf_user_file (user_id, file_id),
    KEY idx_urf_user_time (user_id, last_accessed_at DESC),

    CONSTRAINT fk_urf_user FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_urf_file FOREIGN KEY (file_id) REFERENCES files(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
