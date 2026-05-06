# Data Model: File Versioning

**Feature**: 004-file-versioning  
**Date**: 2026-05-06

## Entity Relationship Diagram

```
┌──────────────────┐       ┌──────────────────────────┐
│      files       │──1:N──│     file_versions        │
│   (from 003)     │       │                          │
│                  │       │  version_number          │
│  current_version ┼───────│  storage_key → MinIO     │
│                  │       │  uploaded_by → users     │
└──────────────────┘       │  size_bytes, checksum    │
                           │  change_note             │
                           └──────────────────────────┘
```

## Entities

### FileVersion

Represents a single version of a file. Each upload creates a new version record.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal identifier |
| uuid | CHAR(36) | NOT NULL, UNIQUE | External identifier for API exposure |
| file_id | BIGINT | NOT NULL, FK → files(id) | Parent file |
| version_number | INT | NOT NULL | Sequential version number (1, 2, 3...) |
| storage_key | VARCHAR(512) | NOT NULL | Object key in MinIO for this version's content |
| storage_bucket | VARCHAR(63) | NOT NULL | MinIO bucket name |
| size_bytes | BIGINT | NOT NULL | File size for this version |
| mime_type | VARCHAR(127) | NOT NULL | MIME type for this version |
| checksum_sha256 | VARCHAR(64) | NULL | SHA-256 hash of version content |
| change_note | VARCHAR(500) | NULL | Optional note describing what changed |
| uploaded_by | BIGINT | NOT NULL, FK → users(id) | Who uploaded this version |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When this version was created |

**Indexes**:
- `UNIQUE idx_file_version (file_id, version_number)` — one version number per file
- `UNIQUE idx_version_uuid (uuid)`
- `KEY idx_version_file_id (file_id)` — list versions for a file

**Validation Rules**:
- `version_number`: must be ≥ 1, sequential per file
- `change_note`: 0–500 characters

---

## Schema Changes to Existing Tables

### files table additions

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| current_version_id | BIGINT | NULL, FK → file_versions(id) | Points to the active version |
| version_count | INT | NOT NULL, DEFAULT 1 | Total number of versions |

---

## State Transitions

- **New file upload**: Creates file record + first version (version_number=1). `current_version_id` points to this version.
- **New version upload**: Creates new version record (version_number = version_count + 1). Updates file's `current_version_id` and increments `version_count`. Updates file's `size_bytes`, `mime_type`, `storage_key` to match new version.
- **Restore version**: Creates a new version (copy of the target version's storage object). `current_version_id` updated. `version_count` incremented.
- **File trash**: All versions preserved. No version changes.
- **File permanent delete**: All version records deleted. All version storage objects deleted from MinIO.
