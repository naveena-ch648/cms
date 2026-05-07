# Tasks: AI Document Q&A System

**Input**: Design documents from `/specs/009-ai-document-qa/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/qa-api.md, quickstart.md

**Tests**: Not explicitly requested in the feature specification. Tests are omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Qdrant to Docker infrastructure, add backend/worker dependencies, create Flyway migration

- [x] T001 Add Qdrant service and volume to docker/docker-compose.yml
- [x] T002 [P] Add LangChain4j, Qdrant client, and OpenAI dependencies to backend/pom.xml
- [x] T003 [P] Add sentence-transformers, qdrant-client, tiktoken dependencies to worker/requirements.txt
- [x] T004 [P] Add Q&A environment variables to backend service in docker/docker-compose.yml
- [x] T005 [P] Add embedding environment variables to worker service in docker/docker-compose.yml
- [x] T006 Create Flyway migration backend/src/main/resources/db/migration/V009__ai_qa_tables.sql

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities, repositories, configuration beans, and worker embedding processor that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T007 Create Conversation entity in backend/src/main/java/com/cms/entity/Conversation.java
- [x] T008 [P] Create ConversationMessage entity in backend/src/main/java/com/cms/entity/ConversationMessage.java
- [x] T009 [P] Create EmbeddingJob entity in backend/src/main/java/com/cms/entity/EmbeddingJob.java
- [x] T010 [P] Create ConversationRepository in backend/src/main/java/com/cms/repository/ConversationRepository.java
- [x] T011 [P] Create ConversationMessageRepository in backend/src/main/java/com/cms/repository/ConversationMessageRepository.java
- [x] T012 [P] Create EmbeddingJobRepository in backend/src/main/java/com/cms/repository/EmbeddingJobRepository.java
- [x] T013 Create QdrantConfig bean in backend/src/main/java/com/cms/config/QdrantConfig.java
- [x] T014 [P] Create LLMConfig bean in backend/src/main/java/com/cms/config/LLMConfig.java
- [x] T015 [P] Add Qdrant and LLM properties to backend/src/main/resources/application.yml
- [x] T016 Create VectorSearchService in backend/src/main/java/com/cms/service/VectorSearchService.java
- [x] T017 Create embedding_config module in worker/processors/embedding_config.py
- [x] T018 Create embeddings processor (chunking + embedding + Qdrant upsert) in worker/processors/embeddings.py
- [x] T019 Register embedding job handler in worker/worker.py

**Checkpoint**: Foundation ready — Qdrant connected, entities mapped, embedding pipeline operational, user story implementation can begin

---

## Phase 3: User Story 1 — Ask a Question About Documents (Priority: P1) 🎯 MVP

**Goal**: Users submit a natural language question and receive an evidence-grounded answer with citations from their accessible documents.

**Independent Test**: Upload 3+ documents, wait for embedding, ask a factual question whose answer exists in one document, verify the response contains correct information with a citation pointing to the correct document and page.

### DTOs for User Story 1

- [ ] T020 [P] [US1] Create AskRequest DTO in backend/src/main/java/com/cms/dto/qa/AskRequest.java
- [ ] T021 [P] [US1] Create AskResponse DTO in backend/src/main/java/com/cms/dto/qa/AskResponse.java
- [ ] T022 [P] [US1] Create CitationDto in backend/src/main/java/com/cms/dto/qa/CitationDto.java

### Services for User Story 1

- [ ] T023 [US1] Create EmbeddingJobService (dispatch jobs, check status) in backend/src/main/java/com/cms/service/EmbeddingJobService.java
- [ ] T024 [US1] Create QAService (RAG orchestration: embed question → retrieve chunks → LLM generation → citations) in backend/src/main/java/com/cms/service/QAService.java
- [ ] T025 [US1] Create ConversationService (create conversation, add messages) in backend/src/main/java/com/cms/service/ConversationService.java

### Controller for User Story 1

- [x] T026 [US1] Create QAController with POST /api/v1/qa/ask endpoint in backend/src/main/java/com/cms/controller/QAController.java
- [x] T027 [US1] Add embedding trigger on file upload completion in backend/src/main/java/com/cms/service/FileService.java

### Frontend for User Story 1

- [x] T028 [P] [US1] Create Q&A API client in frontend/src/api/qa.ts
- [x] T029 [US1] Create QAPanel component (chat input, message list, loading state) in frontend/src/components/qa/QAPanel.tsx
- [x] T030 [US1] Create MessageBubble component (user/assistant messages) in frontend/src/components/qa/MessageBubble.tsx
- [x] T031 [US1] Create QAPage and add route in frontend/src/pages/QAPage.tsx

**Checkpoint**: Users can ask questions and get answers with citations. Core RAG pipeline is functional end-to-end.

---

## Phase 4: User Story 2 — View Citations and Navigate to Source (Priority: P1)

**Goal**: Users click citation references in answers to navigate to the exact passage in the source document preview.

**Independent Test**: Ask a question, receive a cited answer, click a citation, verify the system opens the document preview at the correct page with the relevant passage visible.

### Implementation for User Story 2

- [x] T032 [P] [US2] Create CitationLink component (clickable inline citation marker) in frontend/src/components/qa/CitationLink.tsx
- [x] T033 [US2] Create CitationPanel component (source viewer with highlighted excerpt) in frontend/src/components/qa/CitationPanel.tsx
- [x] T034 [US2] Integrate CitationPanel with existing FilePreview component to navigate to page in frontend/src/components/qa/CitationPanel.tsx
- [x] T035 [US2] Update MessageBubble to render CitationLink components within answer text in frontend/src/components/qa/MessageBubble.tsx

**Checkpoint**: Citations are clickable and navigate to source document at the correct location.

---

## Phase 5: User Story 3 — Document Summarization (Priority: P2)

**Goal**: Users select documents and request AI-generated summaries with section citations.

**Independent Test**: Select a document, request a summary, verify output captures key themes with section references.

### DTOs for User Story 3

- [x] T036 [P] [US3] Create SummarizeRequest DTO in backend/src/main/java/com/cms/dto/qa/SummarizeRequest.java
- [x] T037 [P] [US3] Create SummarizeResponse DTO in backend/src/main/java/com/cms/dto/qa/SummarizeResponse.java

### Backend for User Story 3

- [x] T038 [US3] Add summarize method to QAService in backend/src/main/java/com/cms/service/QAService.java
- [x] T039 [US3] Add POST /api/v1/qa/summarize endpoint to QAController in backend/src/main/java/com/cms/controller/QAController.java

### Frontend for User Story 3

- [x] T040 [US3] Create SummarizeDialog component (document selection, length option, result display) in frontend/src/components/qa/SummarizeDialog.tsx
- [x] T041 [US3] Add summarize API method to frontend/src/api/qa.ts
- [x] T042 [US3] Integrate SummarizeDialog trigger into file list context menu in frontend/src/components/FileList.tsx

**Checkpoint**: Users can select documents and generate summaries with citations.

---

## Phase 6: User Story 4 — Follow-up Questions (Priority: P2)

**Goal**: Users ask follow-up questions within a conversation, and the system maintains conversational context.

**Independent Test**: Ask an initial question, then ask a follow-up using a pronoun ("Tell me more about that"), verify the system resolves context and provides a relevant answer.

### Backend for User Story 4

- [x] T043 [US4] Extend QAService.ask() to include conversation history (last 10 messages) in LLM prompt in backend/src/main/java/com/cms/service/QAService.java
- [x] T044 [US4] Add Redis cache for active conversation state (30-min TTL) in backend/src/main/java/com/cms/service/ConversationService.java

### Frontend for User Story 4

- [x] T045 [US4] Update QAPanel to pass conversationId on follow-up questions in frontend/src/components/qa/QAPanel.tsx
- [x] T046 [US4] Display conversation thread with all previous exchanges in QAPanel in frontend/src/components/qa/QAPanel.tsx

**Checkpoint**: Users can have multi-turn conversations with context maintained across exchanges.

---

## Phase 7: User Story 5 — Conversation History (Priority: P3)

**Goal**: Users view, reopen, and continue past Q&A conversations from a history panel.

**Independent Test**: Have a Q&A conversation, navigate away, return to history panel, verify conversation appears and can be continued.

### DTOs for User Story 5

- [x] T047 [P] [US5] Create ConversationDto in backend/src/main/java/com/cms/dto/qa/ConversationDto.java
- [x] T048 [P] [US5] Create MessageDto in backend/src/main/java/com/cms/dto/qa/MessageDto.java

### Backend for User Story 5

- [x] T049 [US5] Add GET /api/v1/qa/conversations endpoint (list with pagination, search) to QAController in backend/src/main/java/com/cms/controller/QAController.java
- [x] T050 [US5] Add GET /api/v1/qa/conversations/{id}/messages endpoint to QAController in backend/src/main/java/com/cms/controller/QAController.java
- [x] T051 [US5] Add DELETE /api/v1/qa/conversations/{id} endpoint (archive/delete) to QAController in backend/src/main/java/com/cms/controller/QAController.java

### Frontend for User Story 5

- [x] T052 [US5] Create ConversationList component (history sidebar with search) in frontend/src/components/qa/ConversationList.tsx
- [x] T053 [US5] Add conversation list and message history API methods to frontend/src/api/qa.ts
- [x] T054 [US5] Integrate ConversationList into QAPage with conversation switching in frontend/src/pages/QAPage.tsx

**Checkpoint**: Users can view, search, reopen, and continue past conversations.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Embedding status visibility, re-indexing, error handling, and performance optimization

- [x] T055 [P] Add GET /api/v1/qa/embedding-status/{fileId} endpoint to QAController in backend/src/main/java/com/cms/controller/QAController.java
- [x] T056 [P] Add POST /api/v1/qa/reindex/{fileId} endpoint to QAController in backend/src/main/java/com/cms/controller/QAController.java
- [x] T057 [P] Add re-embedding trigger when file version is updated in backend/src/main/java/com/cms/service/FileService.java
- [x] T058 [P] Add rate limiting (10 ask/min, 5 summarize/min per user) to QAController in backend/src/main/java/com/cms/controller/QAController.java
- [x] T059 [P] Add embedding status indicator to file detail panel in frontend/src/components/FileDetailPanel.tsx
- [x] T060 Handle Qdrant/LLM unavailability with user-friendly error messages in QAPanel in frontend/src/components/qa/QAPanel.tsx
- [x] T061 Add DLQ handling for failed embedding jobs (max 3 retries) in worker/processors/embeddings.py
- [x] T062 Run quickstart.md validation — verify Qdrant container starts, migration runs, embedding pipeline processes a test document

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — core RAG pipeline
- **User Story 2 (Phase 4)**: Depends on Phase 3 (needs answers with citations to exist)
- **User Story 3 (Phase 5)**: Depends on Phase 2 — can start in parallel with US1 (uses same VectorSearchService)
- **User Story 4 (Phase 6)**: Depends on Phase 3 (extends ask functionality)
- **User Story 5 (Phase 7)**: Depends on Phase 2 — can start in parallel with US1 (conversation CRUD is independent)
- **Polish (Phase 8)**: Depends on Phase 3 minimum; ideally after all stories

### User Story Dependencies

```
Phase 1 (Setup)
    │
    ▼
Phase 2 (Foundation) ──── BLOCKS ALL ────┐
    │                                     │
    ▼                                     ▼
Phase 3 (US1: Ask Q&A) ◄─── required ── Phase 4 (US2: Citations)
    │                                     
    ├── Phase 5 (US3: Summarize) ← can parallel after Phase 2
    │                                     
    ├── Phase 6 (US4: Follow-ups) ← requires Phase 3
    │                                     
    └── Phase 7 (US5: History) ← can parallel after Phase 2
              │
              ▼
        Phase 8 (Polish)
```

### Within Each User Story

- DTOs before Services
- Services before Controllers
- Backend before Frontend (API must exist for frontend to consume)
- Core implementation before integration points

### Parallel Opportunities

**Phase 1**: T002, T003, T004, T005 can all run in parallel (different files)
**Phase 2**: T008, T009 parallel; T010, T011, T012 parallel; T013, T014, T015 parallel; T017, T018 parallel after T017
**Phase 3**: T020, T021, T022 parallel (DTOs); T028 parallel with backend tasks
**Phase 4**: T032 parallel with other US2 frontend tasks
**Phase 5**: T036, T037 parallel; T040, T041 parallel
**Phase 7**: T047, T048 parallel
**Phase 8**: All tasks marked [P] can run in parallel

---

## Implementation Strategy

### MVP Scope (Recommended First Delivery)

**Phases 1 → 2 → 3**: Setup + Foundation + User Story 1 (Ask Questions)

This delivers the core value: users can ask questions and get cited answers. Total: 31 tasks (T001–T031).

### Incremental Delivery After MVP

1. **Phase 4** (US2: Citations UX) — Enhances MVP with click-to-navigate citations
2. **Phase 5** (US3: Summarize) — Adds summarization capability  
3. **Phase 6** (US4: Follow-ups) — Adds conversational context
4. **Phase 7** (US5: History) — Adds persistence and history browsing
5. **Phase 8** (Polish) — Production readiness (rate limits, error handling, re-indexing)

### Key Technical Notes

- **Embedding pipeline** (T017–T019): Extends existing worker — new `embeddings` job type in Redis queue
- **RBAC filtering** (T024): Retrieve user's accessible file IDs from existing permission service, pass as Qdrant payload filter
- **LLM prompt** (T024): System prompt enforces "Answer ONLY from provided context" to prevent hallucination
- **Conversation context** (T043): Last 10 messages (sliding window) passed to LLM for follow-ups
- **Auto-embed on upload** (T027): Hook into existing file upload completion event to dispatch embedding job
