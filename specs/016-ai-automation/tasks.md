# Tasks: AI Automation

**Input**: Design documents from `/specs/016-ai-automation/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema, dependencies, queue configuration, and AI config

- [X] T001 Create Flyway migration V23__ai_automation.sql in backend/src/main/resources/db/migration/V23__ai_automation.sql
- [X] T002 [P] Add OpenAI Python client dependency to worker/requirements.txt (openai>=1.30.0)
- [X] T003 [P] Add AI configuration properties to worker/config.py (OPENAI_API_KEY, AI_QUEUE, AI_DLQ, confidence threshold)
- [X] T004 [P] Create frontend API client for AI automation in frontend/src/api/ai.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entity, repository, DTOs, and service skeleton that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create AIJob entity in backend/src/main/java/com/cms/entity/AIJob.java
- [X] T006 [P] Create AIJobRepository in backend/src/main/java/com/cms/repository/AIJobRepository.java
- [X] T007 [P] Create AI DTO classes (AIJobResponse, AISuggestionsResponse, AcceptTagsRequest, AcceptClassificationRequest, RegenerateRequest, AIConfigRequest, AIConfigResponse, ApplyWorkflowRequest) in backend/src/main/java/com/cms/dto/ai/
- [X] T008 Implement AIAutomationService (enqueue jobs on upload, get suggestions, accept/reject, regenerate, config CRUD) in backend/src/main/java/com/cms/service/AIAutomationService.java
- [X] T009 Implement AIAutomationController (suggestions, accept-tags, accept-classification, regenerate, jobs, apply-workflow, config) in backend/src/main/java/com/cms/controller/AIAutomationController.java
- [X] T010 Add AI job dispatch hook to file upload flow (enqueue AI jobs after file processing completes) in backend/src/main/java/com/cms/service/FileService.java
- [X] T011 [P] Create AI worker dispatcher in worker/processors/ai_dispatcher.py (consume ai:process queue, route by type, retry logic)

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Auto-Tagging & Classification (Priority: P1) 🎯 MVP

**Goal**: Uploaded documents automatically receive suggested tags and a document category. Users can accept or reject suggestions.

**Independent Test**: Upload a document → verify suggested tags and category appear in file detail panel within 60 seconds → accept/reject suggestions

### Implementation for User Story 1

- [X] T012 [US1] Implement ai_tagger.py processor (extract text from MinIO, call LLM for tags + classification, return structured JSON) in worker/processors/ai_tagger.py
- [X] T013 [US1] Add LLM prompt templates for tagging and classification (system prompt, few-shot examples, JSON schema enforcement) in worker/processors/ai_prompts.py
- [X] T014 [P] [US1] Create AISuggestionsPanel component (displays tags with accept/reject, classification with confidence, loading states) in frontend/src/components/ai/AISuggestionsPanel.tsx
- [X] T015 [US1] Integrate AISuggestionsPanel into FileDetailPanel in frontend/src/components/FileDetailPanel.tsx

**Checkpoint**: User Story 1 fully functional — auto-tagging and classification working end-to-end

---

## Phase 4: User Story 2 — Document Summarization (Priority: P1)

**Goal**: Documents receive AI-generated summaries displayed in the file detail panel. Users can regenerate summaries on demand.

**Independent Test**: Upload a multi-page document → verify summary appears in file detail panel → click Regenerate → verify new summary replaces old one

### Implementation for User Story 2

- [X] T016 [US2] Implement ai_summarizer.py processor (extract text, call LLM for summarization, handle large documents via chunked summarization) in worker/processors/ai_summarizer.py
- [X] T017 [US2] Add summary display section to AISuggestionsPanel (summary text, word count, key topics, regenerate button) in frontend/src/components/ai/AISuggestionsPanel.tsx

**Checkpoint**: User Story 2 fully functional — document summarization working

---

## Phase 5: User Story 3 — Duplicate Detection (Priority: P2)

**Goal**: System detects exact and near-duplicate files and notifies users with links to existing files.

**Independent Test**: Upload a file → upload same file again → verify duplicate warning with link to original

### Implementation for User Story 3

- [X] T018 [US3] Implement ai_duplicates.py processor (SHA-256 exact match via DB, Qdrant cosine similarity for near-duplicates, return matches with similarity scores) in worker/processors/ai_duplicates.py
- [X] T019 [P] [US3] Create DuplicateWarning component (shows exact/near-duplicate matches with file links and similarity percentage) in frontend/src/components/ai/DuplicateWarning.tsx
- [X] T020 [US3] Integrate DuplicateWarning into FileDetailPanel (show after upload when duplicates detected) in frontend/src/components/FileDetailPanel.tsx

**Checkpoint**: User Story 3 fully functional — duplicate detection working

---

## Phase 6: User Story 4 — Sensitive Data Detection (Priority: P2)

**Goal**: System scans documents for PII and sensitive data patterns, applies sensitivity labels, and suggests sharing restrictions.

**Independent Test**: Upload document with credit card numbers → verify sensitivity badge appears with detection details → verify sharing restriction suggestion

### Implementation for User Story 4

- [X] T021 [US4] Implement ai_sensitive.py processor (regex patterns for SSN, credit cards, emails, phones, passport; LLM for contextual detection; severity classification) in worker/processors/ai_sensitive.py
- [X] T022 [P] [US4] Create SensitivityBadge component (severity indicator, detection list, sharing restriction warning) in frontend/src/components/ai/SensitivityBadge.tsx
- [X] T023 [US4] Integrate SensitivityBadge into FileDetailPanel and FileList (badge on file cards for flagged files) in frontend/src/components/FileDetailPanel.tsx

**Checkpoint**: User Story 4 fully functional — sensitive data detection working

---

## Phase 7: User Story 5 — Workflow Recommendations (Priority: P3)

**Goal**: System suggests workflow actions based on document classification and organizational workflow mappings.

**Independent Test**: Configure workflow mapping (Contract → Legal Review) → upload document classified as Contract → verify workflow recommendation appears → click Apply

### Implementation for User Story 5

- [X] T024 [US5] Implement ai_workflow.py processor (read org ai_config workflow_mappings, match classification to configured workflows, return recommendation) in worker/processors/ai_workflow.py
- [X] T025 [US5] Add workflow recommendation section to AISuggestionsPanel (recommendation card with Apply/Dismiss buttons) in frontend/src/components/ai/AISuggestionsPanel.tsx

**Checkpoint**: User Story 5 fully functional — workflow recommendations working

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Admin configuration, worker integration, Docker updates, and final validation

- [X] T026 [P] Add AI configuration section to Admin console (enable/disable features, confidence threshold, sensitivity patterns, workflow mappings) in frontend/src/pages/AdminPage.tsx
- [X] T027 [P] Update worker/worker.py to spawn AI dispatcher thread (import and start ai_dispatcher alongside existing workers)
- [X] T028 [P] Update docker/docker-compose.yml with AI environment variables (OPENAI_API_KEY in worker service)
- [X] T029 Add ai:process queue to existing file upload flow trigger in backend (ensure jobs enqueued after file:process completes)
- [X] T030 Run build verification (mvn compile + tsc --noEmit) and validate full flow per quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–7)**: All depend on Foundational phase completion
  - US1 (Auto-Tagging & Classification) and US2 (Summarization) can proceed in parallel
  - US3 (Duplicate Detection) can proceed independently (uses Qdrant, not LLM tagging)
  - US4 (Sensitive Data Detection) can proceed independently (regex-based)
  - US5 (Workflow Recommendations) depends on US1 (requires classification result)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

```
US1 (Tagging/Classification) ──→ US5 (Workflow Recommendations)
US2 (Summarization)          ──→ (none)
US3 (Duplicate Detection)    ──→ (none)
US4 (Sensitive Data)         ──→ (none)
```

### Parallel Execution Opportunities

**Within Phase 2** (after T005):
- T006, T007, T011 can run in parallel

**Across User Stories** (after Phase 2):
- US1 + US2 + US3 + US4 can all run in parallel (different worker files, different frontend components)
- US5 must wait for US1 classification to be implemented

**Within Phase 8**:
- T026, T027, T028 can all run in parallel

---

## Implementation Strategy

### MVP (Minimum Viable Product)
- Phase 1 + Phase 2 + Phase 3 (US1: Auto-Tagging & Classification)
- Delivers immediate value: every uploaded file gets intelligent tags and categories

### Incremental Delivery
1. **MVP**: Tagging + Classification (P1)
2. **Increment 2**: Add Summarization (P1) — extends the AI panel
3. **Increment 3**: Add Duplicate Detection (P2) — new component
4. **Increment 4**: Add Sensitive Data Detection (P2) — new component
5. **Increment 5**: Add Workflow Recommendations (P3) — extends classification
6. **Final**: Admin config + Docker polish
