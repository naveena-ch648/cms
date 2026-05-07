# Data Model: Dashboard & Notifications

**Feature**: 012-dashboard-notifications  
**Date**: 2026-05-06

## New Entities

### ActivityEvent

Tracks user actions for the activity feed.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| actor_id | BIGINT | FK → users(id), NOT NULL | User who performed action |
| actor_name | VARCHAR(255) | NOT NULL | Denormalized display name |
| action_type | ENUM | NOT NULL | Type of action performed |
| target_type | VARCHAR(50) | NOT NULL | Entity type (FILE, FOLDER, WORKSPACE, APPROVAL) |
| target_id | VARCHAR(36) | NOT NULL | UUID of target entity |
| target_name | VARCHAR(255) | NOT NULL | Denormalized display name |
| workspace_id | BIGINT | FK → workspaces(id), NOT NULL | Workspace context |
| organization_id | BIGINT | FK → organizations(id), NOT NULL | Tenant isolation |
| metadata | JSON | NULL | Additional context (e.g., from_state, to_state) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | When action occurred |

**Action Types**: `FILE_UPLOADED`, `FILE_DOWNLOADED`, `FILE_SHARED`, `FILE_MOVED`, `FILE_DELETED`, `FOLDER_CREATED`, `COMMENT_ADDED`, `APPROVAL_SUBMITTED`, `APPROVAL_DECIDED`, `WORKFLOW_TRANSITIONED`

**Indexes**:
- `idx_activity_org_created` ON (organization_id, created_at DESC) — for org-wide feed
- `idx_activity_workspace_created` ON (workspace_id, created_at DESC) — for workspace feed
- `idx_activity_actor_created` ON (actor_id, created_at DESC) — for user's own activity

### UserAlert

Tracks dismissible alerts per user.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| user_id | BIGINT | FK → users(id), NOT NULL | Alert recipient |
| alert_type | ENUM | NOT NULL | Category of alert |
| severity | ENUM | NOT NULL | INFO, WARNING, CRITICAL |
| title | VARCHAR(255) | NOT NULL | Alert headline |
| message | VARCHAR(500) | NOT NULL | Alert description |
| target_type | VARCHAR(50) | NULL | Related entity type |
| target_id | VARCHAR(36) | NULL | Related entity UUID |
| dismissed | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether user dismissed |
| dismissed_at | TIMESTAMP | NULL | When dismissed |
| expires_at | TIMESTAMP | NULL | Auto-expire time |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | When generated |

**Alert Types**: `STORAGE_WARNING`, `STORAGE_CRITICAL`, `LINK_EXPIRING`, `UPLOAD_FAILED`, `SYSTEM_ANNOUNCEMENT`

**Severity**: `INFO`, `WARNING`, `CRITICAL`

**Indexes**:
- `idx_alerts_user_dismissed` ON (user_id, dismissed, created_at DESC) — for active alerts

## Existing Entities (Extended)

### Notification (existing — no schema changes)

Already has: id, uuid, recipient_id, type, title, message, target_type, target_id, actor_id, is_read, read_at, created_at.

Existing types: `MENTION`, `TASK_ASSIGNED`, `TASK_COMPLETED`, `APPROVAL_REQUESTED`, `APPROVAL_APPROVED`, `APPROVAL_REJECTED`

**New types to add**: `FILE_SHARED`, `WORKFLOW_TRANSITIONED`

### FileEntity (existing — no schema changes)

Already has `lastAccessedAt` field used for recent files query.

### SharedLink (existing — no schema changes)

Already has `expiresAt`, `status`, `createdBy` used for shared items and link expiry alerts.

### StorageQuota (existing — no schema changes)

Already has `maxStorageBytes`, `usedStorageBytes` used for storage usage display and alerts.

## Relationships

```
ActivityEvent → User (actor)
ActivityEvent → Workspace
ActivityEvent → Organization

UserAlert → User (recipient)

Notification → User (recipient) [existing]
Notification → User (actor) [existing]
```

## Migration: V19__dashboard_activity_alerts.sql

Creates:
- `activity_events` table with indexes
- `user_alerts` table with indexes
- Adds `FILE_SHARED` and `WORKFLOW_TRANSITIONED` to notifications type enum
