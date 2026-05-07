# Data Model: Audit Logging & Compliance

**Feature**: 013-audit-compliance  
**Date**: 2026-05-06

## Entity Relationship Overview

```
Organization (1) ──── (N) AuditEvent
User (1) ──── (N) AuditEvent (nullable for system/anonymous events)
Organization (1) ──── (N) ComplianceReport
Organization (1) ──── (N) AuditAlertRule
AuditAlertRule (1) ──── (N) AuditAlertInstance
AuditAlertInstance (N) ──── (N) AuditEvent (linking events)
```

## Entities

### AuditEvent (Enhanced — existing entity)

Extends the existing `audit_events` table with additional columns for comprehensive logging.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| user_id | BIGINT | FK → users, NULLABLE | Actor (null for system/anonymous) |
| event_type | VARCHAR(50) | NOT NULL | Action identifier (e.g., FILE_UPLOADED, LOGIN_SUCCESS) |
| category | VARCHAR(30) | NOT NULL | Classification: AUTHENTICATION, FILE_OPERATION, PERMISSION_CHANGE, SHARING, WORKFLOW, SYSTEM |
| resource_type | VARCHAR(50) | NULLABLE | Target entity type (e.g., FILE, FOLDER, USER) |
| resource_id | BIGINT | NULLABLE | Target entity ID |
| resource_name | VARCHAR(255) | NULLABLE | Target display name (denormalized for search) |
| outcome | VARCHAR(10) | NOT NULL, DEFAULT 'SUCCESS' | SUCCESS or FAILURE |
| details | JSON | NULLABLE | Additional context (old/new values, metadata) |
| ip_address | VARCHAR(45) | NULLABLE | Client IP (IPv4/IPv6) |
| user_agent | VARCHAR(500) | NULLABLE | Client user-agent string |
| actor_name | VARCHAR(100) | NULLABLE | Denormalized actor display name (for anonymous: "anonymous") |
| workspace_id | BIGINT | FK → workspaces, NULLABLE | Workspace context if applicable |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Event timestamp |

**Indexes**:
- `idx_audit_org_created` (organization_id, created_at DESC) — primary listing query
- `idx_audit_org_user` (organization_id, user_id) — filter by actor
- `idx_audit_org_category` (organization_id, category) — filter by category
- `idx_audit_org_event_type` (organization_id, event_type) — filter by event type
- `idx_audit_org_resource` (organization_id, resource_type, resource_id) — resource history

**Validation Rules**:
- event_type must be a valid enum value (enforced at application layer)
- category must be one of: AUTHENTICATION, FILE_OPERATION, PERMISSION_CHANGE, SHARING, WORKFLOW, SYSTEM
- outcome must be SUCCESS or FAILURE
- No UPDATE or DELETE operations permitted (append-only)

---

### ComplianceReport (NEW)

Generated compliance reports for download.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| requested_by_id | BIGINT | FK → users, NOT NULL | Who generated it |
| report_type | VARCHAR(30) | NOT NULL | ALL_EVENTS, SECURITY_EVENTS, DATA_ACCESS |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING, GENERATING, COMPLETED, FAILED |
| date_from | DATE | NOT NULL | Report start date |
| date_to | DATE | NOT NULL | Report end date |
| total_events | INT | NULLABLE | Event count (populated on completion) |
| file_path | VARCHAR(500) | NULLABLE | Path to generated CSV file |
| file_size | BIGINT | NULLABLE | File size in bytes |
| error_message | VARCHAR(500) | NULLABLE | Error details if FAILED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Request time |
| completed_at | TIMESTAMP | NULLABLE | Completion time |

**Indexes**:
- `idx_report_org_created` (organization_id, created_at DESC) — listing
- `idx_report_uuid` (uuid) — lookup by public ID

**State Transitions**: PENDING → GENERATING → COMPLETED | FAILED

---

### AuditAlertRule (NEW)

Configurable threshold rules for suspicious pattern detection.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| name | VARCHAR(100) | NOT NULL | Human-readable rule name |
| description | VARCHAR(500) | NULLABLE | Explanation of what this detects |
| event_type | VARCHAR(50) | NOT NULL | Which event type to monitor |
| threshold_count | INT | NOT NULL | Number of events to trigger |
| time_window_minutes | INT | NOT NULL | Sliding window duration |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Active/inactive toggle |
| created_by_id | BIGINT | FK → users, NOT NULL | Rule creator |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | NULLABLE | Last modification |

**Indexes**:
- `idx_alert_rule_org_enabled` (organization_id, enabled) — active rules lookup

**Validation Rules**:
- threshold_count > 0
- time_window_minutes > 0 and ≤ 1440 (max 24 hours)
- event_type must be a valid type

---

### AuditAlertInstance (NEW)

Triggered alert instances with links to offending events.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| rule_id | BIGINT | FK → audit_alert_rules, NOT NULL | Which rule triggered |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| triggered_by_user_id | BIGINT | FK → users, NULLABLE | User who triggered |
| event_count | INT | NOT NULL | Number of events that exceeded threshold |
| window_start | TIMESTAMP | NOT NULL | Sliding window start |
| window_end | TIMESTAMP | NOT NULL | Sliding window end |
| acknowledged | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether admin has seen it |
| acknowledged_by_id | BIGINT | FK → users, NULLABLE | Who acknowledged |
| acknowledged_at | TIMESTAMP | NULLABLE | When acknowledged |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Alert creation time |

**Indexes**:
- `idx_alert_instance_org_created` (organization_id, created_at DESC) — listing
- `idx_alert_instance_rule` (rule_id) — instances per rule

---

### AuditAlertEvent (NEW — join table)

Links alert instances to the specific events that triggered them.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| alert_instance_id | BIGINT | FK → audit_alert_instances, NOT NULL | Alert |
| audit_event_id | BIGINT | FK → audit_events, NOT NULL | Event |

**Primary Key**: (alert_instance_id, audit_event_id)

---

## OpenSearch Index: `audit_events`

### Mapping

```json
{
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "organization_id": { "type": "long" },
      "user_id": { "type": "long" },
      "actor_name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "event_type": { "type": "keyword" },
      "category": { "type": "keyword" },
      "resource_type": { "type": "keyword" },
      "resource_id": { "type": "long" },
      "resource_name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "outcome": { "type": "keyword" },
      "details": { "type": "text" },
      "ip_address": { "type": "ip" },
      "user_agent": { "type": "text" },
      "workspace_id": { "type": "long" },
      "created_at": { "type": "date" }
    }
  }
}
```

---

## Flyway Migration: V20__audit_compliance.sql

Adds new columns to `audit_events`, creates `compliance_reports`, `audit_alert_rules`, `audit_alert_instances`, and `audit_alert_events` tables. Seeds default alert rules (failed logins, bulk deletions).

---

## Event Types (Application-Layer Enum)

| Category | Event Types |
|----------|-------------|
| AUTHENTICATION | LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, PASSWORD_CHANGED, TOKEN_REFRESHED |
| FILE_OPERATION | FILE_UPLOADED, FILE_DOWNLOADED, FILE_DELETED, FILE_MOVED, FILE_RENAMED, FILE_VERSIONED |
| PERMISSION_CHANGE | ROLE_ASSIGNED, ROLE_REVOKED, PERMISSION_GRANTED, PERMISSION_REVOKED |
| SHARING | LINK_CREATED, LINK_REVOKED, LINK_ACCESSED, LINK_EXPIRED |
| WORKFLOW | WORKFLOW_STARTED, APPROVAL_SUBMITTED, APPROVAL_DECIDED, STATE_CHANGED |
| SYSTEM | SCHEDULED_CLEANUP, REPORT_GENERATED, ALERT_TRIGGERED, BULK_IMPORT |
