# Implementation Plan: AI Automation

**Branch**: `016-ai-automation` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/016-ai-automation/spec.md`

## Summary

Build AI automation capabilities that extend the existing document ingestion pipeline with auto-tagging, summarization, classification, duplicate detection, sensitive data detection, and workflow recommendations. Processing is asynchronous via Python workers consuming from a dedicated Redis queue, with results stored in MySQL and surfaced through the Spring Boot API and React frontend.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (AI workers), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Data JPA, Spring Data Redis, OpenAI API (via existing LLM provider), sentence-transformers, Qdrant client, regex-based NLP; React 18, Axios 1.7.7, Vite 6  
**Storage**: MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V23), Redis 7 (port 6379) for job queue, Qdrant for content fingerprints, MinIO for file content  
**Testing**: JUnit 5, pytest, TypeScript (tsc --noEmit)  
**Target Platform**: Docker containers (Linux)  
**Project Type**: Web service (multi-tier)  
**Performance Goals**: 90% of files analyzed within 60 seconds; 10K documents/day throughput  
**Constraints**: AI processing MUST NOT block file access; queue backlog < 5 minutes  
**Scale/Scope**: Multi-tenant, 10K+ documents/day, concurrent workers

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular processors, typed entities, clear interfaces |
| II. Testing Standards | ✅ PASS | Workers include edge case handling, retry logic |
| III. User Experience | ✅ PASS | Async processing with status feedback; accept/reject UI |
| IV. Performance & Scalability | ✅ PASS | Async queue processing; horizontal worker scaling |
| V. Reliability & Fault Tolerance | ✅ PASS | Redis queue with retry (3x exponential backoff), DLQ |
| VI. Security & Compliance | ✅ PASS | Sensitive data detection supports compliance; RBAC enforced |
| VII. Data & AI Governance | ✅ PASS | AI outputs traceable (confidence scores, source tracking) |
| VIII. Observability | ✅ PASS | Job status tracking, processing metrics |
| IX. Developer Experience | ✅ PASS | Consistent project structure, reuses existing patterns |
| X. Continuous Improvement | ✅ PASS | Confidence thresholds tunable via admin config |

## Project Structure

### Documentation (this feature)

```text
specs/016-ai-automation/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── ai-automation-api.md
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── entity/
│   │   └── AIJob.java
│   ├── repository/
│   │   └── AIJobRepository.java
│   ├── dto/ai/
│   │   ├── AIJobResponse.java
│   │   ├── AISuggestionsResponse.java
│   │   ├── AcceptSuggestionsRequest.java
│   │   └── AIConfigRequest.java
│   ├── service/
│   │   └── AIAutomationService.java
│   └── controller/
│       └── AIAutomationController.java
├── src/main/resources/db/migration/
│   └── V23__ai_automation.sql

worker/
├── processors/
│   ├── ai_tagger.py          # Auto-tagging + classification
│   ├── ai_summarizer.py      # Document summarization
│   ├── ai_duplicates.py      # Duplicate detection via content fingerprints
│   ├── ai_sensitive.py       # Sensitive data pattern detection
│   └── ai_workflow.py        # Workflow recommendations

frontend/
├── src/
│   ├── components/
│   │   └── ai/
│   │       ├── AISuggestionsPanel.tsx
│   │       ├── SensitivityBadge.tsx
│   │       └── DuplicateWarning.tsx
│   └── api/
│       └── ai.ts
```

**Structure Decision**: Extends the existing 3-tier architecture (backend/worker/frontend). AI processors are individual Python modules following the same pattern as existing processors (embeddings.py, search_indexer.py). Backend adds one entity, one service, one controller. Frontend adds an AI panel component integrated into the existing FileDetailPanel.

## Complexity Tracking

No constitution violations. No complexity justification needed.
