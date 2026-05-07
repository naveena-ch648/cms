# Quickstart: AI Document Q&A System

**Feature**: 009-ai-document-qa  
**Date**: 2026-05-06

---

## Prerequisites

- Existing CMS platform running (Steps 1–8 complete)
- Docker Compose with MySQL, Redis, MinIO, OpenSearch already operational
- OpenAI API key (or Ollama for local LLM)

## New Infrastructure

### Add Qdrant to Docker Compose

Add the following service to `docker/docker-compose.yml`:

```yaml
qdrant:
  image: qdrant/qdrant:v1.9.0
  container_name: cms-qdrant
  ports:
    - "6333:6333"
    - "6334:6334"
  volumes:
    - qdrant-data:/qdrant/storage
  environment:
    - QDRANT__SERVICE__GRPC_PORT=6334
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:6333/healthz || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 5
```

Add `qdrant-data:` to the `volumes:` section.

### Environment Variables

**Backend** (add to `docker-compose.yml` backend service):
```yaml
QDRANT_HOST: qdrant
QDRANT_PORT: 6333
LLM_PROVIDER: openai
OPENAI_API_KEY: ${OPENAI_API_KEY}
OPENAI_MODEL: gpt-4o-mini
EMBEDDING_MODEL: all-MiniLM-L6-v2
EMBEDDING_DIMENSION: 384
```

**Worker** (add to `docker-compose.yml` worker service):
```yaml
QDRANT_HOST: qdrant
QDRANT_PORT: 6333
EMBEDDING_MODEL: all-MiniLM-L6-v2
EMBEDDING_DIMENSION: 384
EMBEDDING_BATCH_SIZE: 32
EMBEDDING_QUEUE: embedding:process
EMBEDDING_DLQ: embedding:process:dlq
```

## Development Setup

### Backend Dependencies

Add to `backend/pom.xml`:
```xml
<!-- LangChain4j for LLM orchestration -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.35.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.35.0</version>
</dependency>

<!-- Qdrant Java Client -->
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>client</artifactId>
    <version>1.9.0</version>
</dependency>
```

### Worker Dependencies

Add to `worker/requirements.txt`:
```
sentence-transformers==2.7.0
qdrant-client==1.9.0
tiktoken==0.7.0
langchain-text-splitters==0.2.0
```

### Database Migration

Run Flyway migration `V009__ai_qa_tables.sql` which creates:
- `conversations` table
- `conversation_messages` table
- `embedding_jobs` table

### Qdrant Collection Setup

The backend auto-creates the collection on startup if it doesn't exist:
- Collection name: `document_chunks`
- Vector size: from `EMBEDDING_DIMENSION` env var
- Distance: Cosine
- Payload indexes: `organization_id`, `workspace_id`, `document_id`

## Running

```bash
# Start all services including Qdrant
docker compose -f docker/docker-compose.yml up --build

# Verify Qdrant is healthy
curl http://localhost:6333/healthz

# Verify collection exists (after backend starts)
curl http://localhost:6333/collections/document_chunks
```

## Testing the Feature

1. **Upload a document** via the existing file upload UI
2. **Wait for embedding** — check status via `GET /api/v1/qa/embedding-status/{fileId}`
3. **Ask a question** via `POST /api/v1/qa/ask`:
   ```bash
   curl -X POST http://localhost:8080/api/v1/qa/ask \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"question": "What is this document about?", "workspaceId": "<workspace-uuid>"}'
   ```
4. **Verify citations** — response includes document name, page number, and excerpt
5. **Follow-up** — use the returned `conversationId` in the next request

## Key Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| `LLM_PROVIDER` | `openai` | LLM provider: `openai`, `ollama` |
| `OPENAI_API_KEY` | — | Required if provider is `openai` |
| `OPENAI_MODEL` | `gpt-4o-mini` | Model for answer generation |
| `EMBEDDING_MODEL` | `all-MiniLM-L6-v2` | Sentence transformer model |
| `EMBEDDING_DIMENSION` | `384` | Must match model output dimension |
| `EMBEDDING_BATCH_SIZE` | `32` | Chunks per Qdrant upsert batch |
| `QA_MAX_CHUNKS` | `5` | Default chunks retrieved per question |
| `QA_RATE_LIMIT` | `10` | Questions per minute per user |
| `QA_CONVERSATION_WINDOW` | `10` | Messages in context window |
