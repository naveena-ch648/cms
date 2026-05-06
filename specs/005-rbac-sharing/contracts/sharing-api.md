# API Contract: Sharing

**Base path**: `/api/v1/share-links` (authenticated) and `/api/share/{token}` (public)

## Authenticated Endpoints

### POST /api/v1/share-links

Create a new share link.

**Auth**: Required (Editor or Admin permission on target resource)

**Request body**:
```json
{
  "resourceType": "FILE | FOLDER",
  "fileUuid": "string | null",
  "folderUuid": "string | null",
  "password": "string | null",
  "expiresAt": "2026-05-13T00:00:00Z | null",
  "allowDownload": true,
  "watermarkEnabled": false
}
```

**Validation**:
- Exactly one of fileUuid/folderUuid required, matching resourceType
- expiresAt must be in the future (if provided)
- Creator must have Editor+ permission on the resource

**Response 201**:
```json
{
  "data": {
    "uuid": "string",
    "token": "string",
    "url": "https://host/share/{token}",
    "resourceType": "FILE",
    "resourceName": "Proposal.pdf",
    "hasPassword": true,
    "expiresAt": "2026-05-13T00:00:00Z",
    "allowDownload": true,
    "watermarkEnabled": false,
    "status": "ACTIVE",
    "viewCount": 0,
    "createdAt": "2026-05-06T00:00:00Z"
  }
}
```

### GET /api/v1/share-links

List share links created by the current user (or all links for workspace admins).

**Auth**: Required

**Query params**:
- `workspaceUuid` (required): Filter by workspace
- `status` (optional): ACTIVE | REVOKED | EXPIRED
- `page`, `size` (optional): Pagination

**Response 200**:
```json
{
  "data": [ /* array of share link objects */ ],
  "pagination": { "page": 0, "size": 20, "totalElements": 5, "totalPages": 1 }
}
```

### PATCH /api/v1/share-links/{uuid}

Update share link settings.

**Auth**: Required (link creator or workspace admin)

**Request body** (all optional):
```json
{
  "password": "string | null",
  "expiresAt": "2026-05-20T00:00:00Z | null",
  "allowDownload": false,
  "watermarkEnabled": true
}
```

**Response 200**: Updated share link object

### DELETE /api/v1/share-links/{uuid}

Revoke a share link immediately.

**Auth**: Required (link creator or workspace admin)

**Response 204**: No content

### GET /api/v1/share-links/{uuid}/accesses

Get access log for a share link.

**Auth**: Required (link creator or workspace admin)

**Response 200**:
```json
{
  "data": [
    {
      "accessedAt": "2026-05-06T10:30:00Z",
      "ipAddress": "192.168.1.x",
      "userAgent": "Mozilla/5.0..."
    }
  ],
  "pagination": { "page": 0, "size": 50, "totalElements": 12, "totalPages": 1 }
}
```

---

## Public Endpoints (No Auth Required)

### GET /api/share/{token}

Access a shared resource via token.

**Response 200** (valid link, no password):
```json
{
  "data": {
    "resourceType": "FILE",
    "resourceName": "Proposal.pdf",
    "mimeType": "application/pdf",
    "size": 1048576,
    "allowDownload": true,
    "watermarkEnabled": false,
    "previewUrl": "string | null",
    "downloadUrl": "string | null",
    "requiresPassword": false
  }
}
```

**Response 200** (requires password):
```json
{
  "data": {
    "requiresPassword": true
  }
}
```

### POST /api/share/{token}/verify

Verify password for a password-protected link.

**Request body**:
```json
{
  "password": "string"
}
```

**Response 200** (correct password): Full resource data (same as GET without password)

**Response 401**: Invalid password

### GET /api/share/{token}/download

Download the file (if allowDownload=true).

**Query params**:
- `session` (required if password-protected): Session token from verify

**Response**:
- 200 with file stream (Content-Disposition: attachment)
- Or redirect to pre-signed MinIO URL

---

## Error Responses

| Status | Code | Condition |
|--------|------|-----------|
| 404 | LINK_NOT_FOUND | Token doesn't exist or link revoked |
| 410 | LINK_EXPIRED | Link has passed its expiration date |
| 401 | PASSWORD_REQUIRED | Link requires password, none provided |
| 401 | INVALID_PASSWORD | Wrong password |
| 403 | DOWNLOAD_DISABLED | Download not allowed on this link |
| 403 | PERMISSION_DENIED | User lacks Editor+ permission to create/manage link |
