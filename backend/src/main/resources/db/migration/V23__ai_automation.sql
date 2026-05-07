-- AI Automation: ai_jobs table and organization ai_config column
-- Feature: 016-ai-automation

CREATE TABLE ai_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    type ENUM('TAG', 'SUMMARIZE', 'CLASSIFY', 'DETECT_DUPLICATES', 'DETECT_SENSITIVE', 'RECOMMEND_WORKFLOW') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    result JSON NULL,
    confidence DECIMAL(5,2) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    triggered_by ENUM('SYSTEM', 'USER') NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,

    INDEX idx_ai_jobs_file_id (file_id),
    INDEX idx_ai_jobs_org_status (organization_id, status),
    INDEX idx_ai_jobs_file_type (file_id, type),

    CONSTRAINT fk_ai_jobs_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_jobs_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

ALTER TABLE organizations ADD COLUMN ai_config JSON NULL;
