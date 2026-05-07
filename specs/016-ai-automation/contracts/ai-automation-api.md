# API Contract: AI Automation

**Feature**: 016-ai-automation  
**Base Path**: `/api/v1/ai`

## Endpoints

### GET /api/v1/ai/files/{fileId}/suggestions

Get all AI suggestions for a file.

**Auth**: Bearer token (JWT)  
**Access**: File must be accessible to user (read permission)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "fileId": "uuid",
    "processingStatus": "COMPLETED",
    "tags": {
      "status": "COMPLETED",
      "suggestions": ["legal", "contract", "NDA"],
      "confidence": {"legal": 95.0, "contract": 92.0, "NDA": 88.0},
      "acceptedTags": ["legal", "contract"],
      "rejectedTags": []
    },
    "classification": {
      "status": "COMPLETED",
      "category": "Contract",
      "confidence": 91.5,
      "alternatives": [
        {"category": "Policy", "confidence": 45.2}
      ]
    },
    "summary": {
      "status": "COMPLETED",
      "text": "This document outlines...",
      "wordCount": 187,
      "keyTopics": ["terms of service", "data privacy"]
    },
    "duplicates": {
      "status": "COMPLETED",
      "exactMatch": null,
      "nearDuplicates": [
        {"fileId": "uuid-123", "fileName": "contract_v1.pdf", "similarity": 92.3}
      ]
    },
    "sensitivity": {
      "status": "COMPLETED",
      "hasSensitiveData": true,
      "severity": "HIGH",
      "detections": [
        {"type": "CREDIT_CARD", "count": 2, "severity": "HIGH"}
      ]
    },
    "workflowRecommendation": {
      "status": "COMPLETED",
      "recommendedWorkflow": "Legal Review",
      "workflowId": "uuid-456",
      "reason": "Document classified as Contract"
    }
  }
}
```

**Response** `404 Not Found`:
```json
{
  "success": false,
  "error": "File not found"
}
```

---

### POST /api/v1/ai/files/{fileId}/accept-tags

Accept or reject AI-suggested tags for a file.

**Auth**: Bearer token (JWT)  
**Access**: File must be writable by user

**Request Body**:
```json
{
  "acceptedTags": ["legal", "contract"],
  "rejectedTags": ["NDA"]
}
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "appliedTags": ["legal", "contract"],
    "rejectedTags": ["NDA"]
  }
}
```

---

### POST /api/v1/ai/files/{fileId}/accept-classification

Accept the AI-suggested classification.

**Auth**: Bearer token (JWT)  
**Access**: File must be writable by user

**Request Body**:
```json
{
  "category": "Contract"
}
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "category": "Contract",
    "applied": true
  }
}
```

---

### POST /api/v1/ai/files/{fileId}/regenerate

Trigger re-analysis of AI suggestions for a file.

**Auth**: Bearer token (JWT)  
**Access**: File must be writable by user

**Request Body** (optional):
```json
{
  "types": ["TAG", "SUMMARIZE", "CLASSIFY"]
}
```

If `types` is omitted, all AI tasks are re-triggered.

**Response** `202 Accepted`:
```json
{
  "success": true,
  "data": {
    "jobIds": ["uuid-1", "uuid-2", "uuid-3"],
    "message": "AI analysis queued for re-processing"
  }
}
```

---

### GET /api/v1/ai/files/{fileId}/jobs

Get AI job history for a file.

**Auth**: Bearer token (JWT)  
**Access**: File must be accessible to user

**Query Params**:
- `page` (int, default 0)
- `size` (int, default 20)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "type": "TAG",
        "status": "COMPLETED",
        "confidence": 92.0,
        "triggeredBy": "SYSTEM",
        "createdAt": "2026-05-06T10:00:00Z",
        "completedAt": "2026-05-06T10:00:45Z"
      }
    ],
    "totalElements": 6,
    "totalPages": 1
  }
}
```

---

### POST /api/v1/ai/files/{fileId}/apply-workflow

Apply the recommended workflow to the file.

**Auth**: Bearer token (JWT)  
**Access**: File must be writable by user

**Request Body**:
```json
{
  "workflowId": "uuid-456"
}
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "fileId": "uuid",
    "workflowId": "uuid-456",
    "workflowName": "Legal Review",
    "applied": true
  }
}
```

---

### GET /api/v1/ai/config

Get AI automation configuration for the current organization.

**Auth**: Bearer token (JWT, Admin role)

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "enabledFeatures": ["TAG", "SUMMARIZE", "CLASSIFY", "DETECT_DUPLICATES", "DETECT_SENSITIVE", "RECOMMEND_WORKFLOW"],
    "confidenceThreshold": 70,
    "sensitivityPatterns": {
      "customPatterns": [
        {"name": "Employee ID", "pattern": "EMP-\\d{6}"}
      ]
    },
    "workflowMappings": {
      "Contract": "workflow-uuid-1",
      "Invoice": "workflow-uuid-2"
    }
  }
}
```

---

### PUT /api/v1/ai/config

Update AI automation configuration.

**Auth**: Bearer token (JWT, Admin role)

**Request Body**:
```json
{
  "enabledFeatures": ["TAG", "SUMMARIZE", "CLASSIFY"],
  "confidenceThreshold": 80,
  "sensitivityPatterns": {
    "customPatterns": []
  },
  "workflowMappings": {
    "Contract": "workflow-uuid-1"
  }
}
```

**Response** `200 OK`:
```json
{
  "success": true,
  "data": {
    "updated": true
  }
}
```

---

## Error Responses

All endpoints return standard error format:
```json
{
  "success": false,
  "error": "Error message"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Invalid request body |
| 401 | Authentication required |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 429 | Rate limited (AI processing) |
| 500 | Internal server error |
