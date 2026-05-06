# REST API Contracts: Authentication

**Base path**: `/api/v1/auth`
**Authentication**: None (public endpoints)

---

## POST /api/v1/auth/login

**Description**: Authenticate user with email and password.

**Request**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response 200**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "organizationId": "uuid",
      "organizationName": "Acme Corp"
    }
  },
  "error": null,
  "meta": { "timestamp": "2026-05-05T12:00:00Z", "requestId": "uuid" }
}
```

**Response 401**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "details": []
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 423** (account locked):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_ACCOUNT_LOCKED",
    "message": "Account is temporarily locked. Try again in 15 minutes.",
    "details": []
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## POST /api/v1/auth/refresh

**Description**: Refresh access token using refresh token.

**Request**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response 200**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

**Response 401**: `AUTH_INVALID_REFRESH_TOKEN`

---

## POST /api/v1/auth/logout

**Description**: Invalidate current session (blocklist the access token).

**Headers**: `Authorization: Bearer <accessToken>`

**Request**: Empty body

**Response 200**:
```json
{
  "success": true,
  "data": { "message": "Successfully signed out" },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```

---

## GET /api/v1/auth/me

**Description**: Get current authenticated user profile.

**Headers**: `Authorization: Bearer <accessToken>`

**Response 200**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "status": "ACTIVE",
    "organizationId": "uuid",
    "organizationName": "Acme Corp",
    "organizationRole": "Admin",
    "lastLoginAt": "2026-05-05T11:00:00Z"
  },
  "error": null,
  "meta": { "timestamp": "...", "requestId": "..." }
}
```
