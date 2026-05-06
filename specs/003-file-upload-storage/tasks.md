# Tasks: File Upload & Storage System

**Input**: Design documents from `/specs/003-file-upload-storage/`
**Prerequisites**: plan.md, spec.md, data-model.md, research.md, quickstart.md, contracts/ (upload-api.md, files-api.md)

**Note**: US4 (SFTP-Based File Upload, P3) is **deferred** per research decision R7. Architecture accommodates future SFTP by exposing the same FileService interface.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Infrastructure & Dependencies)

**Purpose**: Add MinIO, Python worker, and backend dependencies to the project

- [x] T001 Update docker/docker-compose.yml with MinIO service (ports 9000/9001, volume minio-data) and Python worker service
- [x] T002 Add AWS S3 SDK v2 (software.amazon.awssdk:s3) and Apache Tika (tika-core) dependencies to backend/pom.xml
- [x] T003 [P] Add MinIO connection and file-upload configuration sections to backend/src/main/resources/application.yml
- [x] T004 [P] Create Python worker project skeleton (Dockerfile, requirements.txt, processors/__init__.py) in worker/

---

## Phase 2: Foundational (Database, Entities, Core Services)

**Purpose**: Database schema, JPA entities, and core infrastructure services that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Create V6__create_file_tables.sql migration (files table, storage_quotas table with all columns and indexes per data-model.md) in backend/src/main/resources/db/migration/V6__create_file_tables.sql
- [x] T006 [P] Create V7__seed_file_permissions.sql migration (FILE_UPLOAD, FILE_DOWNLOAD, FILE_MANAGE, FILE_TRASH_RESTORE, FILE_TRASH_DELETE permissions assigned to Viewer/Editor/Admin roles) in backend/src/main/resources/db/migration/V7__seed_file_permissions.sql
- [x] T007 [P] Create V8__seed_default_storage_quota.sql migration (default storage quota for existing organizations) in backend/src/main/resources/db/migration/V8__seed_default_storage_quota.sql
- [x] T008 [P] Create FileEntity JPA entity (maps to files table, status enum ACTIVE/TRASHED/DELETED, all fields per data-model.md) in backend/src/main/java/com/cms/entity/FileEntity.java
- [x] T009 [P] Create StorageQuota JPA entity (maps to storage_quotas table, one-per-org, JSON fields for extensions) in backend/src/main/java/com/cms/entity/StorageQuota.java
- [x] T010 [P] Create FileRepository with query methods (findByFolderIdAndStatus, findByOrganizationId, findByStatusAndPermanentDeleteAtBefore) in backend/src/main/java/com/cms/repository/FileRepository.java
- [x] T011 [P] Create StorageQuotaRepository with findByOrganizationId method in backend/src/main/java/com/cms/repository/StorageQuotaRepository.java
- [x] T012 Create MinioConfig with S3Client bean configured for MinIO endpoint, credentials, and region in backend/src/main/java/com/cms/config/MinioConfig.java
- [x] T013 Implement StorageService (putObject, getObject, deleteObject, presignGetUrl, presignPutUrl, initiateMultipartUpload, completeMultipartUpload, abortMultipartUpload, createBucketIfNotExists) in backend/src/main/java/com/cms/service/StorageService.java
- [x] T014 [P] Implement StorageQuotaService (checkQuotaAvailable, updateUsedStorage, getQuotaForOrg, validateFileSize, validateFileExtension) in backend/src/main/java/com/cms/service/StorageQuotaService.java
- [x] T015 [P] Implement FileProcessingQueueService (publishJob to Redis list file:process, job payload with fileId/orgId/action) in backend/src/main/java/com/cms/service/FileProcessingQueueService.java
- [x] T016 [P] Create file DTOs (FileDto, FileUploadRequest, ChunkUploadResponse, StorageQuotaDto, UploadInitiateRequest, UploadInitiateResponse, UploadSessionStatusDto) in backend/src/main/java/com/cms/dto/
- [x] T017 [P] Create file TypeScript types (FileInfo, UploadSession, StorageQuota, UploadProgress, ChunkStatus) in frontend/src/types/file.ts

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Single & Bulk File Upload via Web UI (Priority: P1) 🎯 MVP

**Goal**: Users can upload single or multiple files via file picker or drag-and-drop, see individual progress bars, and view uploaded files in the folder listing immediately.

**Independent Test**: Open a workspace folder, drag 5 files (10KB–50MB) onto the content area. Each shows progress. All 5 appear in the folder listing with correct names and sizes after upload completes.

### Implementation for User Story 1

- [x] T018 [US1] Implement FileService (createFileRecord, listFilesByFolder with pagination/sorting, getFileById, handleDuplicate with rename/replace/error strategies, permission checks via folder access) in backend/src/main/java/com/cms/service/FileService.java
- [x] T019 [US1] Implement FileUploadService (single file upload: validate quota + extensions, upload to MinIO via StorageService, create file record, update quota usage, publish processing job) in backend/src/main/java/com/cms/service/FileUploadService.java
- [x] T020 [US1] Implement FileUploadController with POST /api/files/upload endpoint (multipart/form-data: file, folderId, description, tags, onDuplicate) returning FileDto in backend/src/main/java/com/cms/controller/FileUploadController.java
- [x] T021 [US1] Implement FileController with GET /api/files (paginated folder listing) and GET /api/files/{fileId} (file details) endpoints in backend/src/main/java/com/cms/controller/FileController.java
- [x] T022 [P] [US1] Implement file API client (uploadFile, listFiles, getFileDetails) in frontend/src/api/files.ts
- [x] T023 [US1] Create FileUploadZone component (drag-and-drop zone with ondragover/ondrop, file picker button, bulk file selection, drop highlight) in frontend/src/components/FileUploadZone.tsx
- [x] T024 [P] [US1] Create UploadProgressPanel component (bottom drawer showing active/queued/completed uploads with individual progress bars) in frontend/src/components/UploadProgressPanel.tsx
- [x] T025 [US1] Create FileList component (file listing table with name, size, type, uploader, date, thumbnail placeholder) in frontend/src/components/FileList.tsx
- [x] T026 [P] [US1] Create DuplicateFileDialog component (rename/replace/skip options on filename conflict) in frontend/src/components/DuplicateFileDialog.tsx
- [x] T027 [US1] Integrate FileUploadZone, UploadProgressPanel, and FileList into WorkspacePage in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: Users can upload files via web UI, see progress, and view files in folder listing

---

## Phase 4: User Story 2 — Large File & Resumable Upload (Priority: P1)

**Goal**: Files over 100 MB use chunked upload with pause/resume capability. Network interruptions are detected and uploads resume from last successful chunk.

**Independent Test**: Begin uploading a 1 GB file. At 40% progress, interrupt the network. After reconnecting, click "Resume" — upload continues from 40% and completes successfully.

### Implementation for User Story 2

- [x] T028 [US2] Add chunked upload session management to FileUploadService (initiateChunkedUpload: create Redis hash with session state + S3 multipart initiation, uploadChunk: presigned URL or direct upload, completeChunkedUpload: verify all chunks + S3 CompleteMultipartUpload + create file record, abortUpload: cleanup Redis + S3) in backend/src/main/java/com/cms/service/FileUploadService.java
- [x] T029 [US2] Add chunked upload endpoints to FileUploadController (POST /upload/initiate, PUT /upload/{sessionId}/chunks/{chunkNumber}, POST /upload/{sessionId}/complete, DELETE /upload/{sessionId}, GET /upload/{sessionId}/status) in backend/src/main/java/com/cms/controller/FileUploadController.java
- [x] T030 [US2] Implement UploadSessionCleanupJob (scheduled task to scan expired Redis upload sessions, abort S3 multipart uploads, clean up partial data) in backend/src/main/java/com/cms/scheduler/UploadSessionCleanupJob.java
- [x] T031 [US2] Create useUploadManager hook (file chunking logic, parallel chunk upload with concurrency limit of 3, pause/resume state, progress tracking with percentage and ETA, network interruption detection, auto-retry) in frontend/src/hooks/useUploadManager.ts
- [x] T032 [US2] Update FileUploadZone to use useUploadManager for large files (auto-detect >100MB), update UploadProgressPanel with pause/resume/cancel controls and estimated time remaining in frontend/src/components/

**Checkpoint**: Large file uploads work with chunking, pause/resume, and network resilience

---

## Phase 5: User Story 3 — API-Based File Upload (Priority: P2)

**Goal**: External systems and scripts can upload files programmatically via REST API with metadata, quota checking, and proper error responses.

**Independent Test**: An authenticated API call uploads a 50 MB file to a specific folder with description and tags. The file appears in the target folder with correct metadata.

### Implementation for User Story 3

- [x] T033 [US3] Add storage quota info endpoint (GET /api/storage/quota returning StorageQuotaDto with used/max/percentage/restrictions) to FileController in backend/src/main/java/com/cms/controller/FileController.java
- [x] T034 [US3] Add organization-level file extension validation (check allowed_extensions and blocked_extensions from StorageQuota before upload) to FileUploadService in backend/src/main/java/com/cms/service/FileUploadService.java

**Checkpoint**: API clients can check quota and upload files programmatically with full metadata support

---

## Phase 6: User Story 5 — File Download & Preview (Priority: P2)

**Goal**: Users can download files and preview images, PDFs, and text files inline without downloading.

**Independent Test**: Upload an image and a PDF. Click the image — see inline preview. Click the PDF — see rendered preview. Download both and verify content matches originals.

### Implementation for User Story 5

- [x] T035 [US5] Add download endpoint (GET /api/files/{fileId}/download → 302 redirect to presigned MinIO URL with 1h expiry, increment download_count, update last_accessed_at) to FileController in backend/src/main/java/com/cms/controller/FileController.java
- [x] T036 [US5] Add preview endpoint (GET /api/files/{fileId}/preview → JSON with presigned URL and MIME type, only for previewable types: image/*, application/pdf, text/*) to FileController in backend/src/main/java/com/cms/controller/FileController.java
- [x] T037 [P] [US5] Create FileDetailPanel component (sidebar panel showing file name, size, type, uploader, date, description, tags, download count, download button, preview button) in frontend/src/components/FileDetailPanel.tsx
- [x] T038 [P] [US5] Create FilePreview component (inline preview: img tag for images, iframe/embed for PDFs, pre block for text, "no preview" fallback for other types) in frontend/src/components/FilePreview.tsx
- [x] T039 [US5] Add download and preview API methods (downloadFile, getPreviewUrl) to frontend/src/api/files.ts
- [x] T040 [US5] Integrate FileDetailPanel and FilePreview into WorkspacePage (click file in FileList → open detail panel with preview) in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: Users can download files and preview images/PDFs/text inline

---

## Phase 7: User Story 6 — File Management Operations (Priority: P2)

**Goal**: Users can rename, move, copy, and delete files. Deleted files go to trash with restore capability. Expired trash is permanently cleaned up.

**Independent Test**: Upload a file to "Folder A", rename it, move it to "Folder B", verify it appears with the new name. Delete it, find it in trash, restore it, then permanently delete it.

### Implementation for User Story 6

- [x] T041 [US6] Add rename and metadata update operations (updateFile: name, description, tags with duplicate name checking) to FileService in backend/src/main/java/com/cms/service/FileService.java
- [x] T042 [US6] Add move and copy operations (moveFile: update folder_id + storage_key, permission check on source and target folders; copyFile: duplicate MinIO object + create new file record) to FileService in backend/src/main/java/com/cms/service/FileService.java
- [x] T043 [US6] Add trash operations (trashFile: set status=TRASHED + trashed_at + permanent_delete_at; restoreFile: set status=ACTIVE + clear trash fields; permanentDelete: delete from MinIO + delete/mark row) to FileService in backend/src/main/java/com/cms/service/FileService.java
- [x] T044 [US6] Add file management endpoints (PATCH /api/files/{fileId}, POST /move, POST /copy, DELETE soft-delete, POST /restore, DELETE /permanent) to FileController in backend/src/main/java/com/cms/controller/FileController.java
- [x] T045 [US6] Add trash listing endpoint (GET /api/files/trash with pagination, showing trashedAt and permanentDeleteAt) to FileController in backend/src/main/java/com/cms/controller/FileController.java
- [x] T046 [US6] Implement TrashCleanupJob (scheduled task to find files with permanent_delete_at in the past, delete from MinIO, remove/mark records, update quota) in backend/src/main/java/com/cms/scheduler/TrashCleanupJob.java
- [x] T047 [P] [US6] Add file management API methods (renameFile, moveFile, copyFile, trashFile, restoreFile, permanentDeleteFile, listTrash) to frontend/src/api/files.ts
- [x] T048 [US6] Add file context menu with rename/move/copy/delete actions to FileList component in frontend/src/components/FileList.tsx
- [x] T049 [US6] Add trash view with restore and permanent-delete actions to WorkspacePage in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: Full file lifecycle management works — rename, move, copy, trash, restore, permanent delete

---

## Phase 8: Python Worker & Async Processing

**Purpose**: Post-upload async processing — thumbnail generation and metadata extraction via Redis queue

- [x] T050 Implement worker configuration module (Redis connection, MinIO connection, MySQL connection, concurrency settings from env vars) in worker/config.py
- [x] T051 Implement main worker loop (Redis BRPOP on file:process queue, job dispatch to processors, retry with exponential backoff 5s/25s/125s, dead-letter to file:process:dead after 3 failures, graceful shutdown) in worker/worker.py
- [x] T052 [P] Implement thumbnail processor (generate WebP thumbnails for JPEG/PNG/GIF/WebP images using Pillow, upload thumbnail to MinIO at thumbs/ path, update file record thumbnail_key in MySQL) in worker/processors/thumbnail.py
- [x] T053 [P] Implement metadata processor (validate MIME type with python-magic, extract image dimensions with Pillow, extract PDF page count, update file record in MySQL) in worker/processors/metadata.py

**Checkpoint**: Uploaded files are asynchronously processed for thumbnails and metadata

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Audit logging, security configuration, and end-to-end validation

- [x] T054 Add audit logging for all file operations (upload, download, rename, move, copy, trash, restore, permanent-delete) via existing AuditService integration in FileService and FileUploadService
- [x] T055 [P] Update SecurityConfig to configure file endpoint permissions (FILE_UPLOAD, FILE_DOWNLOAD, FILE_MANAGE, FILE_TRASH_RESTORE, FILE_TRASH_DELETE) in backend/src/main/java/com/cms/security/ or backend/src/main/java/com/cms/config/
- [x] T056 Run quickstart.md validation (start services, upload small file, upload chunked file, list files, download, preview image/PDF, rename, move, copy, trash, restore, permanent delete, check MinIO console)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Foundational — core upload + listing
- **US2 (Phase 4)**: Depends on US1 (extends FileUploadService and FileUploadController)
- **US3 (Phase 5)**: Depends on US1 (adds quota endpoint and extension validation)
- **US5 (Phase 6)**: Depends on US1 (extends FileController with download/preview)
- **US6 (Phase 7)**: Depends on US1 (extends FileService and FileController with management ops)
- **Python Worker (Phase 8)**: Depends on Foundational (uses Redis queue and MinIO) — can run in parallel with US1+
- **Polish (Phase 9)**: Depends on all user story phases being complete

### Parallel Opportunities

After Foundational (Phase 2) completes:
- **US1 (Phase 3)** and **Python Worker (Phase 8)** can start in parallel
- **US5 (Phase 6)** and **US6 (Phase 7)** can start in parallel after US1 completes
- **US3 (Phase 5)** can start in parallel with US5/US6 after US1 completes

### Within Each User Story

- Backend service before controller
- Controller before frontend API client
- Frontend API client before UI components
- UI components before page integration
- Tasks marked [P] can run in parallel within their phase

### Deferred

- **US4 — SFTP-Based File Upload (P3)**: Deferred per research decision R7. Architecture supports future addition via FileService interface.

---

## Parallel Example: User Story 1

```
T018 (FileService) ──────┐
                         ├──► T020 (FileUploadController) ──► T022 (API client) ──► T023 (FileUploadZone)
T019 (FileUploadService) ┘                                                     ├──► T024 [P] (UploadProgressPanel)
                                                                               ├──► T025 (FileList)
T021 (FileController) ──────────────────────────────────────────────────────── ├──► T026 [P] (DuplicateFileDialog)
                                                                               └──► T027 (WorkspacePage integration)
```

---

## Implementation Strategy

1. **MVP (Phase 1–3)**: Setup + Foundational + US1 delivers a working file upload system with web UI
2. **Core Complete (+ Phase 4)**: Add chunked/resumable uploads for large files
3. **Full Feature (+ Phases 5–8)**: API quota, download/preview, file management, async processing
4. **Production Ready (+ Phase 9)**: Audit logging, security hardening, validation
