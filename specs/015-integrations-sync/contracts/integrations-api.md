# API Contract: Integrations

**Feature**: 015-integrations-sync  
**Base Path**: `/api/v1/integrations`

## Authentication

All endpoints require Bearer token (JWT). Organization scope derived from token.

---

## Endpoints

### POST /api/v1/integrations/google-drive/connect

Initiate Google Drive OAuth2 authorization flow.

**Permission**: Authenticated user (any role)

**Request Body**: None (or optional redirect URI)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "authorizationUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&scope=...&state=..."
  }
}
```

---

### GET /api/v1/integrations/google-drive/callback

OAuth2 callback handler. Exchanges authorization code for tokens.

**Permission**: Authenticated user (state token validated)

**Query Parameters**:
- `code` (string, required): Authorization code from Google
- `state` (string, required): Anti-CSRF state token

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "connectionId": "uuid",
    "provider": "GOOGLE_DRIVE",
    "providerAccountId": "user@gmail.com",
    "status": "ACTIVE",
    "connectedAt": "2026-05-06T12:00:00Z"
  }
}
```

**Errors**:
- `400`: Invalid or expired authorization code
- `409`: Connection already exists for this provider+user

---

### GET /api/v1/integrations/connections

List user's integration connections.

**Permission**: Authenticated user

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "provider": "GOOGLE_DRIVE",
      "providerAccountId": "user@gmail.com",
      "status": "ACTIVE",
      "connectedAt": "2026-05-06T12:00:00Z",
      "lastUsedAt": "2026-05-06T14:30:00Z"
    }
  ]
}
```

---

### DELETE /api/v1/integrations/connections/{connectionId}

Disconnect (revoke) an integration.

**Permission**: Authenticated user (owner of connection)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": { "status": "REVOKED" }
}
```

---

### GET /api/v1/integrations/google-drive/browse

Browse Google Drive files and folders.

**Permission**: Authenticated user with active Google Drive connection

**Query Parameters**:
- `folderId` (string, optional): Drive folder ID to list (default: root)
- `query` (string, optional): Search query within Drive
- `pageToken` (string, optional): Pagination token

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "drive-file-id",
        "name": "Document.pdf",
        "mimeType": "application/pdf",
        "size": 1048576,
        "modifiedTime": "2026-05-01T10:00:00Z",
        "isFolder": false,
        "iconLink": "https://..."
      }
    ],
    "nextPageToken": "token-or-null"
  }
}
```

**Errors**:
- `401`: Google Drive token expired — re-authentication required
- `404`: Folder not found in Drive

---

### POST /api/v1/integrations/google-drive/import

Import files from Google Drive into CMS.

**Permission**: `upload-files` permission

**Request Body**:
```json
{
  "connectionId": "uuid",
  "driveFileIds": ["file-id-1", "file-id-2"],
  "targetFolderId": "cms-folder-uuid",
  "preserveStructure": true
}
```

**Response** `202 Accepted`:
```json
{
  "success": true,
  "data": {
    "jobId": "uuid",
    "status": "QUEUED",
    "totalItems": 5,
    "message": "Import job queued"
  }
}
```

---

### POST /api/v1/integrations/google-drive/export

Export CMS files to Google Drive.

**Permission**: `download-files` permission

**Request Body**:
```json
{
  "connectionId": "uuid",
  "fileIds": ["cms-file-uuid-1", "cms-file-uuid-2"],
  "targetDriveFolderId": "drive-folder-id",
  "conflictStrategy": "SKIP|REPLACE|RENAME"
}
```

**Response** `202 Accepted`:
```json
{
  "success": true,
  "data": {
    "jobId": "uuid",
    "status": "QUEUED",
    "totalItems": 2,
    "message": "Export job queued"
  }
}
```

---

### GET /api/v1/integrations/jobs/{jobId}

Get import/export job status.

**Permission**: Authenticated user (job owner)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "type": "IMPORT",
    "status": "COMPLETED",
    "totalItems": 5,
    "completedItems": 5,
    "failedItems": 0,
    "errors": [],
    "startedAt": "2026-05-06T12:00:00Z",
    "completedAt": "2026-05-06T12:02:30Z"
  }
}
```

---

### POST /api/v1/integrations/sync-links

Create a new sync link between CMS folder and Drive folder.

**Permission**: `manage-files` permission

**Request Body**:
```json
{
  "connectionId": "uuid",
  "folderId": "cms-folder-uuid",
  "externalFolderId": "drive-folder-id",
  "externalFolderName": "My Drive Folder",
  "direction": "BIDIRECTIONAL",
  "syncIntervalMinutes": 15
}
```

**Validation**:
- `syncIntervalMinutes` >= 5
- `folderId` must not already have an active sync link

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "folderId": "cms-folder-uuid",
    "externalFolderName": "My Drive Folder",
    "direction": "BIDIRECTIONAL",
    "syncIntervalMinutes": 15,
    "status": "ACTIVE",
    "nextSyncAt": "2026-05-06T12:15:00Z"
  }
}
```

---

### GET /api/v1/integrations/sync-links

List sync links for current user/organization.

**Permission**: Authenticated user

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "folderId": "cms-folder-uuid",
      "folderName": "Shared Documents",
      "externalFolderName": "My Drive Folder",
      "provider": "GOOGLE_DRIVE",
      "direction": "BIDIRECTIONAL",
      "syncIntervalMinutes": 15,
      "status": "ACTIVE",
      "lastSyncAt": "2026-05-06T12:00:00Z",
      "nextSyncAt": "2026-05-06T12:15:00Z"
    }
  ]
}
```

---

### PUT /api/v1/integrations/sync-links/{syncLinkId}

Update sync link configuration.

**Permission**: `manage-files` permission

**Request Body**:
```json
{
  "direction": "IMPORT_ONLY",
  "syncIntervalMinutes": 30,
  "status": "PAUSED"
}
```

**Response** `200 OK`: Updated sync link object.

---

### DELETE /api/v1/integrations/sync-links/{syncLinkId}

Remove a sync link.

**Permission**: `manage-files` permission

**Response** `204 No Content`

---

### GET /api/v1/integrations/sync-links/{syncLinkId}/jobs

List sync job history for a sync link.

**Permission**: Authenticated user

**Query Parameters**:
- `page` (int, default 0)
- `size` (int, default 20)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "status": "COMPLETED",
      "direction": "BIDIRECTIONAL",
      "itemsSynced": 12,
      "itemsFailed": 0,
      "itemsConflicted": 1,
      "bytesTransferred": 5242880,
      "startedAt": "2026-05-06T12:00:00Z",
      "completedAt": "2026-05-06T12:01:45Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 45 }
}
```

---

## Event Types for Webhooks

These events trigger webhook delivery when matched:

| Event Type | Trigger |
|-----------|---------|
| `file.uploaded` | File successfully uploaded or imported |
| `file.deleted` | File moved to trash or permanently deleted |
| `file.moved` | File moved to a different folder |
| `file.version_created` | New version uploaded for existing file |
| `folder.created` | New folder created |
| `folder.deleted` | Folder deleted |
| `workflow.status_changed` | Document workflow state transition |
| `user.created` | New user account created |
| `user.deactivated` | User account deactivated |
