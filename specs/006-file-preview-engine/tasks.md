# Tasks: File Preview Engine

**Input**: Design documents from `/specs/006-file-preview-engine/`  
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/preview-api.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, new dependencies, Docker configuration

- [X] T001 Add pdfjs-dist dependency to frontend/package.json
- [X] T002 [P] Add preview type definitions in frontend/src/types/preview.ts
- [X] T003 [P] Update worker/Dockerfile to install LibreOffice headless and FFmpeg
- [X] T004 [P] Update worker/requirements.txt with pdf2image, python-pptx, Pillow, pymysql, boto3, ffmpeg-python dependencies

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, entities, repositories, and core services that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 Create Flyway migration V006__preview_system.sql with previews, preview_jobs, and comments tables in backend/src/main/resources/db/migration/V006__preview_system.sql
- [X] T006 [P] Create Preview entity in backend/src/main/java/com/cms/entity/Preview.java
- [X] T007 [P] Create PreviewJob entity in backend/src/main/java/com/cms/entity/PreviewJob.java
- [X] T008 [P] Create Comment entity in backend/src/main/java/com/cms/entity/Comment.java
- [X] T009 [P] Create PreviewRepository in backend/src/main/java/com/cms/repository/PreviewRepository.java
- [X] T010 [P] Create PreviewJobRepository in backend/src/main/java/com/cms/repository/PreviewJobRepository.java
- [X] T011 [P] Create CommentRepository in backend/src/main/java/com/cms/repository/CommentRepository.java
- [X] T012 [P] Create PreviewDto in backend/src/main/java/com/cms/dto/preview/PreviewDto.java
- [X] T013 [P] Create PreviewJobDto in backend/src/main/java/com/cms/dto/preview/PreviewJobDto.java
- [X] T014 [P] Create ThumbnailDto in backend/src/main/java/com/cms/dto/preview/ThumbnailDto.java
- [X] T015 [P] Create CommentDto in backend/src/main/java/com/cms/dto/preview/CommentDto.java
- [X] T016 Implement PreviewJobDispatcher service (publishes to Redis queue) in backend/src/main/java/com/cms/service/PreviewJobDispatcher.java
- [X] T017 Implement PreviewService (lookup, status, presigned URL generation) in backend/src/main/java/com/cms/service/PreviewService.java
- [X] T018 Create PreviewController with GET /preview, GET /thumbnail, POST /regenerate, GET /status endpoints in backend/src/main/java/com/cms/controller/PreviewController.java
- [X] T019 Update worker/worker.py to handle "preview" and "thumbnail" actions and dispatch to new processors
- [X] T020 Create preview API client in frontend/src/api/previews.ts
- [X] T021 [P] Create comments API client in frontend/src/api/comments.ts

**Checkpoint**: Foundation ready — preview infrastructure (DB, entities, APIs, queue dispatch) operational. Worker routing configured.

---

## Phase 3: User Story 1 - Preview PDF Documents (Priority: P1) 🎯 MVP

**Goal**: Users can click a PDF file and see it rendered page-by-page with zoom and navigation controls

**Independent Test**: Upload a multi-page PDF → click to preview → verify pages render with zoom/pagination

### Implementation for User Story 1

- [X] T022 [US1] Implement PDF thumbnail processor in worker/processors/preview_pdf.py (extract first page as 256x256 thumbnail, store in MinIO)
- [X] T023 [US1] Extend preview_pdf.py with full preview generation (render all pages as PNG images, store in MinIO under previews/{fileId}/{versionId}/page-N.png)
- [X] T024 [US1] Implement PdfViewer component using pdfjs-dist with canvas rendering in frontend/src/components/preview/PdfViewer.tsx
- [X] T025 [US1] Implement PreviewToolbar with zoom in/out, fit-to-width, fit-to-page, and page navigation controls in frontend/src/components/preview/PreviewToolbar.tsx
- [X] T026 [US1] Implement PreviewModal as full-screen overlay that loads file preview and selects the correct viewer component in frontend/src/components/preview/PreviewModal.tsx
- [X] T027 [US1] Wire PreviewModal into WorkspacePage file click handler to open preview on file click

**Checkpoint**: PDF files render in-browser with zoom and page navigation. MVP deliverable.

---

## Phase 4: User Story 2 - Preview Images (Priority: P1)

**Goal**: Users can click image files and see them rendered with zoom/pan controls

**Independent Test**: Upload JPEG/PNG/GIF/SVG/WebP → click to preview → verify render with zoom and pan

### Implementation for User Story 2

- [X] T028 [P] [US2] Implement image thumbnail processor in worker/processors/preview_image.py (resize to 256x256, store in MinIO)
- [X] T029 [US2] Implement ImageViewer component with zoom (scroll wheel + buttons) and pan (drag) in frontend/src/components/preview/ImageViewer.tsx
- [X] T030 [US2] Add image type routing in PreviewModal to render ImageViewer for image/* mime types in frontend/src/components/preview/PreviewModal.tsx

**Checkpoint**: Image files display with zoom/pan. Thumbnails generated for folder listings.

---

## Phase 5: User Story 3 - Preview Video Files (Priority: P2)

**Goal**: Users can click video files and see a video player with playback controls

**Independent Test**: Upload MP4/WebM → click to preview → verify player loads with play/pause/seek/volume/fullscreen

### Implementation for User Story 3

- [X] T031 [P] [US3] Implement video thumbnail processor in worker/processors/preview_video.py (extract frame at 2s using FFmpeg, resize to 256x256, store in MinIO)
- [X] T032 [US3] Implement VideoPlayer component with HTML5 video element and controls in frontend/src/components/preview/VideoPlayer.tsx
- [X] T033 [US3] Add video type routing in PreviewModal to render VideoPlayer for video/* mime types in frontend/src/components/preview/PreviewModal.tsx

**Checkpoint**: Video files play in-browser. Video thumbnails appear in folder listings.

---

## Phase 6: User Story 4 - Preview Office Documents (Priority: P2)

**Goal**: Users can click Word/Excel/PPT files and see rendered preview pages with slide/sheet navigation

**Independent Test**: Upload .docx/.xlsx/.pptx → click to preview → verify content renders with pagination

### Implementation for User Story 4

- [X] T034 [US4] Implement Office document preview processor in worker/processors/preview_office.py (LibreOffice headless converts to PDF, then render pages as images, store in MinIO)
- [X] T035 [US4] Implement OfficeViewer component that displays pre-rendered page images with navigation in frontend/src/components/preview/OfficeViewer.tsx
- [X] T036 [US4] Add Office type routing in PreviewModal for application/vnd.openxmlformats* and application/msword mime types in frontend/src/components/preview/PreviewModal.tsx
- [X] T037 [US4] Update isPreviewable() in backend/src/main/java/com/cms/controller/FileController.java to include Office mime types

**Checkpoint**: Office documents render as page images. Full navigation support for multi-page docs.

---

## Phase 7: User Story 5 - File Thumbnails (Priority: P2)

**Goal**: Folder file listings show auto-generated thumbnails for supported file types

**Independent Test**: Upload files of different types → view folder → verify thumbnails appear in file list

### Implementation for User Story 5

- [X] T038 [US5] Update PreviewService to dispatch thumbnail generation job on file upload completion in backend/src/main/java/com/cms/service/PreviewService.java
- [X] T039 [US5] Add GET /files/{fileId}/thumbnail endpoint logic to PreviewController returning presigned URL in backend/src/main/java/com/cms/controller/PreviewController.java
- [X] T040 [US5] Update FileDto to include thumbnailUrl field populated from preview lookup in backend/src/main/java/com/cms/dto/file/FileDto.java
- [X] T041 [US5] Update file list components to display thumbnail images from thumbnailUrl in frontend (WorkspacePage file list rendering)

**Checkpoint**: Thumbnails visible in folder views for all supported file types.

---

## Phase 8: User Story 6 - Inline Metadata and Comments (Priority: P3)

**Goal**: Preview side panel shows file metadata and allows threaded comments

**Independent Test**: Open file preview → verify metadata panel → post comment → verify it persists

### Implementation for User Story 6

- [X] T042 [US6] Implement CommentService with create, list (threaded), and delete operations in backend/src/main/java/com/cms/service/CommentService.java
- [X] T043 [US6] Create CommentController with GET/POST/DELETE endpoints for /files/{fileId}/comments in backend/src/main/java/com/cms/controller/CommentController.java
- [X] T044 [US6] Implement MetadataPanel component showing file info (size, type, date, uploader, tags) and comment list in frontend/src/components/preview/MetadataPanel.tsx
- [X] T045 [US6] Add comment input form with threading (reply-to) support to MetadataPanel in frontend/src/components/preview/MetadataPanel.tsx
- [X] T046 [US6] Integrate MetadataPanel into PreviewModal as a collapsible side panel in frontend/src/components/preview/PreviewModal.tsx

**Checkpoint**: Users can view metadata and post/view threaded comments while previewing files.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, performance, edge cases across all stories

- [X] T047 [P] Add error/loading/fallback states to all viewer components (show download link when preview unavailable) in frontend/src/components/preview/
- [X] T048 [P] Add file size check (100MB limit) before dispatching preview jobs in backend/src/main/java/com/cms/service/PreviewService.java
- [X] T049 [P] Add preview regeneration on new version upload in backend/src/main/java/com/cms/service/PreviewService.java
- [X] T050 Implement retry logic in worker preview processors (max 3 attempts with exponential backoff) in worker/processors/
- [X] T051 Update docker/docker-compose.yml worker service with LibreOffice/FFmpeg environment and config variables
- [X] T052 Run quickstart.md validation — verify full flow: upload → thumbnail → preview → comments

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–8)**: All depend on Foundational phase completion
  - US1 (PDF) and US2 (Image) can proceed in parallel
  - US3 (Video) can proceed in parallel with US1/US2
  - US4 (Office) depends on US1 PDF viewer patterns being established
  - US5 (Thumbnails) depends on at least one processor (US1/US2) being complete
  - US6 (Comments) is fully independent of other user stories
- **Polish (Phase 9)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (PDF Preview)**: Foundation only → MVP starting point
- **US2 (Image Preview)**: Foundation only → parallel with US1
- **US3 (Video Preview)**: Foundation only → parallel with US1/US2
- **US4 (Office Preview)**: Foundation + worker LibreOffice setup (T003) → can parallel after US1 patterns established
- **US5 (Thumbnails)**: Foundation + at least one worker processor complete → can start after T022 or T028
- **US6 (Comments)**: Foundation only → fully independent, can parallel with any story

### Parallel Execution Examples

**Maximum parallelism** (3 developers):
- Dev A: US1 (PDF) → US4 (Office)
- Dev B: US2 (Image) → US5 (Thumbnails)
- Dev C: US3 (Video) → US6 (Comments)

**Solo execution** (priority order):
- Phase 1 → Phase 2 → US1 → US2 → US3 → US4 → US5 → US6 → Polish

---

## Implementation Strategy

**MVP**: Phase 1 + Phase 2 + US1 (PDF Preview) = minimal viable preview system  
**v1.0**: MVP + US2 (Images) + US5 (Thumbnails) = visual file browsing  
**v1.1**: v1.0 + US3 (Video) + US4 (Office) = full format coverage  
**v1.2**: v1.1 + US6 (Comments) + Polish = complete feature
