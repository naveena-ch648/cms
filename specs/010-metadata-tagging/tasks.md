# Tasks: Metadata & Tagging System

**Input**: Design documents from `/specs/010-metadata-tagging/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and DTO/entity scaffolding

- [X] T001 Create Flyway migration for metadata_fields, metadata_values, and file_tags tables in backend/src/main/resources/db/migration/V17__metadata_tagging_tables.sql
- [X] T002 [P] Create MetadataField entity in backend/src/main/java/com/cms/entity/MetadataField.java
- [X] T003 [P] Create MetadataValue entity in backend/src/main/java/com/cms/entity/MetadataValue.java
- [X] T004 [P] Create Tag (FileTag) entity in backend/src/main/java/com/cms/entity/FileTag.java
- [X] T005 [P] Create MetadataFieldRepository in backend/src/main/java/com/cms/repository/MetadataFieldRepository.java
- [X] T006 [P] Create MetadataValueRepository in backend/src/main/java/com/cms/repository/MetadataValueRepository.java
- [X] T007 [P] Create TagRepository (FileTagRepository) in backend/src/main/java/com/cms/repository/FileTagRepository.java
- [X] T008 [P] Create DTO classes in backend/src/main/java/com/cms/dto/metadata/ (MetadataFieldRequest, MetadataFieldResponse, MetadataValueRequest, MetadataValueResponse, TagRequest, TagResponse)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core services and OpenSearch index extension that all user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T009 Create MetadataFieldService with CRUD operations and workspace field limit enforcement in backend/src/main/java/com/cms/service/MetadataFieldService.java
- [X] T010 Create MetadataValueService with type validation and value assignment logic in backend/src/main/java/com/cms/service/MetadataValueService.java
- [X] T011 Create TagService with add/remove/autocomplete and Redis cache integration in backend/src/main/java/com/cms/service/TagService.java
- [X] T012 Extend OpenSearch file index mapping with metadata (object, dynamic) and tags (keyword array) fields in backend/src/main/java/com/cms/service/SearchIndexService.java

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Define Custom Metadata Fields (Priority: P1) 🎯 MVP

**Goal**: Workspace admins can create, edit, list, and soft-delete custom metadata field definitions (text, number, date, dropdown)

**Independent Test**: Admin creates a dropdown field "Department" with options, sees it listed, edits it, then deletes it — all via API

### Implementation for User Story 1

- [X] T013 [US1] Create MetadataFieldController with POST/GET/PUT/DELETE endpoints in backend/src/main/java/com/cms/controller/MetadataFieldController.java
- [X] T014 [US1] Add workspace admin authorization check in MetadataFieldController (create/update/delete restricted to admins)
- [X] T015 [US1] Add Redis cache for metadata fields list (10-min TTL, invalidate on create/update/delete) in MetadataFieldService
- [X] T016 [P] [US1] Create metadata fields API client in frontend/src/api/metadata.ts
- [X] T017 [US1] Create MetadataFieldManager component (admin UI for CRUD fields) in frontend/src/components/metadata/MetadataFieldManager.tsx
- [X] T018 [US1] Integrate MetadataFieldManager into workspace settings page in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: Admins can manage metadata field definitions end-to-end

---

## Phase 4: User Story 2 - Assign Metadata Values to Files (Priority: P1)

**Goal**: Users can view and assign typed metadata values to individual files from the file detail panel

**Independent Test**: User opens file detail panel, sees custom fields, enters values (text, number, date, dropdown), saves, and values persist

### Implementation for User Story 2

- [X] T019 [US2] Create MetadataValueController with GET/PUT/DELETE endpoints in backend/src/main/java/com/cms/controller/MetadataValueController.java
- [X] T020 [US2] Add file access permission check in MetadataValueController (read access for GET, write access for PUT/DELETE)
- [X] T021 [US2] Implement OpenSearch document update on metadata value change in MetadataValueService
- [X] T022 [P] [US2] Create metadata values API client (getFileMetadata, updateFileMetadata) in frontend/src/api/metadata.ts
- [X] T023 [US2] Create MetadataEditor component (renders typed fields with validation) in frontend/src/components/metadata/MetadataEditor.tsx
- [X] T024 [US2] Integrate MetadataEditor into FileDetailPanel in frontend/src/components/FileDetailPanel.tsx

**Checkpoint**: Users can assign and view metadata values on individual files

---

## Phase 5: User Story 3 - Tag Files with Free-Form Tags (Priority: P2)

**Goal**: Users can add/remove free-form tags on files with workspace-scoped autocomplete

**Independent Test**: User types a tag, sees autocomplete suggestions, adds it, removes another tag — changes persist and autocomplete updates

### Implementation for User Story 3

- [X] T025 [US3] Create TagController with GET/POST/DELETE file tag endpoints and workspace autocomplete endpoint in backend/src/main/java/com/cms/controller/TagController.java
- [X] T026 [US3] Add Redis sorted set operations for tag autocomplete cache (ZADD on create, ZRANGEBYLEX for prefix search) in TagService
- [X] T027 [US3] Implement OpenSearch document update on tag change (update tags array) in TagService
- [X] T028 [P] [US3] Create tags API client (getFileTags, addTags, removeTag, autocomplete) in frontend/src/api/tags.ts
- [X] T029 [US3] Create TagInput component with autocomplete dropdown in frontend/src/components/metadata/TagInput.tsx
- [X] T030 [US3] Integrate TagInput into FileDetailPanel in frontend/src/components/FileDetailPanel.tsx

**Checkpoint**: Users can tag files with autocomplete support

---

## Phase 6: User Story 4 - Filter Files by Metadata and Tags (Priority: P2)

**Goal**: Users can filter the file list by metadata values and tags using OpenSearch-backed queries

**Independent Test**: User applies "Department = HR" filter + "urgent" tag filter, only matching files display; clearing filters restores full list

### Implementation for User Story 4

- [X] T031 [US4] Extend file list API to accept metadata and tag filter query parameters in backend/src/main/java/com/cms/controller/FileController.java
- [X] T032 [US4] Implement OpenSearch query builder for metadata/tag filters (term, range, combined AND logic) in backend/src/main/java/com/cms/service/SearchService.java
- [X] T033 [P] [US4] Update file list API client to pass metadata and tag filter params in frontend/src/api/files.ts
- [X] T034 [US4] Create MetadataFilter component (field selector + value input per type) in frontend/src/components/metadata/MetadataFilter.tsx
- [X] T035 [US4] Integrate MetadataFilter into WorkspacePage file list area in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: Users can filter files by metadata values and tags

---

## Phase 7: User Story 5 - Bulk Metadata Assignment (Priority: P3)

**Goal**: Users can select multiple files and assign metadata values or tags in bulk

**Independent Test**: User selects 5 files, opens bulk edit, sets "Department = Legal" and adds tag "reviewed", all files updated

### Implementation for User Story 5

- [X] T036 [US5] Add PUT /api/v1/files/bulk-metadata endpoint in MetadataValueController
- [X] T037 [US5] Add POST /api/v1/files/bulk-tags endpoint in TagController
- [X] T038 [US5] Implement bulk update logic with transaction and OpenSearch bulk update in MetadataValueService and TagService
- [X] T039 [P] [US5] Add bulk metadata/tag API methods in frontend/src/api/metadata.ts and frontend/src/api/tags.ts
- [X] T040 [US5] Create BulkMetadataDialog component (multi-file metadata + tag assignment) in frontend/src/components/metadata/BulkMetadataDialog.tsx
- [X] T041 [US5] Add bulk action trigger in FileList (select files → "Edit Metadata" button) in frontend/src/components/FileList.tsx

**Checkpoint**: Users can efficiently assign metadata and tags to multiple files at once

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Performance, validation, and integration hardening

- [X] T042 [P] Add input validation annotations (@Valid, @Size, @NotBlank) to all DTO request classes in backend/src/main/java/com/cms/dto/metadata/
- [X] T043 [P] Add field count limit enforcement (max 50 per workspace) in MetadataFieldService
- [X] T044 [P] Add tag count limit enforcement (max 20 per file, max 50 chars) in TagService
- [X] T045 Ensure metadata fields cache invalidation works correctly on all mutation paths in MetadataFieldService
- [X] T046 Run quickstart.md validation — verify migration runs, field CRUD works, tag autocomplete responds, filters return correct results

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — field definitions (MVP)
- **User Story 2 (Phase 4)**: Depends on Phase 2 + Phase 3 (needs fields defined to assign values)
- **User Story 3 (Phase 5)**: Depends on Phase 2 only — independent of metadata fields
- **User Story 4 (Phase 6)**: Depends on Phase 4 and Phase 5 (needs values/tags to filter)
- **User Story 5 (Phase 7)**: Depends on Phase 4 and Phase 5 (bulk assigns values/tags)
- **Polish (Phase 8)**: After all user stories

### Parallel Opportunities

**Within Phase 1**: T002, T003, T004, T005, T006, T007, T008 are all parallelizable (independent files)

**Within Phase 3**: T016 (frontend API client) can start parallel to T013 (backend controller)

**Within Phase 5**: T028 (frontend tags API) can start parallel to T025 (backend controller)

**Phase 3 + Phase 5**: User Story 1 and User Story 3 can be implemented in parallel (independent — fields vs tags)

### Implementation Strategy

1. **MVP (Phase 1–3)**: Admin field management — delivers P1 core functionality
2. **Core Value (Phase 4)**: Value assignment — makes fields useful
3. **Enhanced UX (Phase 5–6)**: Tags + filtering — makes metadata navigable
4. **Power Users (Phase 7)**: Bulk operations — efficiency for large-scale use
5. **Hardening (Phase 8)**: Validation limits, cache correctness, integration validation
