-- V26: Internal file sharing between CMS users

CREATE TABLE file_shares (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid            CHAR(36)        NOT NULL UNIQUE,
    file_id         BIGINT          NOT NULL,
    shared_by_user_id BIGINT        NOT NULL,
    shared_with_user_id BIGINT      NOT NULL,
    permission      ENUM('VIEWER','EDITOR') NOT NULL DEFAULT 'VIEWER',
    allow_download  BOOLEAN         NOT NULL DEFAULT TRUE,
    watermark_enabled BOOLEAN       NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMP       NULL,
    status          ENUM('ACTIVE','REVOKED') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_fs_file        FOREIGN KEY (file_id)            REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_fs_shared_by   FOREIGN KEY (shared_by_user_id)  REFERENCES users(id),
    CONSTRAINT fk_fs_shared_with FOREIGN KEY (shared_with_user_id) REFERENCES users(id),
    -- One active share per (file, recipient) — prevent duplicate active rows
    CONSTRAINT uq_fs_file_user   UNIQUE (file_id, shared_with_user_id)
);

CREATE INDEX idx_fs_shared_with ON file_shares (shared_with_user_id, status);
CREATE INDEX idx_fs_file        ON file_shares (file_id, status);
CREATE INDEX idx_fs_shared_by   ON file_shares (shared_by_user_id, status);
