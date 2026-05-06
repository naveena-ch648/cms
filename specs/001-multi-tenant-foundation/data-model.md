# Data Model: Multi-Tenant Foundation

**Feature**: 001-multi-tenant-foundation
**Date**: 2026-05-05
**Source**: spec.md (Key Entities), research.md (decisions)

---

## Entity Relationship Overview

```
Organization (1) ─────┬──── (*) User
                       ├──── (*) Role ──── (*) Permission
                       ├──── (*) Group
                       ├──── (*) Workspace
                       └──── (1) OrganizationPolicy (JSON)

User (*) ────── (*) Group           (via user_groups)
User (*) ────── (*) Role            (via user_organization_roles — org scope)
User (*) ────── (*) Workspace+Role  (via user_workspace_roles — workspace scope)
Group (*) ───── (*) Workspace+Role  (via group_workspace_roles)
Role (1) ────── (0..1) Role         (parent_role_id — inheritance)
Role (*) ────── (*) Permission      (via role_permissions)
```

---

## Entities

### Organization

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique organization identifier |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public-facing tenant identifier |
| name | VARCHAR(255) | NOT NULL | Organization display name |
| slug | VARCHAR(100) | UNIQUE, NOT NULL | URL-safe identifier |
| billing_contact_email | VARCHAR(255) | NOT NULL | Billing contact email |
| status | ENUM('ACTIVE','DEACTIVATED') | NOT NULL, DEFAULT 'ACTIVE' | Tenant status |
| policies | JSON | DEFAULT '{}' | Organization-level policy overrides |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | Last update timestamp |

**Indexes**: `idx_org_slug` (slug), `idx_org_status` (status)

---

### User

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Unique user identifier |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public-facing user identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant ownership |
| email | VARCHAR(255) | NOT NULL | User email (unique within org) |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt hashed password |
| first_name | VARCHAR(100) | NOT NULL | First name |
| last_name | VARCHAR(100) | NOT NULL | Last name |
| status | ENUM('ACTIVE','INACTIVE','LOCKED') | NOT NULL, DEFAULT 'ACTIVE' | Account status |
| last_login_at | TIMESTAMP | NULL | Last successful sign-in |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | |

**Indexes**: `idx_user_org_email` UNIQUE (organization_id, email), `idx_user_org` (organization_id), `idx_user_status` (status)

**Validation rules**:
- Email must be valid format, unique within organization
- Password must satisfy organization policy (min length, complexity)
- First name and last name: 1–100 characters

---

### Role

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant ownership |
| name | VARCHAR(100) | NOT NULL | Role display name |
| description | VARCHAR(500) | NULL | Role description |
| parent_role_id | BIGINT | FK → roles.id, NULL | Inheritance parent |
| is_system | BOOLEAN | NOT NULL, DEFAULT FALSE | System-defined (non-deletable) |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes**: `idx_role_org_name` UNIQUE (organization_id, name), `idx_role_parent` (parent_role_id)

**Validation rules**:
- Name unique within organization
- Circular inheritance prohibited (enforced in service layer)
- System roles (Viewer, Editor, Admin) cannot be deleted

**State transitions**: N/A (roles do not have lifecycle states)

---

### Permission

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| name | VARCHAR(100) | UNIQUE, NOT NULL | Permission key (e.g., "manage-users") |
| description | VARCHAR(500) | NULL | Human-readable description |
| category | VARCHAR(50) | NOT NULL | Grouping (e.g., "user", "workspace", "document") |

**Note**: Permissions are system-defined, not tenant-scoped. All organizations share the same permission catalog.

---

### role_permissions (Join Table)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| role_id | BIGINT | FK → roles.id, NOT NULL | |
| permission_id | BIGINT | FK → permissions.id, NOT NULL | |

**PK**: (role_id, permission_id)

---

### Group

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant ownership |
| name | VARCHAR(100) | NOT NULL | Group name |
| description | VARCHAR(500) | NULL | |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes**: `idx_group_org_name` UNIQUE (organization_id, name)

---

### user_groups (Join Table)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| user_id | BIGINT | FK → users.id, NOT NULL | |
| group_id | BIGINT | FK → groups.id, NOT NULL | |
| created_at | TIMESTAMP | NOT NULL | When membership was added |

**PK**: (user_id, group_id)

---

### Workspace

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant ownership |
| name | VARCHAR(255) | NOT NULL | Workspace name |
| description | TEXT | NULL | |
| status | ENUM('ACTIVE','ARCHIVED','DELETED') | NOT NULL, DEFAULT 'ACTIVE' | |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**Indexes**: `idx_ws_org` (organization_id), `idx_ws_org_name` UNIQUE (organization_id, name)

**State transitions**: ACTIVE → ARCHIVED → DELETED (soft delete flow)

---

### user_organization_roles (Join Table)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| user_id | BIGINT | FK → users.id, NOT NULL | |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | |
| role_id | BIGINT | FK → roles.id, NOT NULL | Org-level default role |

**PK**: (user_id, organization_id)

---

### user_workspace_roles (Join Table)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| user_id | BIGINT | FK → users.id, NOT NULL | |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | |
| role_id | BIGINT | FK → roles.id, NOT NULL | Workspace-scoped role |
| created_at | TIMESTAMP | NOT NULL | |

**PK**: (user_id, workspace_id)

---

### group_workspace_roles (Join Table)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| group_id | BIGINT | FK → groups.id, NOT NULL | |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | |
| role_id | BIGINT | FK → roles.id, NOT NULL | |
| created_at | TIMESTAMP | NOT NULL | |

**PK**: (group_id, workspace_id)

---

### audit_events

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant ownership |
| user_id | BIGINT | FK → users.id, NULL | Acting user (NULL for system events) |
| event_type | VARCHAR(50) | NOT NULL | e.g., LOGIN_SUCCESS, LOGIN_FAILED, PERMISSION_DENIED |
| resource_type | VARCHAR(50) | NULL | e.g., USER, ROLE, WORKSPACE |
| resource_id | BIGINT | NULL | ID of affected resource |
| details | JSON | NULL | Additional event context |
| ip_address | VARCHAR(45) | NULL | Client IP (IPv4/IPv6) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Event timestamp |

**Indexes**: `idx_audit_org_type` (organization_id, event_type), `idx_audit_created` (created_at), `idx_audit_user` (user_id)

---

## Default Data (Seed)

### System Permissions

| Name | Category | Description |
|------|----------|-------------|
| view-workspace | workspace | View workspace and its contents |
| manage-workspace | workspace | Create, update, delete workspaces |
| view-users | user | View user list |
| manage-users | user | Create, update, deactivate users |
| view-roles | role | View role definitions |
| manage-roles | role | Create, update, delete roles |
| view-groups | group | View groups |
| manage-groups | group | Create, update, delete groups |
| manage-policies | organization | Configure organization policies |
| view-audit-log | audit | View audit events |

### Default Roles (per organization)

| Role | Parent | Direct Permissions |
|------|--------|--------------------|
| Viewer | — | view-workspace, view-users, view-roles, view-groups |
| Editor | Viewer | (inherits all Viewer) |
| Admin | Editor | manage-workspace, manage-users, manage-roles, manage-groups, manage-policies, view-audit-log |

### Default Policies

```json
{
  "passwordMinLength": 8,
  "passwordRequireUppercase": true,
  "passwordRequireNumber": true,
  "passwordRequireSpecialChar": false,
  "sessionTimeoutMinutes": 30,
  "maxWorkspaces": 50,
  "maxFailedLoginAttempts": 5,
  "accountLockoutMinutes": 15
}
```

---

## Permission Resolution Algorithm

```
getEffectivePermissions(userId, workspaceId):
  1. Find direct user_workspace_role for (userId, workspaceId)
  2. Find group_workspace_roles where user is member of group
  3. Find user_organization_role as fallback
  4. Collect all applicable roles from steps 1-3
  5. For each role, resolve inherited permissions (walk parent chain)
  6. Return UNION of all permissions (highest privilege wins)
```

**Caching**:
- Redis key: `perms:{orgId}:{userId}:{workspaceId}` → Set of permission names
- TTL: 5 minutes
- Invalidation: on role change, membership change, group change
