# Data Model: Workspace Folder System

**Feature**: 002-workspace-folder-system  
**Date**: 2026-05-05

## Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────┐
│   organizations  │       │    workspaces     │
│   (from Step 1)  │──1:N──│   (from Step 1)   │
└──────────────────┘       └────────┬─────────┘
                                    │ 1:N
                            ┌───────┴────────┐
                            │    folders      │
                            │                 │──┐
                            │  parent_id ─────│──┘ self-ref (0:N)
                            └──┬──────┬───┬──┘
                               │      │   │
                          1:N  │      │   │ 1:N
              ┌────────────────┘      │   └───────────────┐
              ▼                       │                   ▼
┌─────────────────────┐         1:N   │     ┌─────────────────────┐
│  folder_permissions │               │     │   folder_recents    │
│                     │               │     │                     │
│  user_id ──→ users  │               │     │  user_id ──→ users  │
│  group_id ──→ groups│               │     └─────────────────────┘
│  role_id ──→ roles  │               │
└─────────────────────┘               ▼
                            ┌─────────────────────┐
                            │  folder_favorites   │
                            │                     │
                            │  user_id ──→ users  │
                            └─────────────────────┘
```

## Entities

### Folder

Represents a node in the hierarchical folder tree within a workspace.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| uuid | CHAR(36) | NOT NULL, UNIQUE | External identifier for API exposure |
| workspace_id | BIGINT | NOT NULL, FK → workspaces(id) | Owning workspace |
| parent_id | BIGINT | NULL, FK → folders(id) | Parent folder (NULL = root-level) |
| name | VARCHAR(255) | NOT NULL | Display name |
| sort_order | INT | NOT NULL, DEFAULT 0 | Position among siblings for display ordering |
| status | ENUM('ACTIVE','DELETED') | NOT NULL, DEFAULT 'ACTIVE' | Soft-delete support |
| created_by | BIGINT | NULL, FK → users(id) | User who created the folder |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last modification timestamp |

**Indexes**:
- `UNIQUE idx_folder_uuid (uuid)`
- `UNIQUE idx_folder_ws_parent_name (workspace_id, parent_id, name)` — sibling name uniqueness (uses COALESCE for NULL parent_id handling via application logic)
- `KEY idx_folder_ws (workspace_id)` — list all folders in a workspace
- `KEY idx_folder_parent (parent_id)` — list children of a parent

**Validation Rules**:
- `name`: Non-empty, max 255 characters, must not contain `/`, `\`, or null characters
- Case-insensitive uniqueness within same parent (enforced at application layer)
- A folder cannot be its own parent

**State Transitions**:
- `ACTIVE` → `DELETED` (soft delete, cascades to all descendants)
- No restore from DELETED in this phase

### FolderPermission

Links a user or group to a folder with a specific role (explicit permission assignment).

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| folder_id | BIGINT | NOT NULL, FK → folders(id) | Target folder |
| user_id | BIGINT | NULL, FK → users(id) | Assigned user (NULL if group assignment) |
| group_id | BIGINT | NULL, FK → groups_table(id) | Assigned group (NULL if user assignment) |
| role_id | BIGINT | NOT NULL, FK → roles(id) | Assigned role (Viewer, Editor, Admin) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When permission was granted |

**Indexes**:
- `UNIQUE idx_fp_folder_user (folder_id, user_id)` — one role per user per folder
- `UNIQUE idx_fp_folder_group (folder_id, group_id)` — one role per group per folder
- `KEY idx_fp_user (user_id)` — find all folder permissions for a user
- `KEY idx_fp_role (role_id)` — find by role

**Validation Rules**:
- Exactly one of `user_id` or `group_id` must be non-NULL (enforced at application layer)
- `role_id` must reference a role in the same organization as the folder's workspace

### FolderFavorite

Tracks user-favorited folders for quick access.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| user_id | BIGINT | NOT NULL, FK → users(id) | User who favorited |
| folder_id | BIGINT | NOT NULL, FK → folders(id) | Favorited folder |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When favorited |

**Indexes**:
- `UNIQUE idx_ff_user_folder (user_id, folder_id)` — prevent duplicate favorites
- `KEY idx_ff_user (user_id)` — list user's favorites

**Validation Rules**:
- Automatically removed when the referenced folder is deleted (FK CASCADE)

### FolderRecent

Tracks recently visited folders per user.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| user_id | BIGINT | NOT NULL, FK → users(id) | User who visited |
| folder_id | BIGINT | NOT NULL, FK → folders(id) | Visited folder |
| accessed_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When last accessed |

**Indexes**:
- `UNIQUE idx_fr_user_folder (user_id, folder_id)` — one entry per user-folder pair (update timestamp on revisit)
- `KEY idx_fr_user_accessed (user_id, accessed_at DESC)` — list recents in order

**Validation Rules**:
- Maximum 10 entries per user per workspace (enforced at application layer: on insert, delete oldest if count exceeds 10)
- Automatically removed when the referenced folder is deleted (FK CASCADE)

## Relationships to Existing Entities

| Source Entity | Relationship | Target Entity | Description |
|---------------|-------------|---------------|-------------|
| Folder | N:1 | Workspace | Each folder belongs to exactly one workspace |
| Folder | N:1 (self) | Folder | Each folder optionally has one parent folder |
| Folder | 1:N (self) | Folder | Each folder can have many child folders |
| Folder | N:1 | User | `created_by` tracks who created the folder |
| FolderPermission | N:1 | Folder | Each permission targets one folder |
| FolderPermission | N:1 | User | Each permission optionally targets one user |
| FolderPermission | N:1 | Group | Each permission optionally targets one group |
| FolderPermission | N:1 | Role | Each permission grants one role |
| FolderFavorite | N:1 | User | Each favorite belongs to one user |
| FolderFavorite | N:1 | Folder | Each favorite references one folder |
| FolderRecent | N:1 | User | Each recent entry belongs to one user |
| FolderRecent | N:1 | Folder | Each recent entry references one folder |

## Permission Inheritance Model

```
Effective Permission Resolution (highest priority first):
1. Explicit FolderPermission on the target folder for the user
2. Explicit FolderPermission on the nearest ancestor folder (walk up parent chain)
3. User's workspace-level role (from user_workspace_roles)
4. No access (folder hidden from tree)

Group permissions expand to all group members and follow the same precedence.
```

## Migration: V4__create_folder_tables.sql

New Flyway migration follows existing patterns:
- InnoDB engine, utf8mb4 charset
- `fk_` prefix for foreign key constraints
- `idx_` prefix for indexes
- Timestamps with `DEFAULT CURRENT_TIMESTAMP`
- ENUM for status fields
- CASCADE on delete for child references
