# API Contract: Webhooks

**Feature**: 015-integrations-sync  
**Base Path**: `/api/v1/webhooks`

## Authentication

All endpoints require Bearer token (JWT). Organization scope derived from token.

---

## Endpoints

### POST /api/v1/webhooks

Register a new webhook.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Request Body**:
```json
{
  "name": "CI/CD Notification",
  "url": "https://example.com/webhook/cms-events",
  "secret": "my-secret-key-at-least-16-chars",
  "eventTypes": ["file.uploaded", "file.deleted", "workflow.status_changed"]
}
```

**Validation**:
- `name`: required, 1-255 characters
- `url`: required, valid HTTPS URL (HTTP allowed in dev mode only), max 2048 chars
- `secret`: optional, minimum 16 characters if provided
- `eventTypes`: required, non-empty array from allowed event types

**Response** `201 Created`:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "CI/CD Notification",
    "url": "https://example.com/webhook/cms-events",
    "eventTypes": ["file.uploaded", "file.deleted", "workflow.status_changed"],
    "status": "ACTIVE",
    "createdAt": "2026-05-06T12:00:00Z"
  }
}
```

Note: `secret` is never returned in responses.

---

### GET /api/v1/webhooks

List all webhooks for the organization.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Query Parameters**:
- `status` (string, optional): Filter by ACTIVE/DISABLED
- `page` (int, default 0)
- `size` (int, default 20)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "CI/CD Notification",
      "url": "https://example.com/webhook/cms-events",
      "eventTypes": ["file.uploaded", "file.deleted"],
      "status": "ACTIVE",
      "consecutiveFailures": 0,
      "createdBy": {
        "id": "user-uuid",
        "name": "Admin User"
      },
      "createdAt": "2026-05-06T12:00:00Z",
      "updatedAt": "2026-05-06T12:00:00Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 3 }
}
```

---

### GET /api/v1/webhooks/{webhookId}

Get webhook details.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Response** `200 OK`: Single webhook object (same shape as list item).

---

### PUT /api/v1/webhooks/{webhookId}

Update webhook configuration.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Request Body** (all fields optional):
```json
{
  "name": "Updated Name",
  "url": "https://new-url.com/webhook",
  "secret": "new-secret-16-chars-min",
  "eventTypes": ["file.uploaded"],
  "status": "ACTIVE"
}
```

**Response** `200 OK`: Updated webhook object.

---

### DELETE /api/v1/webhooks/{webhookId}

Delete a webhook and all its delivery history.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Response** `204 No Content`

---

### POST /api/v1/webhooks/{webhookId}/test

Send a test event to the webhook URL.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Request Body** (optional):
```json
{
  "eventType": "file.uploaded"
}
```

Default event type: `webhook.test`

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "delivered": true,
    "responseStatus": 200,
    "responseTimeMs": 145,
    "responseBody": "OK"
  }
}
```

If delivery fails:
```json
{
  "success": true,
  "data": {
    "delivered": false,
    "responseStatus": null,
    "responseTimeMs": null,
    "error": "Connection refused"
  }
}
```

---

### GET /api/v1/webhooks/{webhookId}/deliveries

Get delivery history for a webhook.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Query Parameters**:
- `status` (string, optional): PENDING, SUCCESS, FAILED, RETRYING
- `eventType` (string, optional): Filter by event type
- `page` (int, default 0)
- `size` (int, default 50)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "eventType": "file.uploaded",
      "eventId": "event-uuid",
      "status": "SUCCESS",
      "responseStatus": 200,
      "responseTimeMs": 89,
      "attemptNumber": 1,
      "deliveredAt": "2026-05-06T12:01:00Z",
      "createdAt": "2026-05-06T12:00:59Z"
    }
  ],
  "meta": { "page": 0, "size": 50, "totalElements": 150 }
}
```

---

### POST /api/v1/webhooks/{webhookId}/deliveries/{deliveryId}/retry

Manually retry a failed delivery.

**Permission**: `ADMIN` or `manage-webhooks` permission

**Response** `202 Accepted`:
```json
{
  "success": true,
  "data": {
    "status": "RETRYING",
    "message": "Retry queued"
  }
}
```

---

## Webhook Payload Format

All webhook deliveries use the following envelope:

```json
{
  "id": "event-uuid",
  "type": "file.uploaded",
  "timestamp": "2026-05-06T12:00:59Z",
  "organizationId": "org-uuid",
  "data": {
    // Event-specific payload
  }
}
```

### Headers Sent with Delivery

| Header | Description |
|--------|-------------|
| `Content-Type` | `application/json` |
| `X-CMS-Event` | Event type (e.g., `file.uploaded`) |
| `X-CMS-Delivery` | Delivery UUID for deduplication |
| `X-CMS-Signature` | `sha256=<HMAC-SHA256 hex digest>` (if secret configured) |
| `X-CMS-Timestamp` | Unix timestamp of event |
| `User-Agent` | `CMS-Webhook/1.0` |

### Signature Verification

Recipients verify the signature:
```
expected = HMAC-SHA256(webhook_secret, raw_request_body)
actual = X-CMS-Signature header value (strip "sha256=" prefix)
compare using constant-time comparison
```

---

## Event Payloads

### file.uploaded
```json
{
  "file": {
    "id": "file-uuid",
    "name": "document.pdf",
    "mimeType": "application/pdf",
    "size": 1048576,
    "folderId": "folder-uuid",
    "uploadedBy": "user-uuid"
  }
}
```

### file.deleted
```json
{
  "file": {
    "id": "file-uuid",
    "name": "document.pdf",
    "deletedBy": "user-uuid",
    "permanent": false
  }
}
```

### folder.created
```json
{
  "folder": {
    "id": "folder-uuid",
    "name": "New Folder",
    "parentId": "parent-folder-uuid",
    "createdBy": "user-uuid"
  }
}
```

### workflow.status_changed
```json
{
  "file": { "id": "file-uuid", "name": "report.docx" },
  "workflow": {
    "id": "workflow-uuid",
    "previousStatus": "PENDING_REVIEW",
    "newStatus": "APPROVED",
    "changedBy": "user-uuid"
  }
}
```
