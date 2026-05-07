# Dashboard & Notifications API Contract

**Base URL**: `/api/v1`  
**Auth**: Bearer JWT (all endpoints)  
**Response Envelope**: `ApiResponse<T>` with `{ success, data, error, meta }`

---

## Dashboard Endpoints

### GET /dashboard/summary

Returns aggregated dashboard data for the authenticated user in a single call.

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "recentFilesCount": 10,
    "unreadNotifications": 3,
    "pendingApprovals": 2,
    "storageUsedBytes": 5368709120,
    "storageMaxBytes": 10737418240,
    "storagePercentage": 50,
    "activeAlertsCount": 1
  }
}
```

**Caching**: Redis, 2-min TTL per user. Invalidated on file upload, share, notification create.

---

### GET /dashboard/recent-files

Returns recently accessed/modified files for the authenticated user.

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| limit | int | 10 | Max items (1-20) |

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "report.pdf",
      "mimeType": "application/pdf",
      "sizeBytes": 1048576,
      "workspaceId": "ws-uuid",
      "workspaceName": "Marketing",
      "folderId": "folder-uuid",
      "folderPath": "/Documents/Reports",
      "lastAccessedAt": "2026-05-06T10:30:00Z",
      "updatedAt": "2026-05-05T14:00:00Z"
    }
  ]
}
```

---

### GET /dashboard/activity

Returns activity feed for the authenticated user across their workspaces.

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size (max 50) |
| workspaceId | string | null | Filter by workspace (optional) |

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "actorName": "John Smith",
      "actionType": "FILE_UPLOADED",
      "targetType": "FILE",
      "targetId": "file-uuid",
      "targetName": "presentation.pptx",
      "workspaceName": "Marketing",
      "metadata": {},
      "createdAt": "2026-05-06T09:15:00Z"
    }
  ],
  "meta": {
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 156,
      "totalPages": 8
    }
  }
}
```

---

### GET /dashboard/shared

Returns shared items for the authenticated user (both shared-with-me and shared-by-me).

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| direction | string | "with_me" | "with_me" or "by_me" |
| limit | int | 5 | Max items (1-20) |

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "fileName": "budget.xlsx",
      "fileId": "file-uuid",
      "sharedBy": "Jane Doe",
      "sharedWith": "John Smith",
      "sharedAt": "2026-05-04T16:00:00Z",
      "expiresAt": "2026-05-11T16:00:00Z",
      "type": "LINK"
    }
  ]
}
```

---

### GET /dashboard/alerts

Returns active (non-dismissed, non-expired) alerts for the authenticated user.

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "alertType": "STORAGE_WARNING",
      "severity": "WARNING",
      "title": "Storage Almost Full",
      "message": "You're using 85% of your storage quota (8.5 GB / 10 GB)",
      "targetType": null,
      "targetId": null,
      "createdAt": "2026-05-06T08:00:00Z"
    }
  ]
}
```

---

### POST /dashboard/alerts/{alertId}/dismiss

Dismisses an alert for the authenticated user.

**Response** `204 No Content`

---

## Notification Endpoints (existing — enhanced)

### GET /notifications

Already exists. No changes needed.

**Query Parameters** (existing):
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 20 | Page size (max 50) |

**Enhancement**: Add optional `type` filter parameter.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| type | string | null | Filter by type (e.g., "APPROVAL_REQUESTED") |

---

### GET /notifications/count

Already exists. Returns `{ "unreadCount": N }`.

---

### PATCH /notifications/{notificationId}/read

Already exists. Marks single notification as read.

---

### POST /notifications/read-all

Already exists. Marks all notifications as read.

---

## Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| ALERT_NOT_FOUND | 404 | Alert UUID does not exist |
| ACCESS_DENIED | 403 | User cannot access this alert/notification |
