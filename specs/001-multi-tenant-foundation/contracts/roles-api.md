# REST API Contracts: Roles & Permissions

**Base path**: `/api/v1/roles`, `/api/v1/permissions`
**Authentication**: Bearer token required
**Tenant scoping**: Roles scoped to organization; Permissions are global

---

## GET /api/v1/roles

**Description**: List roles in the current organization.

**Permission required**: `view-roles`

**Query parameters**:
- `page` (int, default 0)
- `size` (int, default 20)

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Editor",
      "description": "Can view and edit content",
      "parentRole": { "id": "uuid", "name": "Viewer" },
      "isSystem": false,
      "directPermissions": ["edit-document"],
      "effectivePermissions": ["view-workspace", "view-users", "view-roles", "view-groups", "edit-document"],
      "createdAt": "2026-05-05T12:00:00Z"
    }
  ],
  "error": null,
  "meta": { "timestamp": "...", "requestId": "...", "pagination": { ... } }
}
```

---

## POST /api/v1/roles

**Description**: Create a new role.

**Permission required**: `manage-roles`

**Request**:
```json
{
  "name": "ContentManager",
  "description": "Manages content within workspaces",
  "parentRoleId": "uuid-of-editor",
  "permissionIds": ["uuid-of-manage-workspace"]
}
```

**Response 201**: Created role object
**Response 409**: `ROLE_NAME_EXISTS`
**Response 400**: `CIRCULAR_INHERITANCE` — would create a cycle

---

## PUT /api/v1/roles/{roleId}

**Description**: Update role details, parent, or permissions.

**Permission required**: `manage-roles`

**Request**:
```json
{
  "name": "ContentManager",
  "description": "Updated description",
  "parentRoleId": "uuid",
  "permissionIds": ["uuid1", "uuid2"]
}
```

**Response 200**: Updated role with recalculated effective permissions
**Response 400**: `CIRCULAR_INHERITANCE`
**Response 403**: `SYSTEM_ROLE_IMMUTABLE` — cannot modify name of system roles

---

## DELETE /api/v1/roles/{roleId}

**Description**: Delete a custom role. Reassigns child roles to this role's parent.

**Permission required**: `manage-roles`

**Response 200**: `{ "success": true, "data": { "message": "Role deleted", "reassignedChildren": 2 } }`
**Response 403**: `SYSTEM_ROLE_IMMUTABLE` — cannot delete system roles
**Response 409**: `ROLE_IN_USE` — users are assigned this role (must reassign first)

---

## GET /api/v1/roles/{roleId}

**Description**: Get role details with full permission tree.

**Permission required**: `view-roles`

**Response 200**: Single role object with `effectivePermissions` and `inheritanceChain`

---

## GET /api/v1/permissions

**Description**: List all available permissions (system-wide, not tenant-scoped).

**Permission required**: `view-roles`

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "manage-users",
      "description": "Create, update, deactivate users",
      "category": "user"
    }
  ],
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```
