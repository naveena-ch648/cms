-- V12: Create shared_links and shared_link_accesses tables

CREATE TABLE shared_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    token VARCHAR(64) NOT NULL,
    resource_type ENUM('FILE', 'FOLDER') NOT NULL,
    file_id BIGINT NULL,
    folder_id BIGINT NULL,
    created_by BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NULL,
    expires_at TIMESTAMP NULL,
    allow_download BOOLEAN NOT NULL DEFAULT TRUE,
    watermark_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    max_views INT NULL,
    view_count INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'REVOKED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    last_accessed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_sl_uuid (uuid),
    UNIQUE KEY idx_sl_token (token),
    KEY idx_sl_file (file_id),
    KEY idx_sl_folder (folder_id),
    KEY idx_sl_creator (created_by),
    KEY idx_sl_workspace_status (workspace_id, status),
    KEY idx_sl_expires (status, expires_at),

    CONSTRAINT fk_sl_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_sl_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_sl_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sl_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shared_link_accesses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shared_link_id BIGINT NOT NULL,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,

    KEY idx_sla_link (shared_link_id),
    KEY idx_sla_link_time (shared_link_id, accessed_at),

    CONSTRAINT fk_sla_link FOREIGN KEY (shared_link_id) REFERENCES shared_links(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
