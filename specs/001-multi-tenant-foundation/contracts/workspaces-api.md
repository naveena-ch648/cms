# REST API Contracts: Workspaces

**Base path**: `/api/v1/workspaces`
**Authentication**: Bearer token required
**Tenant scoping**: All operations scoped to authenticated user's organization

---

## POST /api/v1/workspaces

**Description**: Create a new workspace.

**Permission required**: `manage-workspace`

**Request**:
```json
{
  "name": "Project Alpha",
  "description": "Main project workspace for Alpha initiative"
}
```

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Project Alpha",
    "description": "Main project workspace for Alpha initiative",
    "status": "ACTIVE",
    "memberCount": 1,
    "createdAt": "2026-05-05T12:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 409**: `WORKSPACE_NAME_EXISTS`
**Response 403**: `WORKSPACE_LIMIT_REACHED` — organization policy max workspaces exceeded

---

## GET /api/v1/workspaces

**Description**: List workspaces accessible to the current user.

**Permission required**: User sees only workspaces they are a member of (direct or via group). Admins with `manage-workspace` see all.

**Query parameters**: `page`, `size`, `search`, `status` (ACTIVE, ARCHIVED)

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Project Alpha",
      "description": "...",
      "status": "ACTIVE",
      "memberCount": 12,
      "myRole": { "id": "uuid", "name": "Editor", "source": "direct" },
      "createdAt": "2026-05-05T12:00:00Z"
    }
  ],
  "error": null,
  "meta": { "timestamp": "...", "requestId": "...", "pagination": { ... } }
}
```

**Note**: `myRole.source` indicates how the role was assigned: `"direct"`, `"group"`, or `"organization"` (fallback).

---

## GET /api/v1/workspaces/{workspaceId}

**Description**: Get workspace details.

**Permission required**: Member of workspace or `manage-workspace`

**Response 200**: Workspace object with `members` and `groups` arrays

---

## PUT /api/v1/workspaces/{workspaceId}

**Description**: Update workspace name/description.

**Permission required**: `manage-workspace`

**Request**: `{ "name": "...", "description": "..." }`

**Response 200**: Updated workspace object

---

## DELETE /api/v1/workspaces/{workspaceId}

**Description**: Delete workspace (soft delete → status DELETED).

**Permission required**: `manage-workspace`

**Response 200**: `{ "success": true, "data": { "message": "Workspace deleted" } }`

---

## POST /api/v1/workspaces/{workspaceId}/members

**Description**: Add a user to the workspace with a specific role.

**Permission required**: `manage-workspace`

**Request**:
```json
{
  "userId": "uuid",
  "roleId": "uuid"
}
```

**Response 201**: `{ "success": true, "data": { "userId": "uuid", "role": { "id": "uuid", "name": "Editor" } } }`
**Response 409**: `MEMBER_EXISTS` — user already has a direct role on this workspace

---

## PUT /api/v1/workspaces/{workspaceId}/members/{userId}/role

**Description**: Update a member's workspace role.

**Permission required**: `manage-workspace`

**Request**:
```json
{
  "roleId": "uuid"
}
```

**Response 200**: Updated membership

---

## DELETE /api/v1/workspaces/{workspaceId}/members/{userId}

**Description**: Remove a user from the workspace.

**Permission required**: `manage-workspace`

**Response 200**: `{ "success": true, "data": { "message": "Member removed from workspace" } }`

---

## GET /api/v1/workspaces/{workspaceId}/members

**Description**: List workspace members with their roles and source (direct/group).

**Permission required**: `view-workspace`

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "userId": "uuid",
      "email": "jane@acme.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "effectiveRole": { "id": "uuid", "name": "Editor" },
      "source": "direct",
      "groups": []
    },
    {
      "userId": "uuid",
      "email": "bob@acme.com",
      "firstName": "Bob",
      "lastName": "Jones",
      "effectiveRole": { "id": "uuid", "name": "Editor" },
      "source": "group",
      "groups": [{ "id": "uuid", "name": "Engineering" }]
    }
  ],
  "error": null,
  "meta": { "timestamp": "...", "requestId": "...", "pagination": { ... } }
}
```
