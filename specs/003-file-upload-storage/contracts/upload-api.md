# API Contract: File Upload

**Base Path**: `/api/files`  
**Auth**: Bearer JWT (all endpoints)  
**Permission**: Folder-level permissions checked

---

## POST /api/files/upload

Upload a single file (< 100 MB) directly to a folder.

**Content-Type**: `multipart/form-data`

**Request**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| file | File | Yes | The file to upload |
| folderId | String (UUID) | Yes | Target folder UUID |
| description | String | No | File description (max 1000 chars) |
| tags | String (JSON array) | No | Tags as JSON array string |
| onDuplicate | String | No | `rename` (default), `replace`, `error` |

**Response 201**:
```json
{
  "id": "uuid",
  "name": "document.pdf",
  "originalName": "document.pdf",
  "sizeBytes": 1048576,
  "mimeType": "application/pdf",
  "folderId": "folder-uuid",
  "uploadedBy": { "id": "user-uuid", "name": "John Doe" },
  "uploadCompletedAt": "2026-05-05T10:30:00Z",
  "description": null,
  "tags": [],
  "status": "ACTIVE",
  "createdAt": "2026-05-05T10:30:00Z"
}
```

**Error Responses**:
- `400` — Invalid file type, missing folder, file too large
- `403` — No FILE_UPLOAD permission on folder
- `409` — Duplicate filename (when `onDuplicate=error`)
- `413` — File exceeds org max file size
- `507` — Organization storage quota exceeded

---

## POST /api/files/upload/initiate

Initiate a chunked upload session for files ≥ 100 MB.

**Content-Type**: `application/json`

**Request**:
```json
{
  "fileName": "large-video.mp4",
  "fileSize": 2147483648,
  "mimeType": "video/mp4",
  "folderId": "folder-uuid",
  "chunkSize": 5242880,
  "description": null,
  "tags": [],
  "onDuplicate": "rename"
}
```

**Response 201**:
```json
{
  "sessionId": "upload-session-uuid",
  "chunkSize": 5242880,
  "totalChunks": 410,
  "expiresAt": "2026-05-06T10:30:00Z"
}
```

**Error Responses**:
- `400` — Invalid parameters
- `403` — No FILE_UPLOAD permission
- `413` — File exceeds max file size limit
- `507` — Would exceed storage quota

---

## PUT /api/files/upload/{sessionId}/chunks/{chunkNumber}

Upload a single chunk of a chunked upload.

**Content-Type**: `application/octet-stream`

**Path Parameters**:
- `sessionId`: Upload session UUID
- `chunkNumber`: 0-based chunk index

**Headers**:
- `Content-Length`: Chunk size in bytes
- `Content-MD5` (optional): MD5 hash of chunk for integrity

**Response 200**:
```json
{
  "chunkNumber": 0,
  "received": true,
  "completedChunks": 1,
  "totalChunks": 410
}
```

**Error Responses**:
- `400` — Invalid chunk number, chunk too large
- `404` — Session not found or expired
- `409` — Chunk already uploaded

---

## POST /api/files/upload/{sessionId}/complete

Finalize a chunked upload after all chunks are uploaded.

**Content-Type**: `application/json`

**Request** (optional body):
```json
{
  "checksumSha256": "abc123..."
}
```

**Response 201**:
```json
{
  "id": "file-uuid",
  "name": "large-video.mp4",
  "originalName": "large-video.mp4",
  "sizeBytes": 2147483648,
  "mimeType": "video/mp4",
  "folderId": "folder-uuid",
  "uploadedBy": { "id": "user-uuid", "name": "John Doe" },
  "uploadCompletedAt": "2026-05-05T11:00:00Z",
  "status": "ACTIVE",
  "createdAt": "2026-05-05T10:30:00Z"
}
```

**Error Responses**:
- `400` — Not all chunks uploaded, checksum mismatch
- `404` — Session not found or expired

---

## DELETE /api/files/upload/{sessionId}

Cancel/abort an in-progress chunked upload.

**Response 204**: No content

**Error Responses**:
- `404` — Session not found

---

## GET /api/files/upload/{sessionId}/status

Check status of a chunked upload session.

**Response 200**:
```json
{
  "sessionId": "upload-session-uuid",
  "fileName": "large-video.mp4",
  "totalChunks": 410,
  "completedChunks": 150,
  "percentComplete": 36.6,
  "status": "IN_PROGRESS",
  "expiresAt": "2026-05-06T10:30:00Z",
  "lastActivityAt": "2026-05-05T10:45:00Z"
}
```
