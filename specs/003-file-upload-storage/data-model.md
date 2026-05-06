# Data Model: File Upload & Storage System

**Feature**: 003-file-upload-storage  
**Date**: 2026-05-05

## Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│  organizations   │──1:N──│   workspaces     │──1:N──│    folders       │
│   (from 001)     │       │   (from 001)     │       │   (from 002)     │
└───────┬──────────┘       └──────────────────┘       └────────┬─────────┘
        │ 1:1                                                  │ 1:N
        ▼                                                      ▼
┌──────────────────┐                              ┌─────────────────────┐
│  storage_quotas  │                              │       files         │
│                  │                              │                     │
│  org-level quota │                              │  storage_key → MinIO│
└──────────────────┘                              │  uploader → users   │
                                                  │  status (ACTIVE/    │
                                                  │   TRASHED/DELETED)  │
                                                  └──────┬──────┬───────┘
                                                         │      │
                                                    1:N  │      │ 0:1
                                                         ▼      ▼
                                              ┌──────────────┐ ┌────────────────┐
                                              │ file_chunks  │ │ file_thumbnails│
                                              │ (audit only) │ │                │
                                              └──────────────┘ └────────────────┘

┌──────────────────────────────┐
│   upload_sessions (Redis)    │
│                              │
│   Active chunked upload      │
│   state tracked in Redis     │
│   with 24h TTL               │
│                              │
│   Completed sessions written │
│   to files table in MySQL    │
└──────────────────────────────┘
```

## Entities

### File

Core entity representing an uploaded file stored in MinIO with metadata in MySQL.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| uuid | CHAR(36) | NOT NULL, UNIQUE | External identifier for API exposure |
| folder_id | BIGINT | NOT NULL, FK → folders(id) | Parent folder |
| organization_id | BIGINT | NOT NULL, FK → organizations(id) | Tenant ownership (denormalized for quota queries) |
| workspace_id | BIGINT | NOT NULL, FK → workspaces(id) | Workspace ownership (denormalized for queries) |
| name | VARCHAR(255) | NOT NULL | Display filename |
| original_name | VARCHAR(255) | NOT NULL | Original filename as uploaded |
| size_bytes | BIGINT | NOT NULL | File size in bytes |
| mime_type | VARCHAR(127) | NOT NULL | MIME type (e.g., image/jpeg, application/pdf) |
| storage_key | VARCHAR(512) | NOT NULL | Object key in MinIO (org_id/workspace_id/folder_path/uuid_name) |
| storage_bucket | VARCHAR(63) | NOT NULL | MinIO bucket name |
| checksum_sha256 | VARCHAR(64) | NULL | SHA-256 hash of complete file |
| status | ENUM('ACTIVE','TRASHED','DELETED') | NOT NULL, DEFAULT 'ACTIVE' | Lifecycle state |
| trashed_at | TIMESTAMP | NULL | When file was moved to trash |
| trashed_by | BIGINT | NULL, FK → users(id) | Who trashed the file |
| permanent_delete_at | TIMESTAMP | NULL | Scheduled permanent deletion date |
| uploaded_by | BIGINT | NOT NULL, FK → users(id) | Uploader |
| upload_completed_at | TIMESTAMP | NULL | When upload finished (NULL = in progress) |
| description | VARCHAR(1000) | NULL | Optional file description |
| tags | JSON | DEFAULT '[]' | Array of string tags |
| download_count | INT | NOT NULL, DEFAULT 0 | Total download count |
| last_accessed_at | TIMESTAMP | NULL | Last download/preview timestamp |
| thumbnail_key | VARCHAR(512) | NULL | MinIO key for thumbnail (if generated) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last modification |

**Indexes**:
- `UNIQUE idx_file_uuid (uuid)`
- `KEY idx_file_folder (folder_id, status)` — list files in a folder
- `KEY idx_file_org (organization_id)` — org-level queries and quota aggregation
- `KEY idx_file_workspace (workspace_id)` — workspace-level queries
- `KEY idx_file_status (status)` — trash cleanup queries
- `KEY idx_file_uploaded_by (uploaded_by)` — files by user
- `KEY idx_file_permanent_delete (permanent_delete_at)` — scheduled cleanup job

**Validation Rules**:
- `name`: 1–255 characters, must not contain `/`, `\`, null characters
- `size_bytes`: must be ≥ 0
- `mime_type`: must be valid MIME format
- Filename uniqueness within folder enforced at application layer (rename on conflict)

**State Transitions**:
- `ACTIVE` → `TRASHED` (soft delete, sets `trashed_at`, `permanent_delete_at = trashed_at + retention_days`)
- `TRASHED` → `ACTIVE` (restore from trash, clears `trashed_at`, `permanent_delete_at`)
- `TRASHED` → `DELETED` (permanent deletion — removes from MinIO, hard-deletes row or keeps as audit record)
- `ACTIVE` → `ACTIVE` (rename, move, update metadata)

---

### StorageQuota

Organization-level storage allocation and usage tracking.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| organization_id | BIGINT | NOT NULL, UNIQUE, FK → organizations(id) | One quota per org |
| max_storage_bytes | BIGINT | NOT NULL, DEFAULT 10737418240 | Max storage (default 10 GB) |
| used_storage_bytes | BIGINT | NOT NULL, DEFAULT 0 | Current usage |
| max_file_size_bytes | BIGINT | NOT NULL, DEFAULT 10737418240 | Max single file size (default 10 GB) |
| allowed_extensions | JSON | DEFAULT 'null' | Whitelist of allowed extensions (null = allow all) |
| blocked_extensions | JSON | DEFAULT '[]' | Blacklist of blocked extensions |
| trash_retention_days | INT | NOT NULL, DEFAULT 30 | Days to keep trashed files |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update |

**Indexes**:
- `UNIQUE idx_quota_org (organization_id)`

**Validation Rules**:
- `max_storage_bytes` must be > 0
- `max_file_size_bytes` must be > 0 and ≤ `max_storage_bytes`
- `trash_retention_days` must be ≥ 1

---

### Upload Session (Redis Structure)

Active chunked upload state stored in Redis. Not a MySQL table.

**Redis Key**: `upload_session:{session_uuid}`  
**TTL**: 86400 seconds (24 hours)  
**Type**: Redis Hash

| Field | Type | Description |
|-------|------|-------------|
| session_id | String | UUID for this upload session |
| file_name | String | Target filename |
| folder_id | Long | Target folder ID |
| organization_id | Long | Tenant ID |
| workspace_id | Long | Workspace ID |
| uploaded_by | Long | Uploading user ID |
| total_size | Long | Total file size in bytes |
| chunk_size | Long | Size per chunk (default 5 MB) |
| total_chunks | Int | Expected number of chunks |
| completed_chunks | String (JSON array) | List of completed chunk numbers |
| s3_upload_id | String | MinIO multipart upload ID |
| s3_bucket | String | Target MinIO bucket |
| s3_key | String | Target MinIO object key |
| mime_type | String | File MIME type |
| status | String | INITIATED / IN_PROGRESS / COMPLETING / FAILED |
| created_at | String (ISO timestamp) | Session creation time |
| last_activity_at | String (ISO timestamp) | Last chunk received time |

---

## Flyway Migrations

### V6__create_file_tables.sql

Creates `files` and `storage_quotas` tables.

### V7__seed_file_permissions.sql

Seeds file-related permissions:
- `FILE_UPLOAD` — upload files to a folder
- `FILE_DOWNLOAD` — download/preview files
- `FILE_MANAGE` — rename, move, copy, delete files
- `FILE_TRASH_RESTORE` — restore files from trash
- `FILE_TRASH_DELETE` — permanently delete files from trash

Assigns to existing roles:
- **Viewer**: `FILE_DOWNLOAD`
- **Editor**: `FILE_UPLOAD`, `FILE_DOWNLOAD`, `FILE_MANAGE`
- **Admin**: All file permissions

### V8__seed_default_storage_quota.sql

Creates default `storage_quotas` entry for existing organizations.

---

## MinIO Bucket Structure

```
cms-{org_slug}/
├── {workspace_id}/
│   ├── {folder_uuid}/
│   │   ├── {file_uuid}_{original_name}        # Original file
│   │   └── thumbs/
│   │       └── {file_uuid}_thumb.webp          # Thumbnail
│   └── ...
└── ...
```

- One bucket per organization: `cms-{org_slug}`
- Buckets created on-demand when first file is uploaded to an organization
- Object keys include full path for human-readability in MinIO console
