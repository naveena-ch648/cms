# API Contract: Tags

**Base Path**: `/api/v1/files/{fileId}/tags`  
**Auth**: Bearer JWT required  
**Tenant Isolation**: Workspace-scoped via file ownership

---

## GET /api/v1/files/{fileId}/tags

Get all tags assigned to a file.

**Access**: Any user with file read access

**Response 200**:
```json
{
  "success": true,
  "data": [
    { "name": "confidential", "createdAt": "2026-05-06T12:00:00Z", "createdBy": "user-name" },
    { "name": "reviewed", "createdAt": "2026-05-06T13:00:00Z", "createdBy": "other-user" }
  ]
}
```

---

## POST /api/v1/files/{fileId}/tags

Add one or more tags to a file.

**Access**: Any user with file write access

**Request Body**:
```json
{
  "tags": ["confidential", "priority"]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| tags | string[] | Yes | 1–20 tags, each 1–50 chars |

**Response 200**:
```json
{
  "success": true,
  "data": {
    "added": 2,
    "tags": [
      { "name": "confidential", "createdAt": "2026-05-06T12:00:00Z", "createdBy": "user-name" },
      { "name": "priority", "createdAt": "2026-05-06T14:00:00Z", "createdBy": "user-name" }
    ]
  }
}
```

**Error 400**: Tag exceeds 50 chars, empty tag name, invalid characters  
**Error 403**: No write access to file  
**Error 409**: Tag already exists on file (ignored, not an error — idempotent)  
**Error 422**: File already has 20 tags (limit reached)

---

## DELETE /api/v1/files/{fileId}/tags/{tagName}

Remove a tag from a file.

**Access**: Any user with file write access

**Response 204**: No content  
**Error 403**: No write access  
**Error 404**: Tag not found on file

---

## GET /api/v1/workspaces/{workspaceId}/tags/autocomplete

Get tag suggestions for autocomplete.

**Access**: Any workspace member

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| prefix | string | "" | Prefix to match (case-insensitive) |
| limit | integer | 10 | Max suggestions (1–50) |

**Response 200**:
```json
{
  "success": true,
  "data": ["urgent", "under-review", "updated"]
}
```

---

## POST /api/v1/files/bulk-tags

Add tags to multiple files at once.

**Access**: User must have write access to all specified files

**Request Body**:
```json
{
  "fileIds": ["file-uuid-1", "file-uuid-2", "file-uuid-3"],
  "tags": ["reviewed", "q2-2026"]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| fileIds | string[] | Yes | 1–100 file UUIDs |
| tags | string[] | Yes | 1–20 tags, each 1–50 chars |

**Response 200**:
```json
{
  "success": true,
  "data": {
    "totalFiles": 3,
    "updated": 3,
    "failed": 0,
    "errors": []
  }
}
```

**Error 400**: More than 100 files, invalid tags  
**Error 403**: No write access to one or more files

---

## GET /api/v1/workspaces/{workspaceId}/files?metadata and tag filters

Extends the existing file list API with metadata and tag filter parameters.

**Additional Query Parameters**:
| Param | Type | Description |
|-------|------|-------------|
| tag | string (repeatable) | Filter files having this tag (AND logic for multiple) |
| meta.{fieldName} | string | Filter by metadata value (exact match for dropdown/text) |
| meta.{fieldName}.gte | string | Filter by metadata >= value (number/date) |
| meta.{fieldName}.lte | string | Filter by metadata <= value (number/date) |

**Example**: `GET /api/v1/workspaces/{wsId}/files?tag=urgent&tag=priority&meta.Department=Legal&meta.DueDate.lte=2026-06-30`

**Response**: Same paginated file list response, filtered by the specified criteria.
