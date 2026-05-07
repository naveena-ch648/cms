# Data Model: Metadata & Tagging System

**Feature**: 010-metadata-tagging  
**Date**: 2026-05-06

---

## Entity Relationship Diagram

```
┌─────────────────────┐         ┌─────────────────────┐
│    workspaces       │         │       files          │
│─────────────────────│         │─────────────────────│
│ id (PK)             │         │ id (PK)             │
│ uuid                │         │ uuid                │
└─────────┬───────────┘         └─────────┬───────────┘
          │ 1                             │ 1
          │                               │
          │ *                             │ *
┌─────────┴───────────┐         ┌─────────┴───────────┐
│  metadata_fields    │         │  metadata_values    │
│─────────────────────│         │─────────────────────│
│ id (PK)             │◄────────│ field_id (FK)       │
│ uuid                │    *    │ id (PK)             │
│ workspace_id (FK)   │         │ file_id (FK)        │
│ name                │         │ text_value          │
│ field_type          │         │ number_value        │
│ options (JSON)      │         │ date_value          │
│ display_order       │         │ created_at          │
│ required            │         │ updated_at          │
│ deleted_at          │         └─────────────────────┘
│ created_at          │
│ updated_at          │         ┌─────────────────────┐
└─────────────────────┘         │      file_tags      │
                                │─────────────────────│
                                │ id (PK)             │
                                │ file_id (FK)        │
                                │ workspace_id (FK)   │
                                │ name                │
                                │ created_at          │
                                │ created_by (FK)     │
                                └─────────────────────┘
```

---

## Entities

### MetadataField

Defines a custom metadata field for a workspace.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Owning workspace |
| name | VARCHAR(100) | NOT NULL | Display name of the field |
| field_type | ENUM('TEXT','NUMBER','DATE','DROPDOWN') | NOT NULL | Value type |
| description | VARCHAR(500) | NULL | Optional help text |
| options | JSON | NULL | Dropdown options array (for DROPDOWN type) |
| required | BOOLEAN | NOT NULL, DEFAULT FALSE | Whether value is mandatory |
| display_order | INT | NOT NULL, DEFAULT 0 | Sort order in UI |
| deleted_at | TIMESTAMP | NULL | Soft-delete marker |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | Creation time |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW ON UPDATE | Last modified |

**Indexes**:
- `idx_mf_workspace_active` (workspace_id, deleted_at) — list active fields
- `idx_mf_workspace_order` (workspace_id, display_order) — ordered listing

**Validation Rules**:
- name: 1–100 chars, unique per workspace (among active fields)
- field_type: must be one of the enum values
- options: required and non-empty when field_type = DROPDOWN
- Maximum 50 active fields per workspace

---

### MetadataValue

Stores an assigned value for a specific file + field combination.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| field_id | BIGINT | FK → metadata_fields.id, NOT NULL | Which field |
| file_id | BIGINT | FK → files.id, NOT NULL | Which file |
| text_value | VARCHAR(1000) | NULL | Value for TEXT and DROPDOWN fields |
| number_value | DECIMAL(20,6) | NULL | Value for NUMBER fields |
| date_value | DATE | NULL | Value for DATE fields |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | First assignment |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW ON UPDATE | Last modified |

**Indexes**:
- `idx_mv_file_field` (file_id, field_id) UNIQUE — one value per file per field
- `idx_mv_field` (field_id) — lookup all values for a field

**Validation Rules**:
- Exactly one of text_value, number_value, date_value must be non-null (matches field_type)
- For DROPDOWN fields: text_value must be in the field's options array
- For NUMBER fields: number_value must be set
- For DATE fields: date_value must be set
- For TEXT fields: text_value must be set, max 1000 chars

---

### FileTag (Tag)

Free-form tag associated with a file.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| file_id | BIGINT | FK → files.id, NOT NULL | Tagged file |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Workspace scope (for autocomplete) |
| name | VARCHAR(50) | NOT NULL | Tag text (case-insensitive) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW | When tagged |
| created_by | BIGINT | FK → users.id, NOT NULL | Who added the tag |

**Indexes**:
- `idx_tag_file_name` (file_id, name) UNIQUE — no duplicate tags per file
- `idx_tag_workspace_name` (workspace_id, name) — autocomplete queries
- `idx_tag_name` (name) — global tag search

**Validation Rules**:
- name: 1–50 chars, trimmed, stored lowercase
- Maximum 20 tags per file
- No duplicate tag names on the same file (case-insensitive)

---

## State Transitions

### MetadataField Lifecycle

```
ACTIVE ──(admin deletes)──► SOFT_DELETED (deleted_at set)
                                │
                                └──(admin restores)──► ACTIVE (deleted_at cleared)
```

- Active fields appear in UI for value assignment
- Soft-deleted fields are hidden but existing values preserved
- Values referencing soft-deleted fields are hidden from UI but remain in DB

---

## OpenSearch Index Extension

The existing `files` index in OpenSearch is extended with metadata and tag fields:

```json
{
  "mappings": {
    "properties": {
      "metadata": {
        "type": "object",
        "dynamic": true
      },
      "tags": {
        "type": "keyword"
      }
    }
  }
}
```

Each file document in OpenSearch includes:
- `metadata.{field_name}`: value indexed as appropriate type
- `tags`: array of tag name strings (keyword type for exact match filtering)

---

## Redis Cache Structure

### Tag Autocomplete Cache

- **Key**: `tags:autocomplete:{workspaceId}`
- **Type**: Sorted Set (ZSET)
- **Members**: lowercase tag names
- **Score**: 0 (all equal; using ZRANGEBYLEX for prefix matching)
- **Invalidation**: On tag create/delete within workspace
- **TTL**: None (persistent, explicit invalidation)

### Metadata Fields Cache

- **Key**: `metadata:fields:{workspaceId}`
- **Type**: String (JSON array of field definitions)
- **TTL**: 10 minutes
- **Invalidation**: On field create/update/delete
