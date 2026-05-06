# API Contract: Preview System

**Feature**: 006-file-preview-engine  
**Base Path**: `/api/v1`  
**Authentication**: Bearer JWT (all endpoints except shared link preview)

---

## Preview Endpoints

### GET /files/{fileId}/preview

Get preview information for a file. Returns presigned URLs for the preview assets.

**Request**:
```
GET /api/v1/files/{fileId}/preview
Authorization: Bearer <token>
```

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| fileId | string (UUID) | File identifier |

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| versionId | string (UUID) | null | Specific version (null = latest) |

**Response 200** (Preview ready):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileId": "uuid",
    "type": "FULL_PREVIEW",
    "status": "COMPLETED",
    "mimeType": "application/pdf",
    "pageCount": 12,
    "pages": [
      { "page": 1, "url": "https://minio.../page-1.png", "width": 794, "height": 1123 },
      { "page": 2, "url": "https://minio.../page-2.png", "width": 794, "height": 1123 }
    ],
    "directUrl": "https://minio.../presigned-url",
    "expiresAt": "2026-05-06T12:00:00Z"
  }
}
```

**Response 200** (Preview pending):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileId": "uuid",
    "type": "FULL_PREVIEW",
    "status": "PROCESSING",
    "mimeType": "application/pdf",
    "pageCount": 0,
    "pages": [],
    "directUrl": null,
    "expiresAt": null
  }
}
```

**Response 404**: File not found  
**Response 400**: File type not previewable

**Notes**:
- For images: `directUrl` is a presigned URL to the original file. No page-based rendering.
- For video: `directUrl` is a presigned streaming URL. No page-based rendering.
- For PDF/Office: `pages` array contains per-page image URLs.
- If preview hasn't been generated yet, triggers generation and returns status=PROCESSING.

---

### GET /files/{fileId}/thumbnail

Get the thumbnail URL for a file.

**Request**:
```
GET /api/v1/files/{fileId}/thumbnail
Authorization: Bearer <token>
```

**Response 200**:
```json
{
  "success": true,
  "data": {
    "url": "https://minio.../thumbnail.jpg",
    "width": 256,
    "height": 256,
    "expiresAt": "2026-05-06T12:00:00Z"
  }
}
```

**Response 404**: File not found or thumbnail not yet generated  

**Notes**:
- Returns 404 if thumbnail generation hasn't completed. Frontend shows generic icon.
- Thumbnail URLs are cached with 1-hour expiry.

---

### POST /files/{fileId}/preview/regenerate

Force regenerate preview for a file (e.g., after corruption or manual request).

**Request**:
```
POST /api/v1/files/{fileId}/preview/regenerate
Authorization: Bearer <token>
```

**Response 202**:
```json
{
  "success": true,
  "data": {
    "jobId": "uuid",
    "status": "QUEUED"
  }
}
```

---

### GET /files/{fileId}/preview/status

Check preview generation job status.

**Request**:
```
GET /api/v1/files/{fileId}/preview/status
Authorization: Bearer <token>
```

**Response 200**:
```json
{
  "success": true,
  "data": {
    "thumbnail": { "status": "COMPLETED", "generatedAt": "2026-05-06T10:00:00Z" },
    "fullPreview": { "status": "PROCESSING", "attempts": 1, "queuedAt": "2026-05-06T10:01:00Z" }
  }
}
```

---

## Comment Endpoints

### GET /files/{fileId}/comments

List comments for a file.

**Request**:
```
GET /api/v1/files/{fileId}/comments
Authorization: Bearer <token>
```

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 50 | Page size (max 100) |

**Response 200**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "content": "This looks good, approved!",
        "author": { "id": "uuid", "name": "Jane Doe", "email": "jane@example.com" },
        "parentId": null,
        "replies": [
          {
            "id": "uuid",
            "content": "Thanks for the quick review.",
            "author": { "id": "uuid", "name": "John Smith", "email": "john@example.com" },
            "parentId": "parent-uuid",
            "replies": [],
            "createdAt": "2026-05-06T10:05:00Z",
            "updatedAt": "2026-05-06T10:05:00Z"
          }
        ],
        "createdAt": "2026-05-06T10:00:00Z",
        "updatedAt": "2026-05-06T10:00:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 50
  }
}
```

---

### POST /files/{fileId}/comments

Create a new comment on a file.

**Request**:
```
POST /api/v1/files/{fileId}/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "This document needs revision on page 3.",
  "parentId": null
}
```

**Request Body**:
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| content | string | Yes | Comment text (1-5000 chars) |
| parentId | string (UUID) | No | Parent comment ID for replies |

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "content": "This document needs revision on page 3.",
    "author": { "id": "uuid", "name": "Jane Doe", "email": "jane@example.com" },
    "parentId": null,
    "replies": [],
    "createdAt": "2026-05-06T10:00:00Z",
    "updatedAt": "2026-05-06T10:00:00Z"
  }
}
```

**Response 400**: Invalid content (empty or too long), invalid parentId  
**Response 404**: File not found

---

### DELETE /files/{fileId}/comments/{commentId}

Delete a comment (only the author can delete).

**Request**:
```
DELETE /api/v1/files/{fileId}/comments/{commentId}
Authorization: Bearer <token>
```

**Response 204**: No content (success)  
**Response 403**: Not the comment author  
**Response 404**: Comment not found

---

## Worker Queue Contract

### Preview Job Message (Redis)

Published to `file:process` queue when a file is uploaded or preview regeneration is requested.

**Message format**:
```json
{
  "fileId": "file-uuid",
  "versionId": "version-uuid",
  "organizationId": "org-uuid",
  "action": "preview",
  "mimeType": "application/pdf",
  "storageBucket": "org-bucket-name",
  "storageKey": "files/original-key.pdf",
  "priority": 0,
  "_retries": 0
}
```

**Action values**:
| Action | Description |
|--------|-------------|
| `thumbnail` | Generate 256x256 thumbnail only |
| `preview` | Generate full preview (all pages) + thumbnail |

**Worker response** (via MySQL update):
- On success: Update `previews` table with storage keys, page count, dimensions
- On failure: Update `preview_jobs` table with error message, increment attempts

---

## Error Responses

All endpoints follow the standard error format:
```json
{
  "success": false,
  "error": {
    "code": "PREVIEW_NOT_READY",
    "message": "Preview is still being generated"
  }
}
```

**Error codes**:
| Code | HTTP Status | Description |
|------|-------------|-------------|
| FILE_NOT_FOUND | 404 | File UUID does not exist |
| PREVIEW_NOT_SUPPORTED | 400 | File type cannot be previewed |
| PREVIEW_NOT_READY | 202 | Preview is still generating |
| PREVIEW_FAILED | 500 | Generation failed after max retries |
| FILE_TOO_LARGE | 400 | File exceeds 100MB preview limit |
| COMMENT_NOT_FOUND | 404 | Comment UUID does not exist |
| COMMENT_FORBIDDEN | 403 | User is not the comment author |
| COMMENT_INVALID | 400 | Content empty or exceeds 5000 chars |
