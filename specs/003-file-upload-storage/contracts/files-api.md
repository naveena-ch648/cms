# API Contract: File Operations

**Base Path**: `/api/files`  
**Auth**: Bearer JWT (all endpoints)

---

## GET /api/files

List files in a folder.

**Query Parameters**:
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| folderId | String (UUID) | Yes | Folder to list files from |
| status | String | No | `ACTIVE` (default), `TRASHED` |
| page | Integer | No | Page number (default 0) |
| size | Integer | No | Page size (default 20, max 100) |
| sort | String | No | `name`, `size`, `createdAt`, `updatedAt` (default `name`) |
| direction | String | No | `asc` (default), `desc` |

**Response 200**:
```json
{
  "content": [
    {
      "id": "file-uuid",
      "name": "document.pdf",
      "sizeBytes": 1048576,
      "mimeType": "application/pdf",
      "status": "ACTIVE",
      "uploadedBy": { "id": "user-uuid", "name": "John Doe" },
      "uploadCompletedAt": "2026-05-05T10:30:00Z",
      "thumbnailUrl": "https://...",
      "previewable": true
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

**Error Responses**:
- `403` — No FILE_DOWNLOAD permission on folder
- `404` — Folder not found

---

## GET /api/files/{fileId}

Get file details.

**Response 200**:
```json
{
  "id": "file-uuid",
  "name": "document.pdf",
  "originalName": "document.pdf",
  "sizeBytes": 1048576,
  "mimeType": "application/pdf",
  "folderId": "folder-uuid",
  "folderName": "Documents",
  "workspaceId": "ws-uuid",
  "status": "ACTIVE",
  "checksumSha256": "abc123...",
  "description": "Quarterly report",
  "tags": ["report", "q1"],
  "downloadCount": 5,
  "lastAccessedAt": "2026-05-05T12:00:00Z",
  "uploadedBy": { "id": "user-uuid", "name": "John Doe" },
  "uploadCompletedAt": "2026-05-05T10:30:00Z",
  "thumbnailUrl": "https://...",
  "previewable": true,
  "createdAt": "2026-05-05T10:30:00Z",
  "updatedAt": "2026-05-05T10:30:00Z"
}
```

---

## GET /api/files/{fileId}/download

Download a file. Returns a redirect to a presigned MinIO URL.

**Response 302**: Redirect to presigned URL (1 hour expiry)

**Response Headers**:
- `Location`: Presigned MinIO download URL

**Error Responses**:
- `403` — No FILE_DOWNLOAD permission
- `404` — File not found or trashed

---

## GET /api/files/{fileId}/preview

Get a presigned URL for inline preview.

**Response 200**:
```json
{
  "previewUrl": "https://minio:9000/...",
  "mimeType": "application/pdf",
  "expiresAt": "2026-05-05T11:30:00Z"
}
```

**Error Responses**:
- `400` — File type not previewable
- `403` — No FILE_DOWNLOAD permission

---

## PATCH /api/files/{fileId}

Update file metadata (rename, description, tags).

**Request**:
```json
{
  "name": "new-name.pdf",
  "description": "Updated description",
  "tags": ["updated", "tag"]
}
```

**Response 200**: Updated file object (same shape as GET)

**Error Responses**:
- `403` — No FILE_MANAGE permission
- `409` — Name conflicts with existing file in folder

---

## POST /api/files/{fileId}/move

Move a file to a different folder.

**Request**:
```json
{
  "targetFolderId": "target-folder-uuid",
  "onDuplicate": "rename"
}
```

**Response 200**: Updated file object with new folder info

**Error Responses**:
- `403` — No FILE_MANAGE on source folder + FILE_UPLOAD on target folder
- `404` — Target folder not found
- `409` — Duplicate name in target (when `onDuplicate=error`)

---

## POST /api/files/{fileId}/copy

Copy a file to a folder (creates an independent copy).

**Request**:
```json
{
  "targetFolderId": "target-folder-uuid",
  "onDuplicate": "rename"
}
```

**Response 201**: New file object (the copy)

---

## DELETE /api/files/{fileId}

Soft-delete (trash) a file.

**Response 200**:
```json
{
  "id": "file-uuid",
  "status": "TRASHED",
  "trashedAt": "2026-05-05T14:00:00Z",
  "permanentDeleteAt": "2026-06-04T14:00:00Z"
}
```

**Error Responses**:
- `403` — No FILE_MANAGE permission

---

## POST /api/files/{fileId}/restore

Restore a trashed file.

**Response 200**: Restored file object with status ACTIVE

**Error Responses**:
- `400` — File is not in TRASHED status
- `403` — No FILE_TRASH_RESTORE permission

---

## DELETE /api/files/{fileId}/permanent

Permanently delete a trashed file (removes from MinIO + MySQL).

**Response 204**: No content

**Error Responses**:
- `400` — File is not in TRASHED status
- `403` — No FILE_TRASH_DELETE permission

---

## GET /api/files/trash

List trashed files for the current user's accessible folders.

**Query Parameters**: Same pagination as GET /api/files

**Response 200**: Paginated list of trashed files with `trashedAt` and `permanentDeleteAt` fields

---

## GET /api/storage/quota

Get storage quota info for current organization.

**Response 200**:
```json
{
  "maxStorageBytes": 10737418240,
  "usedStorageBytes": 5368709120,
  "usedPercentage": 50.0,
  "maxFileSizeBytes": 10737418240,
  "trashRetentionDays": 30,
  "allowedExtensions": null,
  "blockedExtensions": [".exe", ".bat"]
}
```
