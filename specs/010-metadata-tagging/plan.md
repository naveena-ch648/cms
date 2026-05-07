# Implementation Plan: Metadata & Tagging System

**Branch**: `010-metadata-tagging` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-metadata-tagging/spec.md`

## Summary

Build a custom metadata fields and free-form tagging system for the CMS platform. Workspace admins define typed fields (text, number, date, dropdown); users assign values to files and tag them. Metadata and tags are indexed in OpenSearch for filtering. The system uses MySQL for storage, Redis for caching autocomplete data, and OpenSearch for search/filter queries.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6, OpenSearch Java Client 2.x  
**Storage**: MySQL 8.0 (port 3307, metadata_fields/metadata_values/tags tables), Redis 7 (tag autocomplete cache), OpenSearch 2.11.0 (metadata index for filtering)  
**Testing**: JUnit 5, Vitest, React Testing Library  
**Target Platform**: Docker Compose (local dev), Linux server (production)  
**Project Type**: Web application (full-stack)  
**Performance Goals**: < 200ms for metadata CRUD, < 300ms for tag autocomplete, < 500ms for metadata-filtered searches  
**Constraints**: 50 fields per workspace, 20 tags per file, 50 chars per tag, multi-tenant isolation  
**Scale/Scope**: 100K+ files per workspace, up to 50 custom fields, thousands of unique tags per workspace

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular service/controller/repository layers; typed models |
| II. Testing Standards | ✅ PASS | Unit + integration tests for services and controllers |
| III. UX Consistency | ✅ PASS | ApiResponse wrapper, consistent form patterns, inline editing |
| IV. Performance | ✅ PASS | Redis cache for autocomplete, OpenSearch for filtered queries, indexed MySQL |
| V. Reliability | ✅ PASS | Input validation, optimistic locking for concurrent edits |
| VI. Security | ✅ PASS | RBAC admin-only field management, tenant isolation |
| VII. Data Governance | ✅ PASS | Soft-delete on field removal, audit trail via existing system |
| VIII. Observability | ✅ PASS | Leverages existing centralized logging |
| IX. Developer Experience | ✅ PASS | Standard project structure, Flyway migrations |
| X. Continuous Improvement | ✅ PASS | Metadata model extensible for future field types |

**Gate Result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/010-metadata-tagging/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── metadata-fields-api.md
│   ├── metadata-values-api.md
│   └── tags-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   ├── MetadataFieldController.java
│   │   ├── MetadataValueController.java
│   │   └── TagController.java
│   ├── service/
│   │   ├── MetadataFieldService.java
│   │   ├── MetadataValueService.java
│   │   └── TagService.java
│   ├── entity/
│   │   ├── MetadataField.java
│   │   ├── MetadataValue.java
│   │   └── Tag.java
│   ├── repository/
│   │   ├── MetadataFieldRepository.java
│   │   ├── MetadataValueRepository.java
│   │   └── TagRepository.java
│   └── dto/metadata/
│       ├── MetadataFieldRequest.java
│       ├── MetadataFieldResponse.java
│       ├── MetadataValueRequest.java
│       ├── MetadataValueResponse.java
│       ├── TagRequest.java
│       └── TagResponse.java
├── src/main/resources/db/migration/
│   └── V17__metadata_tagging_tables.sql
└── src/test/java/com/cms/

frontend/
├── src/
│   ├── api/
│   │   ├── metadata.ts
│   │   └── tags.ts
│   ├── components/metadata/
│   │   ├── MetadataFieldManager.tsx
│   │   ├── MetadataEditor.tsx
│   │   ├── MetadataFilter.tsx
│   │   ├── TagInput.tsx
│   │   └── BulkMetadataDialog.tsx
│   └── pages/
│       └── (integrated into WorkspacePage)
```

**Structure Decision**: Extends existing web application structure. Backend follows established controller→service→repository pattern. Frontend adds metadata components to existing file management views.
