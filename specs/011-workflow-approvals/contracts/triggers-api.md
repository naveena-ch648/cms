# API Contract: Workflow Triggers

**Base Path**: `/api/v1`

---

## POST /workspaces/{workspaceId}/workflow-triggers

Create a new workflow trigger.

**Authorization**: Workspace admin only

**Request Body**:
```json
{
  "name": "Notify reviewers on Review entry",
  "triggerState": "REVIEW",
  "triggerType": "NOTIFICATION",
  "config": {
    "recipients": "all_reviewers",
    "messageTemplate": "Document {fileName} is ready for review"
  },
  "enabled": true
}
```

**Trigger Types & Config Schema**:

NOTIFICATION:
```json
{
  "recipients": "all_reviewers | all_admins | specific",
  "specificUserIds": ["uuid1", "uuid2"],
  "messageTemplate": "string with {fileName}, {actorName}, {state} placeholders"
}
```

PREREQUISITE:
```json
{
  "requireMetadataFields": ["department", "classification"],
  "requireTags": ["reviewed"],
  "customMessage": "Document must have Department and Classification metadata set before publishing"
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "Notify reviewers on Review entry",
    "triggerState": "REVIEW",
    "triggerType": "NOTIFICATION",
    "config": { ... },
    "enabled": true,
    "createdBy": "uuid",
    "createdAt": "2026-05-06T10:00:00Z"
  }
}
```

---

## GET /workspaces/{workspaceId}/workflow-triggers

List all triggers for a workspace.

**Authorization**: Workspace admin

**Query Parameters**:
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| triggerState | string | (all) | Filter by target state |
| enabled | boolean | (all) | Filter by enabled status |

**Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Notify reviewers on Review entry",
      "triggerState": "REVIEW",
      "triggerType": "NOTIFICATION",
      "config": { ... },
      "enabled": true,
      "createdBy": "uuid",
      "createdAt": "2026-05-06T10:00:00Z",
      "updatedAt": "2026-05-06T10:00:00Z"
    }
  ]
}
```

---

## PUT /workspaces/{workspaceId}/workflow-triggers/{triggerId}

Update a workflow trigger.

**Authorization**: Workspace admin

**Request Body**:
```json
{
  "name": "Updated trigger name",
  "triggerState": "PUBLISHED",
  "triggerType": "PREREQUISITE",
  "config": {
    "requireMetadataFields": ["department"],
    "customMessage": "Must set department before publishing"
  },
  "enabled": true
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": { ... }
}
```

---

## DELETE /workspaces/{workspaceId}/workflow-triggers/{triggerId}

Delete a workflow trigger.

**Authorization**: Workspace admin

**Response** (204 No Content)

---

## PATCH /workspaces/{workspaceId}/workflow-triggers/{triggerId}/toggle

Enable or disable a trigger.

**Authorization**: Workspace admin

**Request Body**:
```json
{
  "enabled": false
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "enabled": false
  }
}
```
