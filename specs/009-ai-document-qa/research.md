# Research: AI Document Q&A System

**Feature**: 009-ai-document-qa  
**Date**: 2026-05-06  
**Purpose**: Resolve technology choices and document best practices for RAG implementation

---

## 1. Vector Database: Qdrant

**Decision**: Use Qdrant as the vector database for storing document chunk embeddings.

**Rationale**:
- Purpose-built for vector similarity search with excellent performance at scale
- Native Docker support — aligns with existing Docker Compose infrastructure
- REST and gRPC APIs with official Java and Python clients
- Supports payload filtering (essential for RBAC-based document access filtering)
- Built-in support for multiple named vectors per point (useful for future multi-model embeddings)
- Supports collection-level and point-level metadata for document lineage tracking

**Alternatives Considered**:
- **Pinecone**: Managed service, not self-hostable — doesn't fit Docker Compose local dev model
- **Weaviate**: Heavier footprint, more opinionated schema — unnecessary complexity
- **pgvector (PostgreSQL extension)**: Would require changing from MySQL; limited filtering capabilities
- **OpenSearch kNN**: Already in stack but vector search is secondary feature, less optimized for RAG workloads

**Configuration**:
- Docker image: `qdrant/qdrant:v1.9.0`
- Port: 6333 (REST), 6334 (gRPC)
- Collection: `document_chunks` with cosine similarity
- Vector dimension: 384 (all-MiniLM-L6-v2) or 1536 (OpenAI ada-002), configurable
- Payload indexes on: `organization_id`, `workspace_id`, `document_id`, `user_accessible_ids`

---

## 2. Embedding Model Strategy

**Decision**: Default to `all-MiniLM-L6-v2` (local, open-source) with configurable fallback to OpenAI `text-embedding-3-small`.

**Rationale**:
- all-MiniLM-L6-v2: 384 dimensions, fast inference (~14ms/sentence on CPU), no API costs, runs locally in Docker
- Good balance of quality vs. speed for document retrieval tasks
- OpenAI option available for users who want higher quality and have API access
- Model choice stored per-organization in config, allowing future per-workspace customization

**Alternatives Considered**:
- **OpenAI text-embedding-ada-002**: Higher quality but requires API key, adds latency and cost
- **BGE-large-en**: Better retrieval quality but 1024 dimensions, slower inference
- **Cohere embed-v3**: Excellent quality but external dependency

**Implementation**:
- Python worker uses `sentence-transformers` library for local embedding
- Model downloaded at container build time (cached in Docker layer)
- Configurable via `EMBEDDING_MODEL` environment variable
- Batch processing: embed chunks in batches of 32 for throughput

---

## 3. Document Chunking Strategy

**Decision**: Use recursive character text splitting with semantic awareness (paragraph boundaries, headings).

**Rationale**:
- Recursive splitting respects natural document structure (paragraphs, sections)
- Chunk size of 512 tokens with 50-token overlap balances context vs. precision
- Metadata preserved per chunk: document_id, page_number, section_heading, position_index
- Allows citation references to point to specific document locations

**Implementation Details**:
- Primary separators: `\n\n` (paragraphs), `\n` (lines), `. ` (sentences)
- Chunk size: 512 tokens (measured by tiktoken for consistency)
- Overlap: 50 tokens (ensures context continuity between adjacent chunks)
- Each chunk stores: text, document_id, page_number, char_start, char_end, section_title
- PDF: Extract text page-by-page using existing worker text extraction
- Word/PPT: Convert to text preserving section boundaries
- Minimum chunk size: 50 tokens (skip very small fragments)

**Alternatives Considered**:
- **Fixed-size splitting**: Simpler but breaks mid-sentence, poor retrieval quality
- **Semantic chunking (embedding-based)**: Higher quality boundaries but significantly slower processing
- **Document-specific parsers**: Too complex for MVP, can iterate later

---

## 4. LLM Provider for Answer Generation

**Decision**: Use OpenAI GPT-4o-mini as default LLM via LangChain4j in the Spring Boot backend, with configurable provider.

**Rationale**:
- LangChain4j provides a unified Java API for multiple LLM providers
- GPT-4o-mini offers excellent quality/cost ratio for RAG tasks
- Structured prompting ensures answers stay grounded in retrieved context
- Provider abstraction allows switching to local models (Ollama) or other APIs
- Spring Boot integration keeps RAG orchestration close to the permission checks

**Alternatives Considered**:
- **Python LangChain for orchestration**: Would add another service; keeping in Java simplifies deployment
- **Direct OpenAI API calls**: Less flexible, harder to swap providers
- **Ollama (local)**: Good for offline/privacy but lower quality for complex questions; offered as config option

**Implementation**:
- LangChain4j dependency in Spring Boot backend
- `application.yml` config for API key, model name, temperature, max_tokens
- System prompt enforces: "Answer ONLY from the provided context. If information is not in the context, say so."
- Temperature: 0.1 (low creativity, high factuality)
- Max tokens: 1024 for answers, 2048 for summaries
- Context window: Top-5 retrieved chunks passed as context

---

## 5. RAG Pipeline Architecture

**Decision**: Hybrid architecture — embedding in Python worker (async), retrieval + generation in Spring Boot (sync).

**Rationale**:
- Embedding is CPU/GPU-intensive → async processing via existing Redis queue
- Retrieval + generation is request-driven → sync in API request path
- Leverages existing infrastructure: Redis queue (worker), Spring Boot (API)
- Permission filtering happens at retrieval time in Java (close to RBAC logic)

**Pipeline Flow**:
1. **Ingest** (async): Upload → Redis queue → Python worker → chunk → embed → store in Qdrant
2. **Query** (sync): User question → Spring Boot → embed question → Qdrant search (with RBAC filter) → LLM generation → response with citations
3. **Summarize** (sync): Select docs → retrieve all chunks for docs → LLM summarization → response

**Permission Filtering**:
- At query time, resolve user's accessible document IDs (from existing RBAC)
- Pass as Qdrant payload filter: `document_id IN [accessible_ids]`
- Cache accessible document list in Redis (5-min TTL, same as existing permission cache)

---

## 6. Conversation Context Management

**Decision**: Store conversation history in MySQL, pass recent messages as LLM context window.

**Rationale**:
- MySQL persists conversations for history feature
- Last 10 messages passed to LLM as conversation context for follow-ups
- Redis caches active conversation state (30-min TTL) to reduce DB reads
- Conversation scoped to user + workspace (RBAC boundary)

**Implementation**:
- On follow-up: include previous Q&A pairs in system prompt
- Sliding window: last 10 messages (5 exchanges) — keeps within token limits
- If conversation exceeds window, summarize older context into a system message
- New conversation: no carry-over from previous conversations

---

## 7. Citation Implementation

**Decision**: Citations reference chunk metadata (document_id, page_number, char_start, char_end) with highlighted passage.

**Rationale**:
- Chunks already store position metadata from chunking phase
- Frontend can navigate to document preview at the cited page
- Highlighted text excerpt gives immediate verification without navigating away
- Multiple citations per answer supported (LLM prompted to cite each source)

**Implementation**:
- LLM prompted to wrap cited text in `[cite:N]` markers
- Backend post-processes to match markers with retrieved chunk metadata
- Response includes structured citations array: `{documentId, documentName, pageNumber, excerpt, chunkId}`
- Frontend renders citation badges, clicking opens preview at cited location

---

## 8. Docker Infrastructure Addition

**Decision**: Add Qdrant service to existing Docker Compose.

**Configuration**:
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

---

## 9. Error Handling & Edge Cases

**Decision**: Graceful degradation with clear user feedback.

- **LLM unavailable**: Return error "AI service temporarily unavailable" with retry suggestion
- **Qdrant unavailable**: Return error "Search service unavailable"
- **No relevant chunks found**: LLM not called; return "No relevant information found in your documents"
- **Document not yet indexed**: Show "Document is being processed" status
- **Rate limiting**: 10 questions/minute per user (configurable)
- **Large document sets**: Batch embedding (32 chunks per Qdrant upsert), pagination for conversation history
