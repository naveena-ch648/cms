# Data Model: Integrations & Sync

**Feature**: 015-integrations-sync  
**Date**: 2026-05-06

## Entities

### IntegrationConnection

Represents a user's linked external provider account (e.g., Google Drive).

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant isolation |
| user_id | BIGINT | FK → users.id, NOT NULL | Owning user |
| provider | VARCHAR(50) | NOT NULL | Provider type (e.g., 'GOOGLE_DRIVE') |
| provider_account_id | VARCHAR(255) | NULL | External account identifier (email/ID) |
| access_token_encrypted | TEXT | NULL | AES-256-GCM encrypted access token |
| refresh_token_encrypted | TEXT | NOT NULL | AES-256-GCM encrypted refresh token |
| token_expires_at | TIMESTAMP | NULL | Access token expiry |
| scopes | VARCHAR(1000) | NULL | Granted OAuth scopes (comma-separated) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, EXPIRED, REVOKED |
| connected_at | TIMESTAMP | NOT NULL | When the connection was established |
| last_used_at | TIMESTAMP | NULL | Last API call timestamp |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP | Last modification |

**Indexes**: `(organization_id, user_id, provider)` UNIQUE, `(organization_id, status)`

**Relationships**: Many-to-one with User; one-to-many with SyncLink

---

### Webhook

A registered endpoint for receiving CMS event notifications.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant isolation |
| name | VARCHAR(255) | NOT NULL | Display name |
| url | VARCHAR(2048) | NOT NULL | Target delivery URL (HTTPS) |
| secret | VARCHAR(255) | NULL | HMAC-SHA256 signing secret (encrypted) |
| event_types | JSON | NOT NULL | Array of subscribed event types |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, DISABLED |
| consecutive_failures | INT | NOT NULL, DEFAULT 0 | Track for auto-disable (threshold: 10) |
| created_by | BIGINT | FK → users.id, NOT NULL | Admin who created |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP | Last modification |

**Indexes**: `(organization_id, status)`, `(organization_id, event_types)` — note: JSON index for event type filtering

**Validation rules**:
- URL must be valid HTTPS endpoint (HTTP allowed only in dev mode)
- Event types must be from allowed set: file.uploaded, file.deleted, file.moved, file.version_created, folder.created, folder.deleted, workflow.status_changed, user.created, user.deactivated
- Secret is optional but recommended; if provided, minimum 16 characters

---

### WebhookDelivery

A record of each webhook delivery attempt.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| webhook_id | BIGINT | FK → webhooks.id, NOT NULL | Parent webhook |
| event_type | VARCHAR(50) | NOT NULL | e.g., 'file.uploaded' |
| event_id | VARCHAR(36) | NOT NULL | Idempotency key for the event |
| payload | JSON | NOT NULL | Delivered JSON payload |
| response_status | INT | NULL | HTTP response status code |
| response_body | TEXT | NULL | First 1000 chars of response |
| response_time_ms | INT | NULL | Round-trip time in milliseconds |
| attempt_number | INT | NOT NULL, DEFAULT 1 | 1, 2, or 3 |
| status | VARCHAR(20) | NOT NULL | PENDING, SUCCESS, FAILED, RETRYING |
| error_message | VARCHAR(500) | NULL | Error details on failure |
| delivered_at | TIMESTAMP | NULL | When delivery completed |
| next_retry_at | TIMESTAMP | NULL | Scheduled retry time |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation |

**Indexes**: `(webhook_id, created_at DESC)`, `(status, next_retry_at)` for retry worker, `(event_id)` for deduplication

**Retention**: Deliveries older than 30 days may be archived/deleted.

---

### SyncLink

A mapping between a CMS folder and an external provider folder for ongoing synchronization.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant isolation |
| connection_id | BIGINT | FK → integration_connections.id, NOT NULL | OAuth connection used |
| folder_id | BIGINT | FK → folders.id, NOT NULL | CMS folder |
| external_folder_id | VARCHAR(255) | NOT NULL | Google Drive folder ID |
| external_folder_name | VARCHAR(500) | NULL | Display name of external folder |
| direction | VARCHAR(20) | NOT NULL, DEFAULT 'BIDIRECTIONAL' | BIDIRECTIONAL, IMPORT_ONLY, EXPORT_ONLY |
| sync_interval_minutes | INT | NOT NULL, DEFAULT 15 | Minimum 5 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, PAUSED, ERROR |
| last_sync_at | TIMESTAMP | NULL | Last successful sync completion |
| next_sync_at | TIMESTAMP | NULL | Scheduled next sync |
| last_error | VARCHAR(1000) | NULL | Last error message |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | ON UPDATE CURRENT_TIMESTAMP | Last modification |

**Indexes**: `(organization_id, status)`, `(status, next_sync_at)` for scheduler, `(folder_id)` UNIQUE — one sync per folder

**Validation rules**:
- sync_interval_minutes >= 5
- One active sync link per CMS folder (prevent conflicts)
- Connection must be ACTIVE status

---

### SyncJob

A record of each sync execution.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| sync_link_id | BIGINT | FK → sync_links.id, NOT NULL | Parent sync link |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'RUNNING' | RUNNING, COMPLETED, FAILED |
| direction | VARCHAR(20) | NOT NULL | Direction executed |
| items_synced | INT | NOT NULL, DEFAULT 0 | Successfully synced items |
| items_failed | INT | NOT NULL, DEFAULT 0 | Failed items |
| items_conflicted | INT | NOT NULL, DEFAULT 0 | Conflict items |
| bytes_transferred | BIGINT | NOT NULL, DEFAULT 0 | Total bytes moved |
| error_details | JSON | NULL | Array of per-file errors |
| started_at | TIMESTAMP | NOT NULL | Job start time |
| completed_at | TIMESTAMP | NULL | Job completion time |

**Indexes**: `(sync_link_id, started_at DESC)`

---

## Entity Relationships

```
Organization
  └── IntegrationConnection (1:N)
        └── SyncLink (1:N)
              └── SyncJob (1:N)
  └── Webhook (1:N)
        └── WebhookDelivery (1:N)
  └── User (1:N)
        └── IntegrationConnection (1:N)
```

## State Transitions

### IntegrationConnection.status
```
ACTIVE → EXPIRED (token refresh fails)
EXPIRED → ACTIVE (user re-authenticates)
ACTIVE → REVOKED (user disconnects)
REVOKED → [terminal]
```

### Webhook.status
```
ACTIVE → DISABLED (admin disables OR 10 consecutive failures)
DISABLED → ACTIVE (admin re-enables)
```

### SyncLink.status
```
ACTIVE → PAUSED (user pauses)
ACTIVE → ERROR (consecutive sync failures)
PAUSED → ACTIVE (user resumes)
ERROR → ACTIVE (user re-enables after fixing issue)
```

### WebhookDelivery.status
```
PENDING → SUCCESS (2xx response)
PENDING → RETRYING (non-2xx, attempts < 3)
RETRYING → SUCCESS (retry succeeds)
RETRYING → FAILED (attempts exhausted)
PENDING → FAILED (connection refused, timeout)
```
