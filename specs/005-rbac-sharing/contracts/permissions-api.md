# API Contract: Permissions

**Base path**: `/api/v1/folders/{folderUuid}/permissions`

## Endpoints

### GET /api/v1/folders/{folderUuid}/permissions

List all explicit permissions on a folder.

**Auth**: Required (Admin permission on folder)

**Response 200**:
```json
{
  "data": [
    {
      "id": "uuid",
      "folderUuid": "string",
      "userUuid": "string | null",
      "userName": "string | null",
      "groupUuid": "string | null",
      "groupName": "string | null",
      "role": "Viewer | Editor | Admin",
      "isOverride": false,
      "createdAt": "2026-05-06T00:00:00Z"
    }
  ]
}
```

### POST /api/v1/folders/{folderUuid}/permissions

Assign or update a permission on a folder.

**Auth**: Required (Admin permission on folder)

**Request body**:
```json
{
  "userUuid": "string | null",
  "groupUuid": "string | null",
  "roleUuid": "string",
  "isOverride": false
}
```

**Validation**:
- Exactly one of userUuid/groupUuid required
- roleUuid must reference valid role

**Response 201**:
```json
{
  "data": {
    "id": "uuid",
    "folderUuid": "string",
    "userUuid": "string | null",
    "groupUuid": "string | null",
    "role": "Viewer",
    "isOverride": false,
    "createdAt": "2026-05-06T00:00:00Z"
  }
}
```

### DELETE /api/v1/folders/{folderUuid}/permissions/{permissionId}

Remove a permission assignment.

**Auth**: Required (Admin permission on folder)

**Response 204**: No content

### GET /api/v1/folders/{folderUuid}/effective-permission

Get the calling user's effective permission on a folder (resolved via inheritance).

**Auth**: Required (any authenticated user)

**Response 200**:
```json
{
  "data": {
    "folderUuid": "string",
    "effectiveRole": "Viewer | Editor | Admin | null",
    "source": "DIRECT | INHERITED | GROUP",
    "sourceFolderUuid": "string"
  }
}
```

---

## Permission Filtering (Middleware)

The following existing endpoints are filtered by the `PermissionInterceptor`:

- `GET /api/v1/workspaces/{id}/folders` — returns only folders the user can access
- `GET /api/v1/folders/{id}/children` — returns only accessible child folders
- `GET /api/v1/folders/{id}/files` — returns only files in accessible folders

Unauthorized folders/files are silently excluded from results (not 403).

## Error Responses

| Status | Code | Condition |
|--------|------|-----------|
| 403 | PERMISSION_DENIED | User lacks Admin role on folder for permission management |
| 404 | FOLDER_NOT_FOUND | Folder UUID does not exist |
| 400 | INVALID_PERMISSION | Both or neither user/group specified |
| 409 | PERMISSION_EXISTS | Duplicate assignment (same user/group on folder) |
