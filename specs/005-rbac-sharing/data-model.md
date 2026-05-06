# Data Model: RBAC & Sharing System

**Feature**: 005-rbac-sharing  
**Date**: 2026-05-06

## Entity Relationship Overview

```
folders (existing)
  └── folder_permissions (extended)
        ├── user_id → users
        ├── group_id → groups_table
        └── role_id → roles

files (existing)
  └── shared_links (NEW)
        ├── created_by → users
        └── shared_link_accesses (NEW)
```

## Entities

### FolderPermission (Extended)

Existing table `folder_permissions` extended with inheritance metadata.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | BIGINT PK | No | Auto-increment |
| folder_id | BIGINT FK | No | Reference to folders.id |
| user_id | BIGINT FK | Yes | Reference to users.id (XOR with group_id) |
| group_id | BIGINT FK | Yes | Reference to groups_table.id (XOR with user_id) |
| role_id | BIGINT FK | No | Reference to roles.id (Viewer/Editor/Admin) |
| is_override | BOOLEAN | No | Default FALSE. TRUE = explicit override of inherited permission |
| created_at | TIMESTAMP | No | Auto-set on creation |

**Validation rules**:
- Exactly one of (user_id, group_id) must be set
- Unique constraint on (folder_id, user_id) and (folder_id, group_id) — already exists
- role_id must reference a valid system role

**State transitions**: N/A (stateless assignment, only create/update/delete)

### SharedLink (New)

Represents a secure external sharing link for a file or folder.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | BIGINT PK | No | Auto-increment |
| uuid | CHAR(36) | No | Public identifier (unique) |
| token | VARCHAR(64) | No | Cryptographic random token for URL (unique, indexed) |
| resource_type | ENUM('FILE','FOLDER') | No | Type of shared resource |
| file_id | BIGINT FK | Yes | Reference to files.id (when resource_type=FILE) |
| folder_id | BIGINT FK | Yes | Reference to folders.id (when resource_type=FOLDER) |
| created_by | BIGINT FK | No | Reference to users.id (link creator) |
| workspace_id | BIGINT FK | No | Reference to workspaces.id (for admin dashboard queries) |
| password_hash | VARCHAR(255) | Yes | Bcrypt hash of password (null = no password) |
| expires_at | TIMESTAMP | Yes | Expiration timestamp (null = never expires) |
| allow_download | BOOLEAN | No | Default TRUE. FALSE = preview only |
| watermark_enabled | BOOLEAN | No | Default FALSE. TRUE = apply watermark overlay |
| max_views | INT | Yes | Optional view limit (null = unlimited) |
| view_count | INT | No | Default 0. Incremented on each access |
| status | ENUM('ACTIVE','REVOKED','EXPIRED') | No | Default ACTIVE |
| last_accessed_at | TIMESTAMP | Yes | Last access timestamp |
| created_at | TIMESTAMP | No | Auto-set |
| updated_at | TIMESTAMP | No | Auto-updated |

**Validation rules**:
- Exactly one of (file_id, folder_id) must be set, matching resource_type
- token must be cryptographically random (32 bytes hex-encoded = 64 chars)
- expires_at must be in the future at creation time (if set)
- Creator must have Editor or Admin permission on the target resource

**State transitions**:
- ACTIVE → REVOKED (manual revocation)
- ACTIVE → EXPIRED (system checks expires_at on access)
- REVOKED → (terminal, no transitions)
- EXPIRED → ACTIVE (admin extends expiry)

### SharedLinkAccess (New)

Tracks individual access events for audit and statistics.

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | BIGINT PK | No | Auto-increment |
| shared_link_id | BIGINT FK | No | Reference to shared_links.id |
| accessed_at | TIMESTAMP | No | Auto-set |
| ip_address | VARCHAR(45) | Yes | Viewer's IP (IPv4/IPv6) |
| user_agent | VARCHAR(500) | Yes | Browser user-agent |

**Validation rules**:
- shared_link_id must reference an existing shared_link
- IP address truncated/anonymized per GDPR if applicable

## Indexes

### shared_links
- `UNIQUE idx_sl_uuid (uuid)`
- `UNIQUE idx_sl_token (token)`
- `KEY idx_sl_file (file_id)`
- `KEY idx_sl_folder (folder_id)`
- `KEY idx_sl_creator (created_by)`
- `KEY idx_sl_workspace_status (workspace_id, status)`
- `KEY idx_sl_expires (status, expires_at)` — for expiry cleanup queries

### shared_link_accesses
- `KEY idx_sla_link (shared_link_id)`
- `KEY idx_sla_link_time (shared_link_id, accessed_at)`

## Migration Plan

### V10__add_permission_inheritance.sql
```sql
ALTER TABLE folder_permissions 
  ADD COLUMN is_override BOOLEAN NOT NULL DEFAULT FALSE;
```

### V11__create_shared_links_tables.sql
```sql
CREATE TABLE shared_links ( ... );
CREATE TABLE shared_link_accesses ( ... );
```

## Cache Strategy

| Cache Key | Value | TTL | Invalidation |
|-----------|-------|-----|--------------|
| `folder_perm:{userId}:{folderId}` | Effective role name or "NONE" | 5min | On permission change to folder or ancestors |
| `share_link:{token}` | Link metadata JSON (status, expires, password_hash, flags) | 2min | On link update/revoke |
