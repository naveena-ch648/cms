-- Preview table
CREATE TABLE previews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    version_id BIGINT NULL,
    type ENUM('THUMBNAIL', 'FULL_PREVIEW') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    storage_bucket VARCHAR(255),
    storage_key_prefix VARCHAR(500),
    thumbnail_key VARCHAR(500),
    page_count INT DEFAULT 0,
    mime_type VARCHAR(100),
    width INT,
    height INT,
    file_size_bytes BIGINT DEFAULT 0,
    error_message VARCHAR(1000),
    generated_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_preview_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_preview_version FOREIGN KEY (version_id) REFERENCES file_versions(id) ON DELETE SET NULL,
    INDEX idx_preview_file_type (file_id, type, status),
    INDEX idx_preview_version (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Preview jobs table
CREATE TABLE preview_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    version_id BIGINT NULL,
    job_type ENUM('THUMBNAIL', 'FULL_PREVIEW') NOT NULL,
    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'QUEUED',
    priority INT DEFAULT 0,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    error_message VARCHAR(1000),
    queued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    completed_at DATETIME,
    CONSTRAINT fk_job_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_version FOREIGN KEY (version_id) REFERENCES file_versions(id) ON DELETE SET NULL,
    INDEX idx_job_status (status, priority DESC, queued_at ASC),
    INDEX idx_job_file (file_id, job_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments table
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_comment_file (file_id, created_at),
    INDEX idx_comment_parent (parent_id),
    INDEX idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
