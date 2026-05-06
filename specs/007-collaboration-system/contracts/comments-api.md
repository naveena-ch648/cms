# Comments API Contract

**Base Path**: `/api/v1`  
**Authentication**: Bearer JWT (all endpoints)

---

## File Comments

### GET /files/{fileId}/comments

List comments on a file (top-level only, with nested replies).

**Path Parameters**: `fileId` (string, UUID)  
**Query Parameters**:
- `page` (int, default: 0) — page number
- `size` (int, default: 50, max: 100) — page size

**Response** (200):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "content": "Great work on this @[user-uuid]!",
        "author": { "id": "uuid", "name": "Jane Doe", "email": "jane@co.com" },
        "parentId": null,
        "replies": [
          {
            "id": "uuid",
            "content": "Thanks!",
            "author": { "id": "uuid", "name": "John Smith" },
            "parentId": "parent-uuid",
            "replies": [],
            "mentions": [],
            "createdAt": "2026-05-06T10:30:00Z",
            "updatedAt": "2026-05-06T10:30:00Z"
          }
        ],
        "mentions": [
          { "userId": "user-uuid", "name": "Bob" }
        ],
        "createdAt": "2026-05-06T10:00:00Z",
        "updatedAt": "2026-05-06T10:00:00Z"
      }
    ],
    "totalElements": 42,
    "totalPages": 1,
    "number": 0,
    "size": 50
  }
}
```

---

### POST /files/{fileId}/comments

Create a comment on a file. Mentions are auto-detected from content.

**Path Parameters**: `fileId` (string, UUID)  
**Request Body**:
```json
{
  "content": "Looks good @[user-uuid], can you review?",
  "parentId": "optional-parent-comment-uuid"
}
```

**Validation**:
- `content`: required, 1–5000 characters
- `parentId`: optional, must reference a top-level comment on the same file (no reply-to-reply)

**Response** (201):
```json
{
  "success": true,
  "data": {
    "id": "new-uuid",
    "content": "Looks good @[user-uuid], can you review?",
    "author": { "id": "uuid", "name": "Jane Doe" },
    "parentId": null,
    "replies": [],
    "mentions": [{ "userId": "user-uuid", "name": "Bob" }],
    "createdAt": "2026-05-06T11:00:00Z",
    "updatedAt": "2026-05-06T11:00:00Z"
  }
}
```

**Errors**: 400 (invalid content/parent), 403 (no file access), 404 (file not found)

---

### DELETE /files/{fileId}/comments/{commentId}

Delete a comment. Only the author or workspace admin can delete.

**Response** (204): No content  
**Errors**: 403 (not author/admin), 404 (comment not found)

---

## Folder Comments

### GET /folders/{folderId}/comments

Same structure as file comments but scoped to a folder.

### POST /folders/{folderId}/comments

Same request/response as file comments but targets a folder.

### DELETE /folders/{folderId}/comments/{commentId}

Same as file comment deletion.

---

## Comment Count

### GET /files/{fileId}/comments/count

**Response** (200):
```json
{
  "success": true,
  "data": { "count": 12 }
}
```

### GET /folders/{folderId}/comments/count

Same structure for folder comment count.

---

## Mention Autocomplete

### GET /workspaces/{workspaceId}/members/search?q={query}

Search workspace members for @mention autocomplete.

**Query Parameters**: `q` (string, min 1 char) — search term (matches first/last name, email)

**Response** (200):
```json
{
  "success": true,
  "data": [
    { "id": "user-uuid", "name": "Jane Doe", "email": "jane@co.com" },
    { "id": "user-uuid", "name": "John Smith", "email": "john@co.com" }
  ]
}
```

Max 10 results returned.
