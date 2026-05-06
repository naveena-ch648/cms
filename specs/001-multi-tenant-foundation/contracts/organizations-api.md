# REST API Contracts: Organizations

**Base path**: `/api/v1/organizations`
**Authentication**: Bearer token required (all endpoints)

---

## POST /api/v1/organizations

**Description**: Create a new organization (platform admin only).

**Permission required**: `platform-admin` (super admin)

**Request**:
```json
{
  "name": "Acme Corporation",
  "slug": "acme-corp",
  "billingContactEmail": "billing@acme.com"
}
```

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "billingContactEmail": "billing@acme.com",
    "status": "ACTIVE",
    "policies": {},
    "createdAt": "2026-05-05T12:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 409**: `ORG_SLUG_EXISTS` — slug already taken

---

## GET /api/v1/organizations/{orgId}

**Description**: Get organization details.

**Permission required**: Member of the organization

**Response 200**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "billingContactEmail": "billing@acme.com",
    "status": "ACTIVE",
    "policies": {
      "passwordMinLength": 10,
      "sessionTimeoutMinutes": 60
    },
    "createdAt": "2026-05-05T12:00:00Z",
    "updatedAt": "2026-05-05T13:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## PUT /api/v1/organizations/{orgId}

**Description**: Update organization details.

**Permission required**: `manage-policies`

**Request**:
```json
{
  "name": "Acme Corp Updated",
  "billingContactEmail": "newbilling@acme.com"
}
```

**Response 200**: Updated organization object (same shape as GET)

---

## PUT /api/v1/organizations/{orgId}/policies

**Description**: Update organization policies.

**Permission required**: `manage-policies`

**Request**:
```json
{
  "passwordMinLength": 12,
  "passwordRequireUppercase": true,
  "passwordRequireNumber": true,
  "passwordRequireSpecialChar": true,
  "sessionTimeoutMinutes": 15,
  "maxWorkspaces": 100
}
```

**Response 200**:
```json
{
  "success": true,
  "data": {
    "policies": {
      "passwordMinLength": 12,
      "passwordRequireUppercase": true,
      "passwordRequireNumber": true,
      "passwordRequireSpecialChar": true,
      "sessionTimeoutMinutes": 15,
      "maxWorkspaces": 100,
      "maxFailedLoginAttempts": 5,
      "accountLockoutMinutes": 15
    }
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## PUT /api/v1/organizations/{orgId}/deactivate

**Description**: Deactivate organization (soft delete).

**Permission required**: `platform-admin`

**Response 200**: Organization with `status: "DEACTIVATED"`
