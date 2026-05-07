-- V20: Audit & Compliance system
-- Adds new columns to audit_events, creates compliance_reports, audit_alert_rules,
-- audit_alert_instances, and audit_alert_events tables.

-- Extend audit_events with new columns
ALTER TABLE audit_events
    ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'SYSTEM' AFTER event_type,
    ADD COLUMN outcome VARCHAR(10) NOT NULL DEFAULT 'SUCCESS' AFTER resource_id,
    ADD COLUMN resource_name VARCHAR(255) NULL AFTER resource_id,
    ADD COLUMN actor_name VARCHAR(100) NULL AFTER ip_address,
    ADD COLUMN user_agent VARCHAR(500) NULL AFTER ip_address,
    ADD COLUMN workspace_id BIGINT NULL AFTER details;

ALTER TABLE audit_events
    ADD INDEX idx_audit_org_created (organization_id, created_at DESC),
    ADD INDEX idx_audit_org_user (organization_id, user_id),
    ADD INDEX idx_audit_org_category (organization_id, category),
    ADD INDEX idx_audit_org_event_type (organization_id, event_type),
    ADD INDEX idx_audit_org_resource (organization_id, resource_type, resource_id),
    ADD CONSTRAINT fk_audit_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE SET NULL;

-- Compliance reports table
CREATE TABLE compliance_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    report_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    total_events INT NULL,
    file_path VARCHAR(500) NULL,
    file_size BIGINT NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    INDEX idx_report_org_created (organization_id, created_at DESC),
    CONSTRAINT fk_report_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_user FOREIGN KEY (requested_by_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit alert rules table
CREATE TABLE audit_alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    event_type VARCHAR(50) NOT NULL,
    threshold_count INT NOT NULL,
    time_window_minutes INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alert_rule_org_enabled (organization_id, enabled),
    CONSTRAINT fk_alert_rule_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rule_user FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit alert instances table
CREATE TABLE audit_alert_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    rule_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    triggered_by_user_id BIGINT NULL,
    event_count INT NOT NULL,
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by_id BIGINT NULL,
    acknowledged_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alert_instance_org_created (organization_id, created_at DESC),
    INDEX idx_alert_instance_rule (rule_id),
    CONSTRAINT fk_alert_instance_rule FOREIGN KEY (rule_id) REFERENCES audit_alert_rules(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_instance_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_instance_user FOREIGN KEY (triggered_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_instance_ack FOREIGN KEY (acknowledged_by_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Alert-event join table
CREATE TABLE audit_alert_events (
    alert_instance_id BIGINT NOT NULL,
    audit_event_id BIGINT NOT NULL,
    PRIMARY KEY (alert_instance_id, audit_event_id),
    CONSTRAINT fk_alert_event_instance FOREIGN KEY (alert_instance_id) REFERENCES audit_alert_instances(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_event_audit FOREIGN KEY (audit_event_id) REFERENCES audit_events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default alert rules for organization 1
INSERT INTO audit_alert_rules (uuid, organization_id, name, description, event_type, threshold_count, time_window_minutes, enabled, created_by_id)
VALUES
    (UUID(), 1, 'Failed Login Spike', 'Alert when a user fails login 5+ times in 5 minutes', 'LOGIN_FAILURE', 5, 5, TRUE, 1),
    (UUID(), 1, 'Bulk File Deletion', 'Alert when a user deletes 20+ files in 10 minutes', 'FILE_DELETED', 20, 10, TRUE, 1);
