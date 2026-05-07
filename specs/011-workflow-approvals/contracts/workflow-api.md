# API Contract: Workflow State Transitions

**Base Path**: `/api/v1`

---

## POST /files/{fileId}/workflow/transition

Transition a document to a new workflow state.

**Authorization**: Document owner or workspace admin

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| fileId | string (UUID) | File identifier |

**Request Body**:
```json
{
  "targetState": "REVIEW",
  "comment": "Ready for team review"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "fileId": "uuid",
    "fromState": "DRAFT",
    "toState": "REVIEW",
    "actorId": "uuid",
    "actorName": "John Doe",
    "comment": "Ready for team review",
    "createdAt": "2026-05-06T10:00:00Z"
  }
}
```

**Error** (400 - Invalid transition):
```json
{
  "success": false,
  "error": "Invalid transition: cannot move from DRAFT to PUBLISHED"
}
```

**Error** (403 - Requires approval):
```json
{
  "success": false,
  "error": "Transition from REVIEW to APPROVED requires approval. Submit an approval request instead."
}
```

---

## POST /files/bulk-workflow/transition

Transition multiple documents to a new state.

**Authorization**: Document owner or workspace admin for all files

**Request Body**:
```json
{
  "fileIds": ["uuid1", "uuid2", "uuid3"],
  "targetState": "REVIEW",
  "comment": "Batch submission for review"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "transitioned": 3,
    "fileIds": ["uuid1", "uuid2", "uuid3"]
  }
}
```

**Error** (400 - Mixed states):
```json
{
  "success": false,
  "error": "All files must be in the same state for bulk transition. Found: DRAFT (2), REVIEW (1)"
}
```

---

## GET /files/{fileId}/workflow/history

Get the complete workflow transition history for a document.

**Authorization**: Workspace member with file read access

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number |
| size | int | 50 | Page size (max 100) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "fromState": "REVIEW",
      "toState": "APPROVED",
      "actorId": "uuid",
      "actorName": "Jane Smith",
      "comment": "All reviewers approved",
      "approvalRequestId": "uuid",
      "createdAt": "2026-05-06T12:00:00Z"
    },
    {
      "id": "uuid",
      "fromState": "DRAFT",
      "toState": "REVIEW",
      "actorId": "uuid",
      "actorName": "John Doe",
      "comment": "Ready for team review",
      "approvalRequestId": null,
      "createdAt": "2026-05-06T10:00:00Z"
    }
  ],
  "meta": {
    "page": 0,
    "size": 50,
    "totalResults": 2,
    "totalPages": 1
  }
}
```

---

## GET /files/{fileId}/workflow/state

Get the current workflow state of a document.

**Authorization**: Workspace member with file read access

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "currentState": "REVIEW",
    "allowedTransitions": ["APPROVED", "DRAFT"],
    "requiresApproval": ["APPROVED"],
    "hasActiveApproval": true,
    "activeApprovalId": "uuid"
  }
}
```
