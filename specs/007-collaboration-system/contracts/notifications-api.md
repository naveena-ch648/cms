# Notifications API Contract

**Base Path**: `/api/v1`  
**Authentication**: Bearer JWT (all endpoints)

---

## Notifications

### GET /notifications

List notifications for the current user.

**Query Parameters**:
- `unreadOnly` (boolean, default: false) — filter unread only
- `page` (int, default: 0)
- `size` (int, default: 20, max: 50)

**Response** (200):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "notification-uuid",
        "type": "MENTION",
        "title": "Jane Doe mentioned you",
        "message": "...can you review this section?",
        "targetType": "FILE",
        "targetId": "file-uuid",
        "actor": { "id": "user-uuid", "name": "Jane Doe" },
        "isRead": false,
        "readAt": null,
        "createdAt": "2026-05-06T10:00:00Z"
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### GET /notifications/count

Get unread notification count (cached in Redis).

**Response** (200):
```json
{
  "success": true,
  "data": { "unreadCount": 7 }
}
```

---

### PATCH /notifications/{notificationId}/read

Mark a single notification as read.

**Response** (200):
```json
{
  "success": true,
  "data": { "id": "notification-uuid", "isRead": true, "readAt": "2026-05-06T11:00:00Z" }
}
```

---

### POST /notifications/read-all

Mark all unread notifications as read for the current user.

**Response** (200):
```json
{
  "success": true,
  "data": { "markedCount": 7 }
}
```

---

## Notification Types

| Type | Trigger | Title Template | Target |
|------|---------|---------------|--------|
| MENTION | User @mentioned in comment | "{actor} mentioned you in {fileName}" | FILE or FOLDER |
| TASK_ASSIGNED | Task assigned to user | "{actor} assigned you a task: {title}" | FILE |
| TASK_COMPLETED | Assigned task completed | "{actor} completed task: {title}" | FILE |
