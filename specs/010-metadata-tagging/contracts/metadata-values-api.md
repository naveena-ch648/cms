# API Contract: Metadata Values

**Base Path**: `/api/v1/files/{fileId}/metadata`  
**Auth**: Bearer JWT required  
**Tenant Isolation**: Workspace-scoped via file ownership

---

## GET /api/v1/files/{fileId}/metadata

Get all metadata values assigned to a file.

**Access**: Any user with file read access

**Response 200**:
```json
{
  "success": true,
  "data": [
    {
      "fieldId": "field-uuid-1",
      "fieldName": "Department",
      "fieldType": "DROPDOWN",
      "value": "Engineering",
      "updatedAt": "2026-05-06T14:00:00Z"
    },
    {
      "fieldId": "field-uuid-2",
      "fieldName": "Contract Value",
      "fieldType": "NUMBER",
      "value": 50000.00,
      "updatedAt": "2026-05-06T14:00:00Z"
    },
    {
      "fieldId": "field-uuid-3",
      "fieldName": "Due Date",
      "fieldType": "DATE",
      "value": "2026-12-31",
      "updatedAt": "2026-05-06T14:00:00Z"
    }
  ]
}
```

---

## PUT /api/v1/files/{fileId}/metadata

Set or update metadata values for a file. Accepts a batch of field-value pairs.

**Access**: Any user with file write access

**Request Body**:
```json
{
  "values": [
    { "fieldId": "field-uuid-1", "value": "Engineering" },
    { "fieldId": "field-uuid-2", "value": 50000.00 },
    { "fieldId": "field-uuid-3", "value": "2026-12-31" }
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| values | array | Yes | Non-empty array of field-value pairs |
| values[].fieldId | string | Yes | Must reference an active field in the file's workspace |
| values[].value | string/number/null | Yes | Type must match field type; null to clear |

**Response 200**:
```json
{
  "success": true,
  "data": {
    "updated": 3,
    "values": [
      { "fieldId": "field-uuid-1", "fieldName": "Department", "fieldType": "DROPDOWN", "value": "Engineering", "updatedAt": "..." },
      { "fieldId": "field-uuid-2", "fieldName": "Contract Value", "fieldType": "NUMBER", "value": 50000.00, "updatedAt": "..." },
      { "fieldId": "field-uuid-3", "fieldName": "Due Date", "fieldType": "DATE", "value": "2026-12-31", "updatedAt": "..." }
    ]
  }
}
```

**Error 400**: Type mismatch, invalid dropdown value, validation failure  
**Error 403**: No write access to file  
**Error 404**: File or field not found  
**Error 422**: Required field has null value

---

## DELETE /api/v1/files/{fileId}/metadata/{fieldId}

Clear a specific metadata value from a file.

**Access**: Any user with file write access

**Response 204**: No content  
**Error 403**: No write access  
**Error 404**: File or field not found, or no value set

---

## PUT /api/v1/files/bulk-metadata

Assign metadata values to multiple files at once.

**Access**: User must have write access to all specified files

**Request Body**:
```json
{
  "fileIds": ["file-uuid-1", "file-uuid-2", "file-uuid-3"],
  "values": [
    { "fieldId": "field-uuid-1", "value": "Legal" }
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| fileIds | string[] | Yes | 1–100 file UUIDs |
| values | array | Yes | Non-empty field-value pairs |

**Response 200**:
```json
{
  "success": true,
  "data": {
    "totalFiles": 3,
    "updated": 3,
    "failed": 0,
    "errors": []
  }
}
```

**Error 400**: More than 100 files, empty values  
**Error 403**: No write access to one or more files (returns list of inaccessible file IDs)
