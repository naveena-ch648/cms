-- V22: Integration connections, webhooks, sync links, sync jobs
-- Feature: 015-integrations-sync

CREATE TABLE integration_connections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_account_id VARCHAR(255) NULL,
    access_token_encrypted TEXT NULL,
    refresh_token_encrypted TEXT NOT NULL,
    token_expires_at TIMESTAMP NULL,
    scopes VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    connected_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ic_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_ic_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_ic_org_user_provider UNIQUE (organization_id, user_id, provider)
);

CREATE INDEX idx_ic_org_status ON integration_connections(organization_id, status);

CREATE TABLE webhooks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    secret VARCHAR(255) NULL,
    event_types JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    consecutive_failures INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wh_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_wh_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_wh_org_status ON webhooks(organization_id, status);

CREATE TABLE webhook_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    webhook_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    payload JSON NOT NULL,
    response_status INT NULL,
    response_body TEXT NULL,
    response_time_ms INT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500) NULL,
    delivered_at TIMESTAMP NULL,
    next_retry_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wd_webhook FOREIGN KEY (webhook_id) REFERENCES webhooks(id) ON DELETE CASCADE
);

CREATE INDEX idx_wd_webhook_created ON webhook_deliveries(webhook_id, created_at DESC);
CREATE INDEX idx_wd_status_retry ON webhook_deliveries(status, next_retry_at);
CREATE INDEX idx_wd_event_id ON webhook_deliveries(event_id);

CREATE TABLE sync_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    external_folder_id VARCHAR(255) NOT NULL,
    external_folder_name VARCHAR(500) NULL,
    direction VARCHAR(20) NOT NULL DEFAULT 'BIDIRECTIONAL',
    sync_interval_minutes INT NOT NULL DEFAULT 15,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_sync_at TIMESTAMP NULL,
    next_sync_at TIMESTAMP NULL,
    last_error VARCHAR(1000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sl_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_sl_connection FOREIGN KEY (connection_id) REFERENCES integration_connections(id),
    CONSTRAINT fk_sl_folder FOREIGN KEY (folder_id) REFERENCES folders(id),
    CONSTRAINT uq_sl_folder UNIQUE (folder_id)
);

CREATE INDEX idx_sl_org_status ON sync_links(organization_id, status);
CREATE INDEX idx_sl_status_next ON sync_links(status, next_sync_at);

CREATE TABLE sync_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    sync_link_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    direction VARCHAR(20) NOT NULL,
    items_synced INT NOT NULL DEFAULT 0,
    items_failed INT NOT NULL DEFAULT 0,
    items_conflicted INT NOT NULL DEFAULT 0,
    bytes_transferred BIGINT NOT NULL DEFAULT 0,
    error_details JSON NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_sj_sync_link FOREIGN KEY (sync_link_id) REFERENCES sync_links(id) ON DELETE CASCADE
);

CREATE INDEX idx_sj_link_started ON sync_jobs(sync_link_id, started_at DESC);
