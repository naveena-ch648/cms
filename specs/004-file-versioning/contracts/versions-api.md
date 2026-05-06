# API Contract: File Versions

**Feature**: 004-file-versioning  
**Base Path**: `/api/files/{fileId}/versions`

---

## POST /api/files/{fileId}/versions

Upload a new version of a file.

**Request**: `multipart/form-data`

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| file | File | Yes | The new version content |
| changeNote | string | No | Description of changes (max 500 chars) |

**Response** (201 Created):
```json
{
  "data": {
    "id": "uuid",
    "versionNumber": 2,
    "fileName": "report.pdf",
    "sizeBytes": 204800,
    "mimeType": "application/pdf",
    "checksumSha256": "abc123...",
    "changeNote": "Updated Q2 figures",
    "uploadedBy": { "id": "uuid", "name": "John Doe" },
    "createdAt": "2026-05-06T10:00:00Z"
  }
}
```

---

## GET /api/files/{fileId}/versions

List version history for a file, ordered by version number descending.

**Query Parameters**:

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size (max 100) |

**Response** (200 OK):
```json
{
  "data": {
    "content": [
      {
        "id": "uuid",
        "versionNumber": 3,
        "fileName": "report.pdf",
        "sizeBytes": 204800,
        "mimeType": "application/pdf",
        "checksumSha256": "abc123...",
        "changeNote": "Restored from version 1",
        "uploadedBy": { "id": "uuid", "name": "John Doe" },
        "createdAt": "2026-05-06T12:00:00Z",
        "isCurrent": true
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

## GET /api/files/{fileId}/versions/{versionId}

Get details of a specific version.

**Response** (200 OK):
```json
{
  "data": {
    "id": "uuid",
    "versionNumber": 1,
    "fileName": "report.pdf",
    "sizeBytes": 102400,
    "mimeType": "application/pdf",
    "checksumSha256": "def456...",
    "changeNote": "Initial upload",
    "uploadedBy": { "id": "uuid", "name": "Jane Smith" },
    "createdAt": "2026-05-06T08:00:00Z",
    "isCurrent": false
  }
}
```

---

## GET /api/files/{fileId}/versions/{versionId}/download

Download a specific version's content.

**Response** (302 Found): Redirects to a presigned MinIO URL with 1-hour expiry.

---

## POST /api/files/{fileId}/versions/{versionId}/restore

Restore a previous version (creates a new version with the same content).

**Response** (200 OK):
```json
{
  "data": {
    "id": "uuid",
    "versionNumber": 4,
    "fileName": "report.pdf",
    "sizeBytes": 102400,
    "mimeType": "application/pdf",
    "checksumSha256": "def456...",
    "changeNote": "Restored from version 1",
    "uploadedBy": { "id": "uuid", "name": "John Doe" },
    "createdAt": "2026-05-06T14:00:00Z",
    "isCurrent": true
  }
}
```

---

## GET /api/files/{fileId}/versions/compare?v1={versionId}&v2={versionId}

Compare two versions of a file.

**Response** (200 OK):
```json
{
  "data": {
    "version1": {
      "id": "uuid",
      "versionNumber": 1,
      "sizeBytes": 102400,
      "mimeType": "application/pdf",
      "checksumSha256": "def456...",
      "uploadedBy": { "id": "uuid", "name": "Jane Smith" },
      "createdAt": "2026-05-06T08:00:00Z",
      "downloadUrl": "https://minio.../presigned-url"
    },
    "version2": {
      "id": "uuid",
      "versionNumber": 2,
      "sizeBytes": 204800,
      "mimeType": "application/pdf",
      "checksumSha256": "abc123...",
      "uploadedBy": { "id": "uuid", "name": "John Doe" },
      "createdAt": "2026-05-06T10:00:00Z",
      "downloadUrl": "https://minio.../presigned-url"
    },
    "sizeDifference": 102400,
    "sameContent": false
  }
}
```
