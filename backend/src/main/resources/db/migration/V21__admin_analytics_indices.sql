-- V21: Admin console analytics performance indices

-- Index for counting users by status within an organization
CREATE INDEX idx_users_org_status ON users (organization_id, status);

-- Index for counting files by organization
CREATE INDEX idx_files_org_status ON files (organization_id, status);

-- Index for audit events by org + created_at for trend queries
CREATE INDEX idx_audit_events_org_created ON audit_events (organization_id, created_at);

-- Index for audit events by org + event_type for upload counting
CREATE INDEX idx_audit_events_org_type_created ON audit_events (organization_id, event_type, created_at);

-- Index for audit events by org + user for top active users query
CREATE INDEX idx_audit_events_org_user_created ON audit_events (organization_id, user_id, created_at);

-- Index for workspaces by org + status for workspace counting
CREATE INDEX idx_workspaces_org_status ON workspaces (organization_id, status);
