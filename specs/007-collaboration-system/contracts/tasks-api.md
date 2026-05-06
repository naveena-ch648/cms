# Tasks API Contract

**Base Path**: `/api/v1`  
**Authentication**: Bearer JWT (all endpoints)

---

## File Tasks

### GET /files/{fileId}/tasks

List tasks linked to a file.

**Path Parameters**: `fileId` (string, UUID)  
**Query Parameters**:
- `status` (string, optional) — filter by OPEN or DONE
- `page` (int, default: 0)
- `size` (int, default: 20, max: 50)

**Response** (200):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "task-uuid",
        "title": "Review page 3 formatting",
        "description": "The table on page 3 needs column alignment",
        "status": "OPEN",
        "fileId": "file-uuid",
        "fileName": "report.pdf",
        "creator": { "id": "user-uuid", "name": "Jane Doe" },
        "assignee": { "id": "user-uuid", "name": "John Smith" },
        "dueDate": "2026-05-10",
        "completedAt": null,
        "createdAt": "2026-05-06T10:00:00Z",
        "updatedAt": "2026-05-06T10:00:00Z"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### POST /files/{fileId}/tasks

Create a task on a file.

**Request Body**:
```json
{
  "title": "Review page 3 formatting",
  "description": "The table on page 3 needs column alignment",
  "assigneeId": "user-uuid",
  "dueDate": "2026-05-10"
}
```

**Validation**:
- `title`: required, 1–255 characters
- `description`: optional, max 2000 characters
- `assigneeId`: required, must be a workspace member
- `dueDate`: optional, ISO date format (YYYY-MM-DD)

**Response** (201): Same shape as GET list item  
**Errors**: 400 (validation), 403 (no file access), 404 (file/assignee not found)

---

### PATCH /files/{fileId}/tasks/{taskId}

Update a task (status change, reassign, edit).

**Request Body** (all fields optional):
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "assigneeId": "new-user-uuid",
  "dueDate": "2026-05-15",
  "status": "DONE"
}
```

**Response** (200): Updated task object  
**Errors**: 400 (invalid status transition), 403 (not creator/assignee/admin), 404

---

### DELETE /files/{fileId}/tasks/{taskId}

Delete a task. Only the creator or workspace admin can delete.

**Response** (204): No content  
**Errors**: 403 (not creator/admin), 404

---

## My Tasks

### GET /users/me/tasks

List all tasks assigned to the current user across all files.

**Query Parameters**:
- `status` (string, optional) — OPEN, DONE, OVERDUE
- `page` (int, default: 0)
- `size` (int, default: 20, max: 50)

**Response** (200): Same paginated structure as file tasks, includes fileId and fileName for navigation context.

**Note**: OVERDUE filter returns tasks where status=OPEN AND due_date < today.
