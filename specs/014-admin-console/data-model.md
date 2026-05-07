# Data Model: Admin Console

**Feature**: 014-admin-console  
**Date**: 2026-05-06

## Entity Relationship Overview

```
Organization (1) ──── (N) User
Organization (1) ──── (N) Role
Organization (1) ──── (N) Group
Organization (1) ──── (1) StorageQuota
Organization (1) ──── (1) policies (JSON column)
User (N) ──── (1) Role (via UserOrganizationRole)
User (N) ──── (N) Group (via UserGroup)
Role (N) ──── (N) Permission (via role_permissions)
```

## Entities

> **Note**: All entities below already exist in the database. The admin console feature creates NO new tables. It adds only administrative operations and analytics views on existing data.

### User (existing — read/write via admin)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| email | VARCHAR(255) | UNIQUE per org, NOT NULL | Login identifier |
| password_hash | VARCHAR(255) | NOT NULL | Bcrypt-hashed password |
| first_name | VARCHAR(100) | NOT NULL | Display name |
| last_name | VARCHAR(100) | NOT NULL | Display name |
| status | ENUM | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, LOCKED |
| last_login_at | TIMESTAMP | NULLABLE | Last successful login time |
| created_at | TIMESTAMP | NOT NULL | Account creation time |
| updated_at | TIMESTAMP | NOT NULL | Last modification time |

**Admin Operations**: List (search, filter by status), Create, Update (name, status), Change role, Reset password, Bulk status/role change, Deactivate (with self-deactivation protection)

### Role (existing — read/write via admin)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| name | VARCHAR(100) | NOT NULL | Role display name |
| description | VARCHAR(500) | NULLABLE | Role purpose |
| parent_role_id | BIGINT | FK → roles, NULLABLE | Inheritance parent |
| is_system | BOOLEAN | NOT NULL, DEFAULT FALSE | True for Viewer/Editor/Admin |
| created_at | TIMESTAMP | NOT NULL | Creation time |
| updated_at | TIMESTAMP | NOT NULL | Last modification time |

**Admin Operations**: List (with user counts), Create custom, Update custom, Delete custom (prevent if assigned), View permission matrix. System roles (is_system=true) are read-only.

### Permission (existing — read-only via admin)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Permission identifier |
| description | VARCHAR(255) | NULLABLE | Human-readable description |
| category | VARCHAR(50) | NULLABLE | Grouping category |

**System Permissions (10)**: view-workspace, manage-workspace, view-users, manage-users, view-roles, manage-roles, view-groups, manage-groups, manage-policies, view-audit-log

### Group (existing — read/write via admin)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations, NOT NULL | Tenant scope |
| name | VARCHAR(100) | NOT NULL | Group name |
| description | VARCHAR(500) | NULLABLE | Group purpose |
| created_at | TIMESTAMP | NOT NULL | Creation time |
| updated_at | TIMESTAMP | NOT NULL | Last modification time |

**Admin Operations**: List (with member counts), Create, Update, Delete, Add/remove members

### StorageQuota (existing — read/write via admin)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| organization_id | BIGINT | FK → organizations, UNIQUE, NOT NULL | One quota per org |
| max_storage_bytes | BIGINT | NOT NULL, DEFAULT 10737418240 | Max total storage (10GB default) |
| used_storage_bytes | BIGINT | NOT NULL, DEFAULT 0 | Current usage |
| max_file_size_bytes | BIGINT | NULLABLE | Max single file size |
| allowed_extensions | JSON | NULLABLE | Whitelist of extensions |
| blocked_extensions | JSON | NULLABLE | Blacklist of extensions |
| trash_retention_days | INT | NOT NULL, DEFAULT 30 | Days before permanent delete |
| updated_at | TIMESTAMP | NOT NULL | Last modification time |

**Admin Operations**: View current usage vs limits, Update all configurable fields (maxStorageBytes, maxFileSizeBytes, allowedExtensions, blockedExtensions, trashRetentionDays)

**Validation Rules**:
- max_storage_bytes must be positive (cannot set to 0)
- If max_storage_bytes is reduced below used_storage_bytes, existing files are preserved but new uploads are blocked
- max_file_size_bytes must be positive or null (null = no limit)
- allowed_extensions and blocked_extensions are mutually exclusive (only one should be set)
- trash_retention_days must be between 1 and 365

### Organization Policies (existing — JSON column on organizations table)

| Policy Key | Type | Default | Description |
|------------|------|---------|-------------|
| password_min_length | INT | 8 | Minimum password length |
| password_require_uppercase | BOOLEAN | true | Require uppercase letter |
| password_require_lowercase | BOOLEAN | true | Require lowercase letter |
| password_require_number | BOOLEAN | true | Require number |
| password_require_special | BOOLEAN | false | Require special character |
| session_timeout_minutes | INT | 60 | Session idle timeout |

**Admin Operations**: View current policies, Update individual policy values via `PUT /api/v1/organizations/{orgId}/policies`

## New Database Objects (V21 Migration)

### Analytics Index (performance optimization)

No new tables are needed. Add a composite index to optimize admin analytics queries:

```sql
-- Optimize user counting by status for admin analytics
CREATE INDEX idx_users_org_status ON users (organization_id, status);

-- Optimize file counting/storage analytics
CREATE INDEX idx_files_org_status_created ON files (organization_id, status, created_at);
```

## State Transitions

### User Status Transitions (admin-initiated)

```
ACTIVE ──[deactivate]──→ INACTIVE
INACTIVE ──[reactivate]──→ ACTIVE
ACTIVE ──[lock (automated)]──→ LOCKED
LOCKED ──[reactivate]──→ ACTIVE
```

**Constraints**:
- Admin cannot deactivate self
- Last admin cannot be deactivated or have role changed
- All status transitions are logged as audit events
