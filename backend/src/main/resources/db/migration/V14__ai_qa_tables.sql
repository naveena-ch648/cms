CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    message_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_conversation_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_conversation_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    INDEX idx_conversation_user_workspace (user_id, workspace_id, status),
    INDEX idx_conversation_org (organization_id),
    INDEX idx_conversation_updated (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    conversation_id BIGINT NOT NULL,
    role ENUM('USER','ASSISTANT') NOT NULL,
    content TEXT NOT NULL,
    citations JSON NULL,
    token_count INT NULL,
    model_used VARCHAR(100) NULL,
    retrieval_chunks JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_message_conversation (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE embedding_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    status ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
    chunk_count INT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    vector_dimension INT NOT NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_embedding_job_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_embedding_job_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_embedding_job_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    INDEX idx_embedding_job_file (file_id),
    INDEX idx_embedding_job_status (organization_id, status),
    INDEX idx_embedding_job_workspace (workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
