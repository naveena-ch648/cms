# API Contract: Audit & Compliance

**Feature**: 013-audit-compliance  
**Base URL**: `/api/audit`  
**Authentication**: JWT Bearer token required  
**Authorization**: Organization Admin role required for all endpoints

---

## 1. Search Audit Events

**GET** `/api/audit/events`

Search and filter audit events within the caller's organization.

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| q | string | No | Full-text search query |
| userId | long | No | Filter by actor user ID |
| category | string | No | Filter: AUTHENTICATION, FILE_OPERATION, PERMISSION_CHANGE, SHARING, WORKFLOW, SYSTEM |
| eventType | string | No | Filter by specific event type (e.g., LOGIN_FAILURE) |
| resourceType | string | No | Filter by target resource type |
| resourceId | long | No | Filter by target resource ID |
| outcome | string | No | Filter: SUCCESS, FAILURE |
| workspaceId | long | No | Filter by workspace |
| dateFrom | ISO date | No | Start of date range (inclusive) |
| dateTo | ISO date | No | End of date range (inclusive) |
| page | int | No | Page number (default: 0) |
| size | int | No | Page size (default: 20, max: 100) |
| sort | string | No | Sort field (default: createdAt) |
| direction | string | No | ASC or DESC (default: DESC) |

### Response: 200 OK

```json
{
  "content": [
    {
      "id": 12345,
      "eventType": "FILE_UPLOADED",
      "category": "FILE_OPERATION",
      "actorName": "john.doe@company.com",
      "userId": 42,
      "resourceType": "FILE",
      "resourceId": 789,
      "resourceName": "Q4-Report.pdf",
      "outcome": "SUCCESS",
      "ipAddress": "192.168.1.1",
      "workspaceId": 5,
      "createdAt": "2026-05-06T14:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1523,
  "totalPages": 77
}
```

### Error Responses

- 401: Unauthorized (missing/invalid token)
- 403: Forbidden (not organization admin)

---

## 2. Get Audit Event Detail

**GET** `/api/audit/events/{eventId}`

Retrieve full details of a single audit event.

### Path Parameters

| Param | Type | Description |
|-------|------|-------------|
| eventId | long | Audit event ID |

### Response: 200 OK

```json
{
  "id": 12345,
  "eventType": "ROLE_ASSIGNED",
  "category": "PERMISSION_CHANGE",
  "actorName": "admin@company.com",
  "userId": 1,
  "resourceType": "USER",
  "resourceId": 42,
  "resourceName": "john.doe@company.com",
  "outcome": "SUCCESS",
  "details": {
    "oldRole": "VIEWER",
    "newRole": "EDITOR",
    "workspaceName": "Engineering"
  },
  "ipAddress": "10.0.0.1",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
  "workspaceId": 5,
  "createdAt": "2026-05-06T14:30:00Z"
}
```

### Error Responses

- 401: Unauthorized
- 403: Forbidden (event belongs to different organization)
- 404: Not found

---

## 3. Get Audit Statistics

**GET** `/api/audit/stats`

Get aggregated audit statistics for the organization.

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| dateFrom | ISO date | No | Start of period (default: 30 days ago) |
| dateTo | ISO date | No | End of period (default: today) |

### Response: 200 OK

```json
{
  "totalEvents": 45230,
  "eventsByCategory": {
    "AUTHENTICATION": 12500,
    "FILE_OPERATION": 25000,
    "PERMISSION_CHANGE": 800,
    "SHARING": 3200,
    "WORKFLOW": 2730,
    "SYSTEM": 1000
  },
  "eventsByOutcome": {
    "SUCCESS": 44100,
    "FAILURE": 1130
  },
  "topActors": [
    { "userId": 42, "name": "john.doe@company.com", "eventCount": 5230 }
  ],
  "period": {
    "from": "2026-04-06",
    "to": "2026-05-06"
  }
}
```

---

## 4. Request Compliance Report

**POST** `/api/audit/reports`

Request generation of a compliance report (async).

### Request Body

```json
{
  "reportType": "ALL_EVENTS",
  "dateFrom": "2026-04-01",
  "dateTo": "2026-04-30"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| reportType | string | Yes | ALL_EVENTS, SECURITY_EVENTS, DATA_ACCESS |
| dateFrom | ISO date | Yes | Report start date |
| dateTo | ISO date | Yes | Report end date |

### Response: 202 Accepted

```json
{
  "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "reportType": "ALL_EVENTS",
  "status": "PENDING",
  "dateFrom": "2026-04-01",
  "dateTo": "2026-04-30",
  "createdAt": "2026-05-06T15:00:00Z"
}
```

### Validation Errors: 400 Bad Request

- dateFrom must be before dateTo
- Date range must not exceed 365 days
- reportType must be valid

---

## 5. List Compliance Reports

**GET** `/api/audit/reports`

List all compliance reports for the organization.

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| page | int | No | Page number (default: 0) |
| size | int | No | Page size (default: 10) |

### Response: 200 OK

```json
{
  "content": [
    {
      "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "reportType": "ALL_EVENTS",
      "status": "COMPLETED",
      "dateFrom": "2026-04-01",
      "dateTo": "2026-04-30",
      "totalEvents": 15230,
      "fileSize": 2048576,
      "createdAt": "2026-05-06T15:00:00Z",
      "completedAt": "2026-05-06T15:00:45Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5,
  "totalPages": 1
}
```

---

## 6. Download Compliance Report

**GET** `/api/audit/reports/{uuid}/download`

Download a completed compliance report as CSV.

### Path Parameters

| Param | Type | Description |
|-------|------|-------------|
| uuid | string | Report UUID |

### Response: 200 OK

- Content-Type: `text/csv`
- Content-Disposition: `attachment; filename="compliance-report-2026-04-01-to-2026-04-30.csv"`

### Error Responses

- 404: Report not found
- 409: Report not yet completed (status != COMPLETED)

---

## 7. List Alert Rules

**GET** `/api/audit/alerts/rules`

List all configured alert rules for the organization.

### Response: 200 OK

```json
{
  "content": [
    {
      "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "name": "Failed Login Spike",
      "description": "Alert when a user fails login 5+ times in 5 minutes",
      "eventType": "LOGIN_FAILURE",
      "thresholdCount": 5,
      "timeWindowMinutes": 5,
      "enabled": true,
      "createdAt": "2026-05-01T10:00:00Z"
    }
  ]
}
```

---

## 8. Create Alert Rule

**POST** `/api/audit/alerts/rules`

Create a new alert threshold rule.

### Request Body

```json
{
  "name": "Bulk File Deletion",
  "description": "Alert when a user deletes 20+ files in 10 minutes",
  "eventType": "FILE_DELETED",
  "thresholdCount": 20,
  "timeWindowMinutes": 10
}
```

### Response: 201 Created

```json
{
  "uuid": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "name": "Bulk File Deletion",
  "description": "Alert when a user deletes 20+ files in 10 minutes",
  "eventType": "FILE_DELETED",
  "thresholdCount": 20,
  "timeWindowMinutes": 10,
  "enabled": true,
  "createdAt": "2026-05-06T16:00:00Z"
}
```

### Validation: 400 Bad Request

- name is required (max 100 chars)
- eventType is required and must be valid
- thresholdCount must be > 0
- timeWindowMinutes must be 1-1440

---

## 9. Update Alert Rule

**PUT** `/api/audit/alerts/rules/{uuid}`

Update an existing alert rule.

### Request Body

```json
{
  "name": "Bulk File Deletion",
  "thresholdCount": 30,
  "timeWindowMinutes": 15,
  "enabled": false
}
```

### Response: 200 OK

Returns updated rule object.

---

## 10. Delete Alert Rule

**DELETE** `/api/audit/alerts/rules/{uuid}`

Delete an alert rule.

### Response: 204 No Content

---

## 11. List Alert Instances

**GET** `/api/audit/alerts`

List triggered alert instances.

### Query Parameters

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| acknowledged | boolean | No | Filter by acknowledged status |
| ruleId | string | No | Filter by rule UUID |
| page | int | No | Page number (default: 0) |
| size | int | No | Page size (default: 20) |

### Response: 200 OK

```json
{
  "content": [
    {
      "uuid": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "ruleName": "Failed Login Spike",
      "ruleUuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "triggeredByUserName": "suspicious.user@company.com",
      "eventCount": 7,
      "windowStart": "2026-05-06T14:25:00Z",
      "windowEnd": "2026-05-06T14:30:00Z",
      "acknowledged": false,
      "createdAt": "2026-05-06T14:30:15Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

## 12. Acknowledge Alert

**POST** `/api/audit/alerts/{uuid}/acknowledge`

Mark an alert instance as acknowledged.

### Response: 200 OK

```json
{
  "uuid": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "acknowledged": true,
  "acknowledgedBy": "admin@company.com",
  "acknowledgedAt": "2026-05-06T15:00:00Z"
}
```

---

## 13. Get Alert Instance Events

**GET** `/api/audit/alerts/{uuid}/events`

Get the audit events that triggered a specific alert.

### Response: 200 OK

Returns paginated list of AuditEvent DTOs (same format as search results) linked to this alert.

---

## Common Error Response Format

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Date range must not exceed 365 days",
  "timestamp": "2026-05-06T15:00:00Z"
}
```

## Notes

- All endpoints are scoped to the caller's organization (extracted from JWT).
- No DELETE or PUT endpoints exist for audit events (immutable).
- Report download URLs are valid only for the requesting organization's admins.
- OpenSearch is used for full-text search; MySQL is the source of truth for all data.
