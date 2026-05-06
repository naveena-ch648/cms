# API Contracts: Workspace Folder System

**Base URL**: `/api/v1/workspaces/{workspaceId}/folders`  
**Authentication**: JWT Bearer token (existing)  
**Tenant Isolation**: Organization ID extracted from JWT claims via TenantContext  
**Response Envelope**: `ApiResponse<T>` with `{ success, data, error, meta }`

---

## Folder CRUD

### POST `/api/v1/workspaces/{workspaceId}/folders`

Create a new folder.

**Permission**: `manage-folders` or Editor+ role on parent folder (or workspace if root)

**Request**:
```json
{
  "name": "Documents",
  "parentId": "uuid-of-parent-or-null",
  "sortOrder": 0
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": "folder-uuid",
    "name": "Documents",
    "parentId": null,
    "workspaceId": "workspace-uuid",
    "sortOrder": 0,
    "status": "ACTIVE",
    "createdBy": "user-uuid",
    "createdAt": "2026-05-05T10:00:00Z",
    "updatedAt": "2026-05-05T10:00:00Z"
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Errors**:
- `400 INVALID_NAME` — Name is empty, too long, or contains invalid characters
- `400 DUPLICATE_NAME` — Folder with same name already exists under this parent
- `404 PARENT_NOT_FOUND` — Specified parentId does not exist
- `403 FORBIDDEN` — User lacks permission

---

### GET `/api/v1/workspaces/{workspaceId}/folders`

List all accessible folders in a workspace (flat list for tree building).

**Permission**: Authenticated workspace member

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| lazy | boolean | false | If true, return only root-level folders |

**Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "folder-uuid-1",
      "name": "Documents",
      "parentId": null,
      "sortOrder": 0,
      "status": "ACTIVE",
      "childCount": 3,
      "createdAt": "2026-05-05T10:00:00Z"
    },
    {
      "id": "folder-uuid-2",
      "name": "Legal",
      "parentId": "folder-uuid-1",
      "sortOrder": 0,
      "status": "ACTIVE",
      "childCount": 0,
      "createdAt": "2026-05-05T10:01:00Z"
    }
  ],
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

### GET `/api/v1/workspaces/{workspaceId}/folders/{folderId}`

Get a single folder with its breadcrumb path.

**Permission**: View access on the folder

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "folder-uuid-2",
    "name": "Legal",
    "parentId": "folder-uuid-1",
    "workspaceId": "workspace-uuid",
    "sortOrder": 0,
    "status": "ACTIVE",
    "createdBy": "user-uuid",
    "createdAt": "2026-05-05T10:01:00Z",
    "updatedAt": "2026-05-05T10:01:00Z",
    "breadcrumbs": [
      { "id": "folder-uuid-1", "name": "Documents" },
      { "id": "folder-uuid-2", "name": "Legal" }
    ]
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

### GET `/api/v1/workspaces/{workspaceId}/folders/{folderId}/children`

List direct children of a folder (for lazy-loading expansion).

**Permission**: View access on the parent folder

**Response** (200 OK): Same array format as folder list

---

### PUT `/api/v1/workspaces/{workspaceId}/folders/{folderId}`

Rename a folder or update its sort order.

**Permission**: `manage-folders` or Editor+ role on the folder

**Request**:
```json
{
  "name": "Legal Documents",
  "sortOrder": 1
}
```

**Response** (200 OK): Updated folder object

**Errors**:
- `400 DUPLICATE_NAME` — New name conflicts with sibling
- `404 NOT_FOUND` — Folder not found

---

### DELETE `/api/v1/workspaces/{workspaceId}/folders/{folderId}`

Soft-delete a folder and all descendants.

**Permission**: `manage-folders` or Admin role on the folder

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| permanent | boolean | false | If true, hard-delete (irreversible) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## Folder Move

### PUT `/api/v1/workspaces/{workspaceId}/folders/{folderId}/move`

Move a folder (with all descendants) to a new parent.

**Permission**: `manage-folders` or Editor+ role on source AND target

**Request**:
```json
{
  "targetParentId": "folder-uuid-or-null",
  "sortOrder": 0
}
```

**Response** (200 OK): Updated folder object with new parentId

**Errors**:
- `400 CIRCULAR_MOVE` — Target is a descendant of the source folder
- `400 DUPLICATE_NAME` — Folder name conflicts with existing sibling at target
- `404 TARGET_NOT_FOUND` — Target parent folder not found

---

## Favorites

### POST `/api/v1/workspaces/{workspaceId}/folders/{folderId}/favorite`

Add folder to user's favorites.

**Permission**: View access on the folder

**Response** (201 Created):
```json
{
  "success": true,
  "data": { "folderId": "folder-uuid", "favoritedAt": "2026-05-05T10:00:00Z" },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

### DELETE `/api/v1/workspaces/{workspaceId}/folders/{folderId}/favorite`

Remove folder from user's favorites.

**Response** (200 OK): `{ "success": true, "data": null }`

### GET `/api/v1/workspaces/{workspaceId}/favorites`

List user's favorite folders in this workspace.

**Response** (200 OK): Array of folder summary objects with `favoritedAt`

---

## Recents

### POST `/api/v1/workspaces/{workspaceId}/folders/{folderId}/visit`

Record a folder visit (updates recent items).

**Permission**: View access on the folder

**Response** (200 OK): `{ "success": true, "data": null }`

### GET `/api/v1/workspaces/{workspaceId}/recents`

List user's recently visited folders (max 10, newest first).

**Response** (200 OK): Array of folder summary objects with `accessedAt`

---

## Folder Permissions

### GET `/api/v1/workspaces/{workspaceId}/folders/{folderId}/permissions`

List all explicit permission assignments on a folder.

**Permission**: Admin role on the folder

**Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 5,
      "userName": "Alice Smith",
      "groupId": null,
      "groupName": null,
      "roleId": "role-uuid",
      "roleName": "Editor",
      "inherited": false,
      "inheritedFrom": null,
      "createdAt": "2026-05-05T10:00:00Z"
    },
    {
      "id": null,
      "userId": 8,
      "userName": "Bob Jones",
      "groupId": null,
      "groupName": null,
      "roleId": "role-uuid",
      "roleName": "Viewer",
      "inherited": true,
      "inheritedFrom": { "id": "parent-folder-uuid", "name": "Projects" },
      "createdAt": null
    }
  ],
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

### POST `/api/v1/workspaces/{workspaceId}/folders/{folderId}/permissions`

Assign a role to a user or group on this folder.

**Permission**: Admin role on the folder

**Request**:
```json
{
  "userId": 5,
  "groupId": null,
  "roleId": "role-uuid"
}
```

**Response** (201 Created): Created permission object

**Errors**:
- `400 INVALID_ASSIGNMENT` — Both userId and groupId are null, or both are set
- `400 DUPLICATE_ASSIGNMENT` — User/group already has an explicit assignment

### DELETE `/api/v1/workspaces/{workspaceId}/folders/{folderId}/permissions/{permissionId}`

Remove an explicit permission assignment.

**Permission**: Admin role on the folder

**Response** (200 OK): `{ "success": true, "data": null }`
