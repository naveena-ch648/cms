# Activity API Contract

**Base Path**: `/api/v1`  
**Authentication**: Bearer JWT (all endpoints)

---

## File Activity Timeline

### GET /files/{fileId}/activity

List activity events for a file in reverse-chronological order.

**Path Parameters**: `fileId` (string, UUID)  
**Query Parameters**:
- `type` (string, optional) — filter by event type (COMMENT, TASK, VERSION, SHARE, UPLOAD)
- `page` (int, default: 0)
- `size` (int, default: 50, max: 100)

**Response** (200):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1234,
        "eventType": "COMMENT_CREATED",
        "category": "COMMENT",
        "actor": { "id": "user-uuid", "name": "Jane Doe" },
        "description": "Jane Doe posted a comment",
        "details": { "commentPreview": "Looks great!" },
        "createdAt": "2026-05-06T10:30:00Z"
      },
      {
        "id": 1233,
        "eventType": "FILE_UPLOADED",
        "category": "UPLOAD",
        "actor": { "id": "user-uuid", "name": "John Smith" },
        "description": "John Smith uploaded the file",
        "details": null,
        "createdAt": "2026-05-06T09:00:00Z"
      }
    ],
    "totalElements": 28,
    "totalPages": 1,
    "number": 0,
    "size": 50
  }
}
```

---

## Folder Activity Timeline

### GET /folders/{folderId}/activity

Same structure as file activity but scoped to folder-level events.

---

## Event Categories & Types

| Category | Event Types | Description |
|----------|-------------|-------------|
| UPLOAD | FILE_UPLOADED | File created/uploaded |
| VERSION | FILE_VERSION_CREATED | New version uploaded |
| COMMENT | COMMENT_CREATED, COMMENT_DELETED | Discussion activity |
| TASK | TASK_CREATED, TASK_COMPLETED, TASK_REOPENED | Task lifecycle |
| SHARE | FOLDER_PERMISSION_ASSIGNED, SHARED_LINK_CREATED | Access changes |

---

## Notes

- Activity data is sourced from the existing `audit_events` table — no separate storage.
- The `category` field is derived from `eventType` for frontend filtering.
- `details` is a JSON object with event-specific metadata (comment preview, version number, etc.).
- Events are read-only — there is no POST/PUT/DELETE on activity.
