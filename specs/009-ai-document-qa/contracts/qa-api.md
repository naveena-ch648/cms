# API Contract: AI Document Q&A

**Feature**: 009-ai-document-qa  
**Base Path**: `/api/v1/qa`  
**Authentication**: Bearer JWT (required for all endpoints)

---

## Endpoints

### 1. Ask a Question

**POST** `/api/v1/qa/ask`

Submit a question to the Q&A system. If `conversationId` is provided, the question is treated as a follow-up within that conversation's context.

**Request Body**:
```json
{
  "question": "What were the key findings in the quarterly report?",
  "workspaceId": "workspace-uuid",
  "conversationId": "conversation-uuid-or-null",
  "maxChunks": 5
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| question | string | Yes | Natural language question (max 2000 chars) |
| workspaceId | string (UUID) | Yes | Workspace to search within |
| conversationId | string (UUID) | No | Existing conversation for follow-ups. Null creates new conversation. |
| maxChunks | integer | No | Max chunks to retrieve (default: 5, max: 10) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "conversationId": "conversation-uuid",
    "messageId": "message-uuid",
    "answer": "According to the quarterly report, the key findings were...",
    "citations": [
      {
        "index": 1,
        "documentId": "file-uuid",
        "documentName": "Q3-2026-Report.pdf",
        "pageNumber": 5,
        "excerpt": "Revenue increased by 15% year-over-year, driven by...",
        "chunkId": "qdrant-point-id",
        "charStart": 1200,
        "charEnd": 1350
      }
    ],
    "modelUsed": "gpt-4o-mini",
    "tokenCount": 487,
    "noRelevantInfo": false
  }
}
```

**Response** (200 OK — no relevant info):
```json
{
  "success": true,
  "data": {
    "conversationId": "conversation-uuid",
    "messageId": "message-uuid",
    "answer": "I could not find relevant information in your documents to answer this question.",
    "citations": [],
    "modelUsed": null,
    "tokenCount": 0,
    "noRelevantInfo": true
  }
}
```

**Error Responses**:
- `400` — Invalid request (missing question, workspace not found)
- `401` — Unauthorized
- `403` — No access to specified workspace
- `429` — Rate limit exceeded (10 questions/minute)
- `503` — AI service or vector database unavailable

---

### 2. Summarize Documents

**POST** `/api/v1/qa/summarize`

Generate a summary of one or more selected documents.

**Request Body**:
```json
{
  "documentIds": ["file-uuid-1", "file-uuid-2"],
  "workspaceId": "workspace-uuid",
  "maxLength": "medium"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| documentIds | string[] (UUID) | Yes | Documents to summarize (max 5) |
| workspaceId | string (UUID) | Yes | Workspace scope |
| maxLength | string | No | Summary length: "short" (100 words), "medium" (300 words), "long" (600 words). Default: "medium" |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "summary": "The documents cover the following key topics...",
    "citations": [
      {
        "index": 1,
        "documentId": "file-uuid-1",
        "documentName": "strategy.pdf",
        "pageNumber": 2,
        "excerpt": "The primary goal for 2026 is...",
        "chunkId": "qdrant-point-id",
        "charStart": 500,
        "charEnd": 680
      }
    ],
    "documentsProcessed": 2,
    "modelUsed": "gpt-4o-mini",
    "tokenCount": 892
  }
}
```

**Error Responses**:
- `400` — No documents specified, too many documents (>5), document not found
- `401` — Unauthorized
- `403` — No access to one or more documents
- `422` — One or more documents not yet indexed (embedding pending)
- `503` — AI service unavailable

---

### 3. List Conversations

**GET** `/api/v1/qa/conversations`

List user's Q&A conversations in a workspace.

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| workspaceId | string (UUID) | Yes | Workspace to list conversations for |
| status | string | No | Filter: "ACTIVE" (default), "ARCHIVED", "ALL" |
| page | integer | No | Page number (default: 0) |
| size | integer | No | Page size (default: 20, max: 50) |
| search | string | No | Search conversation titles |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "conversation-uuid",
        "title": "Key findings in quarterly report",
        "status": "ACTIVE",
        "messageCount": 6,
        "createdAt": "2026-05-06T10:30:00Z",
        "updatedAt": "2026-05-06T11:15:00Z",
        "lastMessage": "What about the revenue projections?"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1
  }
}
```

---

### 4. Get Conversation Messages

**GET** `/api/v1/qa/conversations/{conversationId}/messages`

Retrieve all messages in a conversation.

**Path Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string (UUID) | Conversation ID |

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| page | integer | No | Page number (default: 0) |
| size | integer | No | Page size (default: 50, max: 100) |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "message-uuid-1",
        "role": "USER",
        "content": "What were the key findings in the quarterly report?",
        "citations": null,
        "createdAt": "2026-05-06T10:30:00Z"
      },
      {
        "id": "message-uuid-2",
        "role": "ASSISTANT",
        "content": "According to the quarterly report, the key findings were...",
        "citations": [
          {
            "index": 1,
            "documentId": "file-uuid",
            "documentName": "Q3-Report.pdf",
            "pageNumber": 5,
            "excerpt": "Revenue increased by 15%...",
            "chunkId": "qdrant-point-id",
            "charStart": 1200,
            "charEnd": 1350
          }
        ],
        "createdAt": "2026-05-06T10:30:04Z"
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 6,
    "totalPages": 1
  }
}
```

**Error Responses**:
- `401` — Unauthorized
- `403` — Conversation belongs to another user
- `404` — Conversation not found

---

### 5. Delete Conversation

**DELETE** `/api/v1/qa/conversations/{conversationId}`

Archive or permanently delete a conversation.

**Path Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| conversationId | string (UUID) | Conversation ID |

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| permanent | boolean | No | If true, permanently deletes. Default: false (archives). |

**Response** (204 No Content)

**Error Responses**:
- `401` — Unauthorized
- `403` — Conversation belongs to another user
- `404` — Conversation not found

---

### 6. Get Embedding Status

**GET** `/api/v1/qa/embedding-status/{fileId}`

Check if a document has been indexed for Q&A.

**Path Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| fileId | string (UUID) | File UUID |

**Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "fileId": "file-uuid",
    "status": "COMPLETED",
    "chunkCount": 47,
    "embeddingModel": "all-MiniLM-L6-v2",
    "vectorDimension": 384,
    "completedAt": "2026-05-06T10:25:00Z"
  }
}
```

**Status values**: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`

---

### 7. Re-index Document

**POST** `/api/v1/qa/reindex/{fileId}`

Trigger re-embedding for a document (e.g., after content update or model change).

**Path Parameters**:

| Parameter | Type | Description |
|-----------|------|-------------|
| fileId | string (UUID) | File UUID |

**Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "jobId": "embedding-job-uuid",
    "status": "PENDING",
    "message": "Re-indexing queued"
  }
}
```

**Error Responses**:
- `401` — Unauthorized
- `403` — No access to document
- `404` — Document not found
- `409` — Document is already being processed

---

## Common Response Envelope

All responses follow the existing CMS API envelope:

```json
{
  "success": true | false,
  "data": { ... } | null,
  "error": "error message" | null,
  "timestamp": "2026-05-06T10:30:00Z"
}
```

## Rate Limiting

| Endpoint | Limit |
|----------|-------|
| POST /ask | 10 requests/minute per user |
| POST /summarize | 5 requests/minute per user |
| GET endpoints | 60 requests/minute per user |

## WebSocket Events (Future)

Reserved for streaming answers in future iterations:
- `qa.answer.start` — Answer generation started
- `qa.answer.chunk` — Partial answer token
- `qa.answer.complete` — Full answer with citations ready
