# Data Model: File Preview Engine

**Feature**: 006-file-preview-engine  
**Date**: 2026-05-06

## Entity Relationship Overview

```
FileEntity (existing) ─┬─── Preview (1:many per version)
                       ├─── PreviewJob (1:many, tracks generation)
                       └─── Comment (1:many, threaded)

User (existing) ────────── Comment.author
```

## Entities

### Preview

Represents a generated preview asset for a specific file version.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Associated file |
| version_id | BIGINT | FK → file_versions.id, NULL | Associated version (null = latest at generation time) |
| type | ENUM | NOT NULL | `THUMBNAIL`, `FULL_PREVIEW` |
| status | ENUM | NOT NULL | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| storage_bucket | VARCHAR(255) | NULL | MinIO bucket where preview is stored |
| storage_key_prefix | VARCHAR(500) | NULL | Path prefix in MinIO (pages stored as prefix/page-N.ext) |
| thumbnail_key | VARCHAR(500) | NULL | Direct key for thumbnail image |
| page_count | INT | DEFAULT 0 | Number of preview pages generated |
| mime_type | VARCHAR(100) | NULL | Output mime type (image/png, image/jpeg) |
| width | INT | NULL | Preview width in pixels |
| height | INT | NULL | Preview height in pixels |
| file_size_bytes | BIGINT | DEFAULT 0 | Total size of generated preview assets |
| error_message | VARCHAR(1000) | NULL | Error details if generation failed |
| generated_at | DATETIME | NULL | When generation completed |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | Record creation |
| updated_at | DATETIME | NOT NULL, DEFAULT NOW() ON UPDATE | Last update |

**Indexes**:
- `idx_preview_file_type` on (file_id, type, status) — lookup active preview for a file
- `idx_preview_version` on (version_id) — lookup by version

**Validation rules**:
- A file can have at most 1 COMPLETED THUMBNAIL and 1 COMPLETED FULL_PREVIEW per version
- When a new version is uploaded, existing previews for that file should be regenerated

---

### PreviewJob

Tracks async preview generation jobs dispatched to the worker queue.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Target file |
| version_id | BIGINT | FK → file_versions.id, NULL | Target version |
| job_type | ENUM | NOT NULL | `THUMBNAIL`, `FULL_PREVIEW` |
| status | ENUM | NOT NULL | `QUEUED`, `PROCESSING`, `COMPLETED`, `FAILED` |
| priority | INT | DEFAULT 0 | Higher = processed first |
| attempts | INT | DEFAULT 0 | Number of processing attempts |
| max_attempts | INT | DEFAULT 3 | Max retries before permanent failure |
| error_message | VARCHAR(1000) | NULL | Last error message |
| queued_at | DATETIME | NOT NULL, DEFAULT NOW() | When job was queued |
| started_at | DATETIME | NULL | When processing started |
| completed_at | DATETIME | NULL | When processing finished |

**Indexes**:
- `idx_job_status` on (status, priority DESC, queued_at ASC) — worker dequeue order
- `idx_job_file` on (file_id, job_type, status) — deduplication check

**State transitions**:
```
QUEUED → PROCESSING → COMPLETED
                   → FAILED (if attempts < max_attempts → QUEUED for retry)
                   → FAILED (if attempts >= max_attempts → permanent)
```

---

### Comment

User comments attached to a file, supporting parent-child threading.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, auto-increment | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Associated file |
| user_id | BIGINT | FK → users.id, NOT NULL | Comment author |
| parent_id | BIGINT | FK → comments.id, NULL | Parent comment for threading (NULL = top-level) |
| content | TEXT | NOT NULL | Comment body (max 5000 chars) |
| created_at | DATETIME | NOT NULL, DEFAULT NOW() | When posted |
| updated_at | DATETIME | NOT NULL, DEFAULT NOW() ON UPDATE | Last edit |

**Indexes**:
- `idx_comment_file` on (file_id, created_at) — list comments for a file
- `idx_comment_parent` on (parent_id) — fetch replies
- `idx_comment_user` on (user_id) — user's comments

**Validation rules**:
- Content must be 1–5000 characters
- parent_id must reference a comment on the same file_id
- Threading depth limited to 2 levels (top-level + replies)

---

## Migration SQL (V006)

```sql
-- Preview table
CREATE TABLE previews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    version_id BIGINT NULL,
    type ENUM('THUMBNAIL', 'FULL_PREVIEW') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    storage_bucket VARCHAR(255),
    storage_key_prefix VARCHAR(500),
    thumbnail_key VARCHAR(500),
    page_count INT DEFAULT 0,
    mime_type VARCHAR(100),
    width INT,
    height INT,
    file_size_bytes BIGINT DEFAULT 0,
    error_message VARCHAR(1000),
    generated_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_preview_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_preview_version FOREIGN KEY (version_id) REFERENCES file_versions(id) ON DELETE SET NULL,
    INDEX idx_preview_file_type (file_id, type, status),
    INDEX idx_preview_version (version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Preview jobs table
CREATE TABLE preview_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    version_id BIGINT NULL,
    job_type ENUM('THUMBNAIL', 'FULL_PREVIEW') NOT NULL,
    status ENUM('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'QUEUED',
    priority INT DEFAULT 0,
    attempts INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    error_message VARCHAR(1000),
    queued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME,
    completed_at DATETIME,
    CONSTRAINT fk_job_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_job_version FOREIGN KEY (version_id) REFERENCES file_versions(id) ON DELETE SET NULL,
    INDEX idx_job_status (status, priority DESC, queued_at ASC),
    INDEX idx_job_file (file_id, job_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Comments table
CREATE TABLE comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_comment_file (file_id, created_at),
    INDEX idx_comment_parent (parent_id),
    INDEX idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Relationship to Existing Entities

- **FileEntity** (existing): Gains `thumbnailUrl` field (already present in FileDto). Preview/thumbnail records reference files.id.
- **FileVersion** (existing): Preview records optionally link to a specific version. When null, represents the current/latest version.
- **User** (existing): Comment author. Referenced by user_id FK.
- **Organization** (existing): Preview storage uses org-scoped MinIO buckets (existing pattern from file uploads).
