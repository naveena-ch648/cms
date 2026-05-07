-- V19: Dashboard activity events and user alerts tables
-- Feature: 012-dashboard-notifications

-- Activity events table for dashboard feed
CREATE TABLE activity_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    actor_id BIGINT NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    action_type ENUM('FILE_UPLOADED','FILE_DOWNLOADED','FILE_SHARED','FILE_MOVED','FILE_DELETED','FOLDER_CREATED','COMMENT_ADDED','APPROVAL_SUBMITTED','APPROVAL_DECIDED','WORKFLOW_TRANSITIONED') NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    target_name VARCHAR(255) NOT NULL,
    workspace_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT fk_activity_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_activity_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

CREATE INDEX idx_activity_org_created ON activity_events(organization_id, created_at DESC);
CREATE INDEX idx_activity_workspace_created ON activity_events(workspace_id, created_at DESC);
CREATE INDEX idx_activity_actor_created ON activity_events(actor_id, created_at DESC);

-- User alerts table for dismissible system alerts
CREATE TABLE user_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    alert_type ENUM('STORAGE_WARNING','STORAGE_CRITICAL','LINK_EXPIRING','UPLOAD_FAILED','SYSTEM_ANNOUNCEMENT') NOT NULL,
    severity ENUM('INFO','WARNING','CRITICAL') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id VARCHAR(36) NULL,
    dismissed BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_alert_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_alerts_user_dismissed ON user_alerts(user_id, dismissed, created_at DESC);

-- Add new notification types to the enum
ALTER TABLE notifications MODIFY COLUMN type ENUM('MENTION','TASK_ASSIGNED','TASK_COMPLETED','APPROVAL_REQUESTED','APPROVAL_APPROVED','APPROVAL_REJECTED','FILE_SHARED','WORKFLOW_TRANSITIONED') NOT NULL;
