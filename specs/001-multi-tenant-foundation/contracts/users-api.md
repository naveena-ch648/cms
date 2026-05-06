# REST API Contracts: Users

**Base path**: `/api/v1/users`
**Authentication**: Bearer token required
**Tenant scoping**: All operations scoped to the authenticated user's organization

---

## POST /api/v1/users

**Description**: Register a new user in the current organization.

**Permission required**: `manage-users`

**Request**:
```json
{
  "email": "jane@acme.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "password": "SecurePass123!",
  "roleId": "uuid"
}
```

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "jane@acme.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "status": "ACTIVE",
    "organizationRole": {
      "id": "uuid",
      "name": "Editor"
    },
    "createdAt": "2026-05-05T12:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 409**: `USER_EMAIL_EXISTS` — email already registered in this organization
**Response 400**: `VALIDATION_ERROR` — password does not meet policy requirements

---

## GET /api/v1/users

**Description**: List users in the current organization (paginated).

**Permission required**: `view-users`

**Query parameters**:
- `page` (int, default 0)
- `size` (int, default 20, max 100)
- `sort` (string, e.g., "lastName,asc")
- `status` (string, filter: ACTIVE, INACTIVE, LOCKED)
- `search` (string, searches email/firstName/lastName)

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "email": "jane@acme.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "status": "ACTIVE",
      "organizationRole": { "id": "uuid", "name": "Editor" },
      "lastLoginAt": "2026-05-05T11:00:00Z",
      "createdAt": "2026-05-05T10:00:00Z"
    }
  ],
  "error": null,
  "meta": {
    "timestamp": "...",
    "requestId": "...",
    "pagination": { "page": 0, "size": 20, "totalElements": 45, "totalPages": 3 }
  }
}
```

---

## GET /api/v1/users/{userId}

**Description**: Get user details.

**Permission required**: `view-users` or own profile

**Response 200**: Single user object (same shape as list item, plus `groups` and `workspaces` arrays)

---

## PUT /api/v1/users/{userId}

**Description**: Update user details.

**Permission required**: `manage-users` or own profile (limited fields)

**Request**:
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "status": "ACTIVE"
}
```

**Response 200**: Updated user object

---

## PUT /api/v1/users/{userId}/role

**Description**: Change user's organization-level role.

**Permission required**: `manage-users`

**Request**:
```json
{
  "roleId": "uuid"
}
```

**Response 200**: Updated user with new role

**Response 409**: `LAST_ADMIN` — cannot remove last admin from organization

---

## PUT /api/v1/users/{userId}/password

**Description**: Change user password.

**Permission required**: `manage-users` or own account (requires current password)

**Request (admin)**:
```json
{
  "newPassword": "NewSecurePass456!"
}
```

**Request (self)**:
```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Response 200**: `{ "success": true, "data": { "message": "Password updated successfully" } }`
**Response 400**: `VALIDATION_ERROR` — password does not meet policy
**Response 401**: `AUTH_INVALID_PASSWORD` — current password incorrect (self-service)

---

## DELETE /api/v1/users/{userId}

**Description**: Deactivate user (soft delete).

**Permission required**: `manage-users`

**Response 200**: User with `status: "INACTIVE"`
**Response 409**: `LAST_ADMIN` — cannot deactivate last admin
