# API Contract: Admin Console

**Feature**: 014-admin-console  
**Date**: 2026-05-06  
**Base URL**: `/api/v1`

## Overview

The admin console leverages existing APIs for most CRUD operations and adds two new endpoint groups:
1. **Admin Analytics** — org-wide metrics and trends
2. **Storage Quota Management** — update quota settings

### Existing APIs (consumed by admin console frontend)

| API | Endpoints | Permission |
|-----|-----------|------------|
| Users | `GET/POST /users`, `GET/PUT/DELETE /users/{id}`, `PUT /users/{id}/role`, `PUT /users/{id}/password` | manage-users |
| Roles | `GET/POST /roles`, `GET/PUT/DELETE /roles/{id}`, `GET /roles/permissions` | manage-roles |
| Groups | `GET/POST /groups`, `GET/PUT/DELETE /groups/{id}`, `POST/DELETE /groups/{id}/members` | manage-groups |
| Policies | `GET/PUT /policies` | manage-policies / view-roles |
| Audit | `GET /audit/events/search`, `GET /audit/stats` | view-audit-log |
| Storage Quota | `GET /files/quota` | (any authenticated user) |

---

## New Endpoints

### 1. Admin Analytics

#### `GET /admin/analytics`

Returns org-wide analytics metrics and trends.

**Permission**: `view-audit-log` (admin role)

**Query Parameters**:

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| days | int | No | 30 | Number of days for trend data (1-90) |

**Response** `200 OK`:

```json
{
  "summary": {
    "totalUsers": 156,
    "activeUsers": 142,
    "inactiveUsers": 12,
    "lockedUsers": 2,
    "totalFiles": 4521,
    "totalStorageUsedBytes": 8589934592,
    "totalStorageMaxBytes": 10737418240,
    "storageUsedPercent": 80.0,
    "totalWorkspaces": 12,
    "activeUsersLast30Days": 98
  },
  "roleDistribution": [
    { "roleName": "Admin", "userCount": 3 },
    { "roleName": "Editor", "userCount": 45 },
    { "roleName": "Viewer", "userCount": 108 }
  ],
  "uploadTrend": [
    { "date": "2026-04-07", "count": 23 },
    { "date": "2026-04-08", "count": 45 },
    ...
  ],
  "storageTrend": [
    { "date": "2026-04-07", "totalBytes": 7516192768 },
    { "date": "2026-04-08", "totalBytes": 7549747200 },
    ...
  ],
  "topActiveUsers": [
    { "userId": "uuid-1", "name": "John Doe", "actionCount": 234 },
    { "userId": "uuid-2", "name": "Jane Smith", "actionCount": 189 }
  ]
}
```

**Error Responses**:
- `401 Unauthorized` — not authenticated
- `403 Forbidden` — missing view-audit-log permission

**Caching**: Redis, 5-minute TTL, key: `admin:analytics:{orgId}:{days}`

---

### 2. Storage Quota Management

#### `PUT /admin/storage-quota`

Update organization storage quota settings.

**Permission**: `manage-policies`

**Request Body**:

```json
{
  "maxStorageBytes": 21474836480,
  "maxFileSizeBytes": 104857600,
  "allowedExtensions": null,
  "blockedExtensions": [".exe", ".bat", ".cmd", ".msi"],
  "trashRetentionDays": 60
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| maxStorageBytes | long | No | > 0 |
| maxFileSizeBytes | long | No | > 0, or null to remove limit |
| allowedExtensions | string[] | No | null or array of extensions (dot-prefixed) |
| blockedExtensions | string[] | No | null or array of extensions (dot-prefixed) |
| trashRetentionDays | int | No | 1–365 |

**Validation Rules**:
- allowedExtensions and blockedExtensions cannot both be set (mutually exclusive)
- Only provided (non-null) fields are updated; omitted fields retain current values
- Reducing maxStorageBytes below usedStorageBytes is allowed but triggers a warning in response

**Response** `200 OK`:

```json
{
  "maxStorageBytes": 21474836480,
  "usedStorageBytes": 8589934592,
  "maxFileSizeBytes": 104857600,
  "allowedExtensions": null,
  "blockedExtensions": [".exe", ".bat", ".cmd", ".msi"],
  "trashRetentionDays": 60,
  "usedPercent": 40.0,
  "warning": null
}
```

If maxStorageBytes < usedStorageBytes:
```json
{
  ...
  "warning": "Storage quota is below current usage. New uploads will be blocked until usage is reduced."
}
```

**Error Responses**:
- `400 Bad Request` — validation failure (e.g., both allowed and blocked set)
- `401 Unauthorized` — not authenticated
- `403 Forbidden` — missing manage-policies permission

---

#### `GET /admin/storage-quota`

Get storage quota with full details (including extension lists).

**Permission**: `manage-policies`

**Response** `200 OK`:

```json
{
  "maxStorageBytes": 10737418240,
  "usedStorageBytes": 8589934592,
  "maxFileSizeBytes": 104857600,
  "allowedExtensions": null,
  "blockedExtensions": [".exe", ".bat"],
  "trashRetentionDays": 30,
  "usedPercent": 80.0
}
```

---

### 3. User Bulk Operations

#### `POST /admin/users/bulk-action`

Perform bulk operations on multiple users.

**Permission**: `manage-users`

**Request Body**:

```json
{
  "userIds": ["uuid-1", "uuid-2", "uuid-3"],
  "action": "CHANGE_ROLE",
  "roleId": "role-uuid"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| userIds | string[] | Yes | List of user UUIDs (max 100) |
| action | string | Yes | CHANGE_ROLE, ACTIVATE, DEACTIVATE |
| roleId | string | Conditional | Required when action=CHANGE_ROLE |

**Response** `200 OK`:

```json
{
  "totalRequested": 3,
  "successful": 2,
  "failed": 1,
  "results": [
    { "userId": "uuid-1", "status": "SUCCESS" },
    { "userId": "uuid-2", "status": "SUCCESS" },
    { "userId": "uuid-3", "status": "FAILED", "reason": "Cannot deactivate last admin" }
  ]
}
```

**Validation Rules**:
- Max 100 users per batch
- Cannot include self in DEACTIVATE action
- Cannot change last admin's role
- Each user operation is independent (partial success allowed)
- All successful operations are logged as individual audit events

**Error Responses**:
- `400 Bad Request` — empty userIds, invalid action, missing roleId
- `401 Unauthorized` — not authenticated
- `403 Forbidden` — missing manage-users permission

---

## Audit Event Types (admin actions)

All admin operations generate audit events with category `PERMISSION_CHANGE` or `SYSTEM`:

| Event Type | Category | Description |
|------------|----------|-------------|
| USER_CREATED | PERMISSION_CHANGE | Admin created a new user |
| USER_UPDATED | PERMISSION_CHANGE | Admin updated user details |
| USER_ROLE_CHANGED | PERMISSION_CHANGE | Admin changed user's role |
| USER_PASSWORD_RESET | PERMISSION_CHANGE | Admin reset user's password |
| USER_DEACTIVATED | PERMISSION_CHANGE | Admin deactivated user |
| USER_ACTIVATED | PERMISSION_CHANGE | Admin reactivated user |
| ROLE_CREATED | PERMISSION_CHANGE | Admin created custom role |
| ROLE_UPDATED | PERMISSION_CHANGE | Admin updated role permissions |
| ROLE_DELETED | PERMISSION_CHANGE | Admin deleted custom role |
| GROUP_CREATED | PERMISSION_CHANGE | Admin created group |
| GROUP_UPDATED | PERMISSION_CHANGE | Admin updated group |
| GROUP_MEMBER_ADDED | PERMISSION_CHANGE | Admin added user to group |
| GROUP_MEMBER_REMOVED | PERMISSION_CHANGE | Admin removed user from group |
| STORAGE_QUOTA_UPDATED | SYSTEM | Admin changed storage quota |
| POLICY_UPDATED | SYSTEM | Admin changed organization policy |
