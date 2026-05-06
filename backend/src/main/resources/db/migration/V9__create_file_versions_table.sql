-- File Versions table for version history
CREATE TABLE file_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    file_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    storage_bucket VARCHAR(63) NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    change_note VARCHAR(500) NULL,
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_version_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_version_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id),

    UNIQUE KEY idx_version_uuid (uuid),
    UNIQUE KEY idx_file_version (file_id, version_number),
    KEY idx_version_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add version tracking columns to files table
ALTER TABLE files ADD COLUMN current_version_id BIGINT NULL;
ALTER TABLE files ADD COLUMN version_count INT NOT NULL DEFAULT 1;

ALTER TABLE files ADD CONSTRAINT fk_file_current_version FOREIGN KEY (current_version_id) REFERENCES file_versions(id) ON DELETE SET NULL;
