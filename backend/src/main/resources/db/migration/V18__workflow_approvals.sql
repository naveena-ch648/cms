-- Workflow & Approvals Engine tables

-- Add workflow_state column to files table
ALTER TABLE files ADD COLUMN workflow_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE files ADD INDEX idx_files_workspace_workflow (workspace_id, workflow_state);

-- Workflow transitions audit log
CREATE TABLE workflow_transitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    from_state VARCHAR(20) NOT NULL,
    to_state VARCHAR(20) NOT NULL,
    actor_id BIGINT NOT NULL,
    comment TEXT NULL,
    approval_request_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wt_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_wt_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    INDEX idx_wt_file_time (file_id, created_at),
    INDEX idx_wt_actor (actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Approval requests
CREATE TABLE approval_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    submitter_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    from_state VARCHAR(20) NOT NULL,
    to_state VARCHAR(20) NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_ar_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_ar_submitter FOREIGN KEY (submitter_id) REFERENCES users(id),
    CONSTRAINT fk_ar_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    INDEX idx_ar_file_status (file_id, status),
    INDEX idx_ar_workspace_status (workspace_id, status),
    INDEX idx_ar_submitter_status (submitter_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add FK from workflow_transitions to approval_requests (now that table exists)
ALTER TABLE workflow_transitions ADD CONSTRAINT fk_wt_approval FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id);

-- Approval decisions (per-reviewer)
CREATE TABLE approval_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    approval_request_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    decision ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    comment TEXT NULL,
    decided_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ad_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id),
    CONSTRAINT fk_ad_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    UNIQUE INDEX idx_ad_request_reviewer (approval_request_id, reviewer_id),
    INDEX idx_ad_reviewer_decision (reviewer_id, decision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Workflow triggers
CREATE TABLE workflow_triggers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    trigger_state VARCHAR(20) NOT NULL,
    trigger_type ENUM('NOTIFICATION','PREREQUISITE') NOT NULL,
    config JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wtr_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_wtr_creator FOREIGN KEY (created_by) REFERENCES users(id),
    INDEX idx_wtr_workspace_state_enabled (workspace_id, trigger_state, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
