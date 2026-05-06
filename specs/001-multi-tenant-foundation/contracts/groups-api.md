# REST API Contracts: Groups

**Base path**: `/api/v1/groups`
**Authentication**: Bearer token required
**Tenant scoping**: All operations scoped to authenticated user's organization

---

## POST /api/v1/groups

**Description**: Create a new group.

**Permission required**: `manage-groups`

**Request**:
```json
{
  "name": "Engineering",
  "description": "Engineering team members"
}
```

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Engineering",
    "description": "Engineering team members",
    "memberCount": 0,
    "workspaceRoles": [],
    "createdAt": "2026-05-05T12:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 409**: `GROUP_NAME_EXISTS`

---

## GET /api/v1/groups

**Description**: List groups in the current organization (paginated).

**Permission required**: `view-groups`

**Query parameters**: `page`, `size`, `search`

**Response 200**: Array of group objects with `memberCount`

---

## GET /api/v1/groups/{groupId}

**Description**: Get group details with members and workspace roles.

**Permission required**: `view-groups`

**Response 200**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Engineering",
    "description": "Engineering team members",
    "members": [
      { "id": "uuid", "email": "jane@acme.com", "firstName": "Jane", "lastName": "Smith" }
    ],
    "workspaceRoles": [
      { "workspaceId": "uuid", "workspaceName": "Project Alpha", "role": { "id": "uuid", "name": "Editor" } }
    ],
    "createdAt": "2026-05-05T12:00:00Z",
    "updatedAt": "2026-05-05T12:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## PUT /api/v1/groups/{groupId}

**Description**: Update group name/description.

**Permission required**: `manage-groups`

**Request**: `{ "name": "...", "description": "..." }`

**Response 200**: Updated group object

---

## DELETE /api/v1/groups/{groupId}

**Description**: Delete group. Removes all workspace role assignments for the group.

**Permission required**: `manage-groups`

**Response 200**: `{ "success": true, "data": { "message": "Group deleted" } }`

---

## POST /api/v1/groups/{groupId}/members

**Description**: Add users to the group.

**Permission required**: `manage-groups`

**Request**:
```json
{
  "userIds": ["uuid1", "uuid2"]
}
```

**Response 200**: Updated member list

---

## DELETE /api/v1/groups/{groupId}/members/{userId}

**Description**: Remove a user from the group.

**Permission required**: `manage-groups`

**Response 200**: `{ "success": true, "data": { "message": "Member removed" } }`

---

## PUT /api/v1/groups/{groupId}/workspaces/{workspaceId}/role

**Description**: Assign or update the group's role on a workspace.

**Permission required**: `manage-groups` AND `manage-workspace`

**Request**:
```json
{
  "roleId": "uuid"
}
```

**Response 200**: Updated workspace role assignment

---

## DELETE /api/v1/groups/{groupId}/workspaces/{workspaceId}/role

**Description**: Remove the group's role from a workspace.

**Permission required**: `manage-groups` AND `manage-workspace`

**Response 200**: `{ "success": true, "data": { "message": "Workspace role removed" } }`
