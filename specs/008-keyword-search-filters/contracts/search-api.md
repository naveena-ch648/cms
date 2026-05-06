# Search API Contract

**Feature**: 008-keyword-search-filters  
**Base URL**: `/api/v1`  
**Auth**: All endpoints require valid JWT (Bearer token)

---

## 1. Full-Text Search

### `GET /api/v1/search`

Search files by keyword with optional filters, sorting, and pagination.

**Query Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | string | No | (empty) | Search keyword (max 500 chars) |
| `workspaceId` | string (UUID) | Yes | — | Workspace to search within |
| `fileType` | string[] | No | — | Filter by type: pdf, image, document, spreadsheet, presentation, video, audio, archive, other |
| `ownerUuid` | string (UUID) | No | — | Filter by file owner |
| `dateFrom` | string (ISO date) | No | — | Filter: created/modified after this date |
| `dateTo` | string (ISO date) | No | — | Filter: created/modified before this date |
| `dateField` | string | No | `updatedAt` | Which date field to filter: `createdAt` or `updatedAt` |
| `sortBy` | string | No | `relevance` | Sort field: relevance, name, dateModified, dateCreated, fileSize, owner |
| `sortOrder` | string | No | `desc` | Sort direction: `asc` or `desc` |
| `page` | integer | No | 0 | Page number (0-indexed) |
| `size` | integer | No | 20 | Results per page (max 100) |

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "results": [
      {
        "fileUuid": "abc-123",
        "fileName": "Q1 Budget Report.pdf",
        "fileType": "pdf",
        "mimeType": "application/pdf",
        "fileSize": 245760,
        "ownerUuid": "user-456",
        "ownerName": "John Smith",
        "folderPath": "/Finance/Reports",
        "folderUuid": "folder-789",
        "createdAt": "2026-03-15T10:30:00Z",
        "updatedAt": "2026-04-20T14:22:00Z",
        "highlights": [
          "The <mark>quarterly</mark> <mark>report</mark> shows revenue growth of 15% compared to..."
        ],
        "score": 8.75
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalResults": 142,
      "totalPages": 8
    },
    "query": "quarterly report",
    "filters": {
      "fileType": ["pdf"],
      "ownerUuid": null,
      "dateFrom": null,
      "dateTo": null
    }
  }
}
```

**Error Responses**:

| Code | Condition |
|------|-----------|
| 400 | Missing workspaceId, query > 500 chars, invalid filter values |
| 401 | Invalid/missing JWT |
| 403 | User not a member of the specified workspace |
| 503 | Search engine unavailable |

---

## 2. Autocomplete

### `GET /api/v1/search/autocomplete`

Get typeahead suggestions as the user types.

**Query Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | string | Yes | — | Prefix text (min 2 chars for file suggestions) |
| `workspaceId` | string (UUID) | Yes | — | Workspace scope |
| `limit` | integer | No | 5 | Max suggestions to return (max 10) |

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "files": [
      {
        "fileUuid": "abc-123",
        "fileName": "Budget_2026.xlsx",
        "folderPath": "/Finance",
        "fileType": "spreadsheet"
      }
    ],
    "recentSearches": [
      "budget planning",
      "budget forecast"
    ]
  }
}
```

**Behavior**:
- If `q` < 2 characters: only `recentSearches` returned (no file suggestions)
- If `q` >= 2 characters: both `files` and `recentSearches` returned
- `recentSearches` filtered by prefix match against user's recent search history
- Results scoped to workspace

---

## 3. Save Recent Search

### `POST /api/v1/search/recent`

Save a search term to user's recent searches (called on full search execution).

**Request Body**:

```json
{
  "query": "quarterly report",
  "workspaceId": "ws-uuid"
}
```

**Response** (200 OK):

```json
{
  "success": true,
  "data": null
}
```

---

## 4. Clear Recent Searches

### `DELETE /api/v1/search/recent`

Clear all recent searches for the current user.

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `workspaceId` | string (UUID) | No | If provided, clear only for this workspace. Otherwise clear all. |

**Response** (200 OK):

```json
{
  "success": true,
  "data": null
}
```

---

## 5. Bulk Re-Index (Admin)

### `POST /api/v1/admin/search/reindex`

Trigger a full re-index of all files in a workspace. Admin-only endpoint.

**Request Body**:

```json
{
  "workspaceId": "ws-uuid"
}
```

**Response** (202 Accepted):

```json
{
  "success": true,
  "data": {
    "message": "Re-index initiated",
    "workspaceId": "ws-uuid",
    "estimatedFiles": 1500
  }
}
```

---

## 6. Index Health

### `GET /api/v1/search/health`

Check search engine connectivity and index status.

**Response** (200 OK):

```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "indexName": "cms_files",
    "documentCount": 15230,
    "indexSizeBytes": 52428800
  }
}
```

**Response** (503 Service Unavailable):

```json
{
  "success": false,
  "error": "Search engine unavailable"
}
```

---

## Response Envelope

All responses follow the existing CMS API pattern:

```json
{
  "success": true|false,
  "data": { ... } | null,
  "error": "string" | null
}
```

This maps to the existing `ApiResponse.ok(data)` / `ApiResponse.error(message)` pattern in the backend.
