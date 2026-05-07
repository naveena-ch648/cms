# Data Model: AI Document Q&A System

**Feature**: 009-ai-document-qa  
**Date**: 2026-05-06

---

## Entities

### Conversation

Represents a Q&A session belonging to a user within a workspace.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| user_id | BIGINT | FK → users.id, NOT NULL | Owner of the conversation |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Workspace scope |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant isolation |
| title | VARCHAR(255) | NOT NULL | Auto-generated from first question |
| status | ENUM('ACTIVE','ARCHIVED') | NOT NULL, DEFAULT 'ACTIVE' | Conversation state |
| message_count | INT | NOT NULL, DEFAULT 0 | Cached count of messages |
| created_at | TIMESTAMP | NOT NULL | Creation time |
| updated_at | TIMESTAMP | NOT NULL | Last activity time |

**Indexes**:
- `idx_conversation_user_workspace` ON (user_id, workspace_id, status)
- `idx_conversation_org` ON (organization_id)
- `idx_conversation_updated` ON (updated_at DESC)

---

### ConversationMessage

A single exchange within a conversation — either a user question or a system answer.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| conversation_id | BIGINT | FK → conversations.id, NOT NULL | Parent conversation |
| role | ENUM('USER','ASSISTANT') | NOT NULL | Message sender type |
| content | TEXT | NOT NULL | Message text content |
| citations | JSON | NULL | Structured citation data (for ASSISTANT messages) |
| token_count | INT | NULL | Token usage for tracking |
| model_used | VARCHAR(100) | NULL | LLM model identifier used |
| retrieval_chunks | JSON | NULL | IDs of chunks retrieved for this answer |
| created_at | TIMESTAMP | NOT NULL | Message timestamp |

**Indexes**:
- `idx_message_conversation` ON (conversation_id, created_at)

**Citations JSON Structure**:
```json
[
  {
    "index": 1,
    "documentId": "uuid-of-file",
    "documentName": "report.pdf",
    "pageNumber": 5,
    "excerpt": "The quarterly revenue increased by 15%...",
    "chunkId": "qdrant-point-id",
    "charStart": 1200,
    "charEnd": 1350
  }
]
```

---

### EmbeddingJob

Tracks the chunking and embedding status of a document through the pipeline.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Source document |
| organization_id | BIGINT | FK → organizations.id, NOT NULL | Tenant isolation |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Workspace scope |
| status | ENUM('PENDING','PROCESSING','COMPLETED','FAILED') | NOT NULL, DEFAULT 'PENDING' | Job state |
| chunk_count | INT | NULL | Number of chunks generated |
| embedding_model | VARCHAR(100) | NOT NULL | Model used for embedding |
| vector_dimension | INT | NOT NULL | Dimension of generated vectors |
| error_message | TEXT | NULL | Error details if failed |
| retry_count | INT | NOT NULL, DEFAULT 0 | Number of retries attempted |
| started_at | TIMESTAMP | NULL | Processing start time |
| completed_at | TIMESTAMP | NULL | Processing completion time |
| created_at | TIMESTAMP | NOT NULL | Job creation time |
| updated_at | TIMESTAMP | NOT NULL | Last status update |

**Indexes**:
- `idx_embedding_job_file` ON (file_id)
- `idx_embedding_job_status` ON (organization_id, status)
- `idx_embedding_job_workspace` ON (workspace_id, status)

---

### DocumentChunk (Qdrant Collection: `document_chunks`)

Stored in Qdrant as vector points with payload metadata. Not a MySQL table.

| Payload Field | Type | Description |
|---------------|------|-------------|
| document_id | string (UUID) | File UUID from files table |
| organization_id | integer | Tenant isolation filter |
| workspace_id | integer | Workspace scope filter |
| page_number | integer | Source page number (1-based) |
| section_title | string | Nearest heading/section title |
| char_start | integer | Character offset start in original text |
| char_end | integer | Character offset end in original text |
| chunk_text | string | The actual text content of the chunk |
| chunk_index | integer | Sequential position within document |
| file_name | string | Original filename (for citations) |
| mime_type | string | Document MIME type |
| embedded_at | string (ISO datetime) | When embedding was generated |
| embedding_model | string | Model used for this embedding |

**Collection Config**:
- Distance metric: Cosine
- Vector size: Configurable (384 for MiniLM, 1536 for OpenAI)
- On-disk storage with in-memory index for payload filters
- Payload indexes on: `organization_id`, `workspace_id`, `document_id`

---

## Relationships

```
organizations (1) ──── (*) conversations
users (1) ──── (*) conversations
workspaces (1) ──── (*) conversations
conversations (1) ──── (*) conversation_messages

files (1) ──── (0..1) embedding_jobs
files (1) ──── (*) document_chunks [in Qdrant]
organizations (1) ──── (*) embedding_jobs
workspaces (1) ──── (*) embedding_jobs
```

## State Transitions

### Conversation Status
```
ACTIVE → ARCHIVED (user archives)
ARCHIVED → ACTIVE (user reopens)
```

### EmbeddingJob Status
```
PENDING → PROCESSING (worker picks up)
PROCESSING → COMPLETED (success)
PROCESSING → FAILED (error, max retries exceeded)
FAILED → PENDING (manual retry triggered)
```

## Flyway Migration: V009__ai_qa_tables.sql

```sql
CREATE TABLE conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status ENUM('ACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    message_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversation_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_conversation_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_conversation_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    INDEX idx_conversation_user_workspace (user_id, workspace_id, status),
    INDEX idx_conversation_org (organization_id),
    INDEX idx_conversation_updated (updated_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    conversation_id BIGINT NOT NULL,
    role ENUM('USER','ASSISTANT') NOT NULL,
    content TEXT NOT NULL,
    citations JSON NULL,
    token_count INT NULL,
    model_used VARCHAR(100) NULL,
    retrieval_chunks JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    INDEX idx_message_conversation (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE embedding_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    status ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
    chunk_count INT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    vector_dimension INT NOT NULL,
    error_message TEXT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_embedding_job_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_embedding_job_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_embedding_job_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    INDEX idx_embedding_job_file (file_id),
    INDEX idx_embedding_job_status (organization_id, status),
    INDEX idx_embedding_job_workspace (workspace_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
