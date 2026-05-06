-- V6: Create file tables
-- files table: stores file metadata, references folder and organization
CREATE TABLE files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    folder_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    storage_bucket VARCHAR(63) NOT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    status ENUM('ACTIVE', 'TRASHED', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    trashed_at TIMESTAMP NULL,
    trashed_by BIGINT NULL,
    permanent_delete_at TIMESTAMP NULL,
    uploaded_by BIGINT NOT NULL,
    upload_completed_at TIMESTAMP NULL,
    description VARCHAR(1000) NULL,
    tags JSON DEFAULT ('[]'),
    download_count INT NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP NULL,
    thumbnail_key VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_file_uuid (uuid),
    KEY idx_file_folder (folder_id, status),
    KEY idx_file_org (organization_id),
    KEY idx_file_workspace (workspace_id),
    KEY idx_file_status (status),
    KEY idx_file_uploaded_by (uploaded_by),
    KEY idx_file_permanent_delete (permanent_delete_at),

    CONSTRAINT fk_file_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT fk_file_trashed_by FOREIGN KEY (trashed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- storage_quotas table: one per organization
CREATE TABLE storage_quotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    max_storage_bytes BIGINT NOT NULL DEFAULT 10737418240,
    used_storage_bytes BIGINT NOT NULL DEFAULT 0,
    max_file_size_bytes BIGINT NOT NULL DEFAULT 10737418240,
    allowed_extensions JSON DEFAULT NULL,
    blocked_extensions JSON DEFAULT ('[]'),
    trash_retention_days INT NOT NULL DEFAULT 30,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY idx_quota_org (organization_id),
    CONSTRAINT fk_quota_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
