# API Contract: Metadata Fields

**Base Path**: `/api/v1/workspaces/{workspaceId}/metadata-fields`  
**Auth**: Bearer JWT required  
**Tenant Isolation**: Workspace-scoped

---

## POST /api/v1/workspaces/{workspaceId}/metadata-fields

Create a new metadata field definition.

**Access**: Workspace Admin only

**Request Body**:
```json
{
  "name": "Department",
  "fieldType": "DROPDOWN",
  "description": "Team or department that owns this document",
  "options": ["HR", "Finance", "Engineering", "Legal"],
  "required": false,
  "displayOrder": 1
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | string | Yes | 1–100 chars, unique per workspace |
| fieldType | enum | Yes | TEXT, NUMBER, DATE, DROPDOWN |
| description | string | No | Max 500 chars |
| options | string[] | Conditional | Required if fieldType=DROPDOWN, non-empty |
| required | boolean | No | Default: false |
| displayOrder | integer | No | Default: 0 |

**Response 201**:
```json
{
  "success": true,
  "data": {
    "id": "uuid-string",
    "name": "Department",
    "fieldType": "DROPDOWN",
    "description": "Team or department that owns this document",
    "options": ["HR", "Finance", "Engineering", "Legal"],
    "required": false,
    "displayOrder": 1,
    "createdAt": "2026-05-06T12:00:00Z",
    "updatedAt": "2026-05-06T12:00:00Z"
  }
}
```

**Error 400**: Invalid field type, missing options for dropdown, name too long  
**Error 403**: User is not workspace admin  
**Error 409**: Field name already exists in workspace  
**Error 422**: Workspace has reached 50-field limit

---

## GET /api/v1/workspaces/{workspaceId}/metadata-fields

List all active metadata fields for a workspace.

**Access**: Any workspace member

**Query Parameters**:
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| includeDeleted | boolean | false | Include soft-deleted fields |

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid-1",
      "name": "Department",
      "fieldType": "DROPDOWN",
      "description": "...",
      "options": ["HR", "Finance", "Engineering", "Legal"],
      "required": false,
      "displayOrder": 1,
      "createdAt": "2026-05-06T12:00:00Z",
      "updatedAt": "2026-05-06T12:00:00Z"
    }
  ]
}
```

---

## PUT /api/v1/workspaces/{workspaceId}/metadata-fields/{fieldId}

Update a metadata field definition.

**Access**: Workspace Admin only

**Request Body**:
```json
{
  "name": "Department (Updated)",
  "description": "Updated description",
  "options": ["HR", "Finance", "Engineering", "Legal", "Marketing"],
  "required": true,
  "displayOrder": 2
}
```

**Note**: `fieldType` cannot be changed after creation (would invalidate existing values).

**Response 200**: Updated field object (same structure as POST response)  
**Error 400**: Validation failure  
**Error 403**: Not workspace admin  
**Error 404**: Field not found  
**Error 409**: New name conflicts with existing field

---

## DELETE /api/v1/workspaces/{workspaceId}/metadata-fields/{fieldId}

Soft-delete a metadata field. Existing values are preserved but hidden from UI.

**Access**: Workspace Admin only

**Response 204**: No content (field soft-deleted)  
**Error 403**: Not workspace admin  
**Error 404**: Field not found or already deleted
