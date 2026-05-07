# API Contract: Approval Requests & Decisions

**Base Path**: `/api/v1`

---

## POST /files/{fileId}/approvals

Submit a document for approval (creates an approval request with designated reviewers).

**Authorization**: Document owner or workspace member with write access

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| fileId | string (UUID) | File identifier |

**Request Body**:
```json
{
  "reviewerIds": ["user-uuid-1", "user-uuid-2"],
  "comment": "Please review the Q4 financial report"
}
```

**Validation**:
- File must be in REVIEW state
- No existing PENDING approval request for this file
- All reviewerIds must be valid workspace members
- At least 1 reviewer required, max 10

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileId": "uuid",
    "fileName": "Q4-report.pdf",
    "submitterId": "uuid",
    "submitterName": "John Doe",
    "status": "PENDING",
    "fromState": "REVIEW",
    "toState": "APPROVED",
    "reviewers": [
      { "id": "uuid", "name": "Jane Smith", "decision": "PENDING", "decidedAt": null },
      { "id": "uuid", "name": "Bob Wilson", "decision": "PENDING", "decidedAt": null }
    ],
    "createdAt": "2026-05-06T10:00:00Z",
    "completedAt": null
  }
}
```

**Error** (400):
```json
{
  "success": false,
  "error": "File must be in REVIEW state to submit for approval. Current state: DRAFT"
}
```

---

## GET /workspaces/{workspaceId}/approvals

List approval requests in a workspace (for reviewers to see their pending work).

**Authorization**: Workspace member

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| status | string | PENDING | Filter by status (PENDING, APPROVED, REJECTED, CANCELLED, ALL) |
| reviewerId | string | (current user) | Filter by reviewer (defaults to current user's pending) |
| page | int | 0 | Page number |
| size | int | 20 | Page size (max 100) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "fileId": "uuid",
      "fileName": "Q4-report.pdf",
      "submitterId": "uuid",
      "submitterName": "John Doe",
      "status": "PENDING",
      "reviewers": [
        { "id": "uuid", "name": "Jane Smith", "decision": "APPROVED", "decidedAt": "2026-05-06T11:00:00Z" },
        { "id": "uuid", "name": "Bob Wilson", "decision": "PENDING", "decidedAt": null }
      ],
      "createdAt": "2026-05-06T10:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalResults": 5,
    "totalPages": 1
  }
}
```

---

## GET /approvals/{approvalId}

Get details of a specific approval request.

**Authorization**: Submitter, designated reviewer, or workspace admin

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileId": "uuid",
    "fileName": "Q4-report.pdf",
    "submitterId": "uuid",
    "submitterName": "John Doe",
    "status": "PENDING",
    "fromState": "REVIEW",
    "toState": "APPROVED",
    "comment": "Please review the Q4 financial report",
    "reviewers": [
      { "id": "uuid", "name": "Jane Smith", "decision": "APPROVED", "comment": "Looks good!", "decidedAt": "2026-05-06T11:00:00Z" },
      { "id": "uuid", "name": "Bob Wilson", "decision": "PENDING", "comment": null, "decidedAt": null }
    ],
    "createdAt": "2026-05-06T10:00:00Z",
    "completedAt": null
  }
}
```

---

## POST /approvals/{approvalId}/decisions

Submit a reviewer's decision (approve or reject).

**Authorization**: Designated reviewer only

**Request Body**:
```json
{
  "decision": "APPROVED",
  "comment": "Reviewed and confirmed all figures are correct."
}
```

**Validation**:
- Caller must be a designated reviewer for this approval request
- Approval request must be in PENDING status
- Reviewer must not have already decided
- Decision must be "APPROVED" or "REJECTED"
- Comment is required for REJECTED decisions

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "approvalRequestId": "uuid",
    "reviewerId": "uuid",
    "reviewerName": "Jane Smith",
    "decision": "APPROVED",
    "comment": "Reviewed and confirmed all figures are correct.",
    "decidedAt": "2026-05-06T11:00:00Z",
    "approvalStatus": "PENDING",
    "approvedCount": 1,
    "totalReviewers": 2
  }
}
```

**Side effects**:
- If all reviewers approved → approval request status becomes APPROVED, document auto-transitions to target state
- If any reviewer rejects → approval request status becomes REJECTED, document returns to DRAFT

---

## POST /approvals/{approvalId}/cancel

Cancel a pending approval request (by the submitter).

**Authorization**: Original submitter or workspace admin

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "CANCELLED",
    "completedAt": "2026-05-06T12:00:00Z"
  }
}
```

**Validation**:
- Approval request must be in PENDING status
