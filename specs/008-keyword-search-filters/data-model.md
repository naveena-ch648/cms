# Data Model: Keyword Search & Filters

**Feature**: 008-keyword-search-filters  
**Date**: 2026-05-06

---

## OpenSearch Index: `cms_files`

The primary search index. One document per file in the system.

### Document Schema

| Field | Type | Purpose | Searchable | Filterable | Sortable |
|-------|------|---------|-----------|-----------|---------|
| `fileUuid` | keyword | Unique file identifier (document _id) | No | No | No |
| `fileName` | text + keyword | File name for search and exact sort | Yes (text) | No | Yes (keyword) |
| `content` | text | Extracted text content | Yes | No | No |
| `fileType` | keyword | Category (pdf, image, document, etc.) | No | Yes | No |
| `mimeType` | keyword | Original MIME type | No | Yes | No |
| `ownerUuid` | keyword | File owner/uploader user UUID | No | Yes | No |
| `ownerName` | text + keyword | Owner display name | Yes (text) | No | Yes (keyword) |
| `workspaceUuid` | keyword | Workspace UUID (tenant isolation) | No | Yes (mandatory) | No |
| `folderPath` | text + keyword | Full folder path (e.g., "/Reports/Q1") | Yes (text) | No | No |
| `folderUuid` | keyword | Parent folder UUID | No | Yes | No |
| `fileSize` | long | File size in bytes | No | No | Yes |
| `createdAt` | date | File creation timestamp | No | Yes (range) | Yes |
| `updatedAt` | date | Last modification timestamp | No | Yes (range) | Yes |
| `indexedAt` | date | When document was indexed | No | No | No |

### Index Settings

```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "index.max_result_window": 10000,
    "analysis": {
      "analyzer": {
        "file_name_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding"]
        }
      }
    }
  }
}
```

### Mapping

```json
{
  "mappings": {
    "properties": {
      "fileUuid": { "type": "keyword" },
      "fileName": {
        "type": "text",
        "analyzer": "file_name_analyzer",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "content": { "type": "text", "analyzer": "standard" },
      "fileType": { "type": "keyword" },
      "mimeType": { "type": "keyword" },
      "ownerUuid": { "type": "keyword" },
      "ownerName": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "workspaceUuid": { "type": "keyword" },
      "folderPath": {
        "type": "text",
        "fields": { "keyword": { "type": "keyword" } }
      },
      "folderUuid": { "type": "keyword" },
      "fileSize": { "type": "long" },
      "createdAt": { "type": "date", "format": "strict_date_optional_time||epoch_millis" },
      "updatedAt": { "type": "date", "format": "strict_date_optional_time||epoch_millis" },
      "indexedAt": { "type": "date", "format": "strict_date_optional_time||epoch_millis" }
    }
  }
}
```

---

## Redis Data Structures

### Recent Searches (per user)

| Key Pattern | Type | Purpose | TTL |
|-------------|------|---------|-----|
| `search:recent:{userId}` | Sorted Set | Recent search terms (score = unix timestamp) | 30 days |

- Max 20 entries per user (trim on insert via `ZREMRANGEBYRANK`)
- Score is Unix timestamp (seconds) for time-based ordering
- Value is the search query string

### Indexing Queue

| Key | Type | Purpose |
|-----|------|---------|
| `search:index` | List (LPUSH/BRPOP) | Queue for file index/delete events |
| `search:index:dlq` | List | Dead letter queue for failed indexing |

**Queue Message Schema**:
```json
{
  "action": "index|delete",
  "fileId": "string (file UUID)",
  "workspaceId": "string (workspace UUID)",
  "organizationId": "string (org UUID)",
  "timestamp": "ISO-8601 string"
}
```

---

## MySQL Additions

No new MySQL tables required. The search system reads from existing tables:

- `files` — file metadata (name, size, mime_type, owner, workspace, folder, timestamps)
- `users` — owner name resolution
- `folders` — folder path construction
- `workspaces` — workspace membership validation

---

## File Type Category Mapping

| Category | MIME Patterns |
|----------|-------------|
| `pdf` | `application/pdf` |
| `image` | `image/*` |
| `document` | `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.*`, `text/*` |
| `spreadsheet` | `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.*` |
| `presentation` | `application/vnd.ms-powerpoint`, `application/vnd.openxmlformats-officedocument.presentationml.*` |
| `video` | `video/*` |
| `audio` | `audio/*` |
| `archive` | `application/zip`, `application/x-rar*`, `application/x-7z*`, `application/gzip`, `application/x-tar` |
| `other` | Everything not matching above |

---

## Relationships

```
┌─────────────┐         ┌──────────────────┐
│  MySQL      │         │  OpenSearch       │
│  (files)    │────────▶│  (cms_files idx)  │
│             │  async  │                   │
└─────────────┘  index  └──────────────────┘
       │                        ▲
       │                        │ query
       ▼                        │
┌─────────────┐         ┌──────────────────┐
│  Redis      │         │  Spring Boot     │
│  (queue)    │────────▶│  SearchService   │
│             │  worker │                   │
└─────────────┘         └──────────────────┘
                                │
                                ▼
                        ┌──────────────────┐
                        │  React Frontend  │
                        │  (SearchPage)    │
                        └──────────────────┘
```

**Data Flow**:
1. File uploaded → Backend saves to MySQL + publishes to `search:index` Redis queue
2. Python worker consumes queue → reads MySQL metadata → writes to OpenSearch
3. User searches → Backend queries OpenSearch (filtered by workspace) → returns results
4. Frontend displays results with highlights, filters, sorting, pagination
