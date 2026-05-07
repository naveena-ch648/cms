# Implementation Plan: AI Document Q&A System

**Branch**: `009-ai-document-qa` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-ai-document-qa/spec.md`

**Note**: This plan covers Phase 0 (Research) and Phase 1 (Design & Contracts). Task generation is done separately via `/speckit.tasks`.

## Summary

Build a RAG-based (Retrieval-Augmented Generation) document Q&A system that allows users to ask natural language questions about their documents and receive evidence-grounded answers with citations. The system uses Qdrant as a vector database for semantic search, Python workers for document chunking and embedding generation, a Spring Boot RAG orchestration service (with LangChain4j), and a React citation viewer UI.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend + RAG orchestration), Python 3.11 (embedding workers), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Boot 3.3.5, LangChain4j 0.35+, Qdrant Java Client, OpenAI API (configurable LLM provider); Python: sentence-transformers, qdrant-client, redis-py, boto3, langchain; React 18, Axios 1.7.7, Vite 6  
**Storage**: MySQL 8.0 (conversations, messages, embedding jobs), Qdrant (vector embeddings), MinIO (document content), Redis 7 (job queue, conversation cache)  
**Testing**: JUnit 5 + Mockito (backend), pytest (workers), React Testing Library (frontend)  
**Target Platform**: Docker Compose (Linux containers)  
**Project Type**: Web service (multi-service)  
**Performance Goals**: Q&A response < 5s, embedding indexing within 5 min of upload, 50 concurrent sessions  
**Constraints**: Answers must be strictly grounded in retrieved evidence (no hallucination), < 5s for answer generation, respect RBAC permissions on document access  
**Scale/Scope**: Same user base as existing CMS, documents already in MinIO, extend existing worker pipeline

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular service separation: embedding worker, RAG service, API controller, React UI |
| II. Testing Standards | ✅ PASS | Unit tests for chunking, mock Qdrant/LLM in integration tests, 80% coverage target |
| III. UX Consistency | ✅ PASS | Follows existing design system, loading/error states for Q&A, structured API responses |
| IV. Performance & Scalability | ✅ PASS | Async embedding via workers, Redis queue, Qdrant horizontal scaling, < 5s target |
| V. Reliability & Fault Tolerance | ✅ PASS | Redis queue with DLQ for embedding jobs, retry on LLM failures, idempotent processing |
| VI. Security & Compliance | ✅ PASS | RBAC-filtered retrieval, no external data leakage, audit logging for Q&A queries |
| VII. Data & AI Governance | ✅ PASS | Document lineage tracked, AI outputs traceable to source chunks, embedding versioning |
| VIII. Observability | ✅ PASS | Logging for embedding pipeline, Q&A latency metrics, error rates |
| IX. Developer Experience | ✅ PASS | Docker Compose for Qdrant, consistent project structure, documented APIs |
| X. Continuous Improvement | ✅ PASS | Configurable LLM/embedding model, re-embedding support |

**Gate Result**: ✅ ALL PASS — Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/009-ai-document-qa/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── qa-api.md        # Q&A REST API contract
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   └── QAController.java           # Q&A REST endpoints
│   ├── entity/
│   │   ├── Conversation.java           # Q&A conversation entity
│   │   ├── ConversationMessage.java    # Individual message entity
│   │   └── EmbeddingJob.java           # Embedding pipeline tracking
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   ├── ConversationMessageRepository.java
│   │   └── EmbeddingJobRepository.java
│   ├── service/
│   │   ├── QAService.java              # RAG orchestration
│   │   ├── VectorSearchService.java    # Qdrant interaction
│   │   ├── EmbeddingJobService.java    # Job dispatch
│   │   └── ConversationService.java    # Conversation CRUD
│   ├── dto/qa/
│   │   ├── AskRequest.java
│   │   ├── AskResponse.java
│   │   ├── CitationDto.java
│   │   ├── ConversationDto.java
│   │   ├── MessageDto.java
│   │   └── SummarizeRequest.java
│   └── config/
│       ├── QdrantConfig.java           # Qdrant client bean
│       └── LLMConfig.java              # LLM provider config
│
├── src/main/resources/
│   └── db/migration/
│       └── V009__ai_qa_tables.sql      # Flyway migration

frontend/
├── src/
│   ├── api/
│   │   └── qa.ts                       # Q&A API client
│   ├── components/qa/
│   │   ├── QAPanel.tsx                 # Main Q&A interface
│   │   ├── MessageBubble.tsx           # Chat message display
│   │   ├── CitationLink.tsx            # Clickable citation
│   │   ├── CitationPanel.tsx           # Source viewer panel
│   │   ├── ConversationList.tsx        # History sidebar
│   │   └── SummarizeDialog.tsx         # Summarization modal
│   └── pages/
│       └── QAPage.tsx                  # Q&A route page

worker/
├── processors/
│   ├── embeddings.py                   # Chunking + embedding generator
│   └── embedding_config.py            # Model configuration
```

**Structure Decision**: Extends existing multi-service architecture (backend/frontend/worker). New entities added to existing Spring Boot backend. Worker extended with embedding processor. New React components under `components/qa/`. Qdrant added as a new Docker service.

## Complexity Tracking

No constitution violations requiring justification.
