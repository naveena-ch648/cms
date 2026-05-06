# Tasks: File Versioning

**Input**: Design documents from `/specs/004-file-versioning/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/versions-api.md

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Database & Entity Setup

**Purpose**: Create version table, entity, and repository

- [x] T001 Create V9__create_file_versions_table.sql migration (file_versions table with all columns, indexes, and ALTER files table to add current_version_id and version_count) in backend/src/main/resources/db/migration/V9__create_file_versions_table.sql
- [x] T002 Create FileVersion JPA entity (maps to file_versions table, all fields per data-model.md) in backend/src/main/java/com/cms/entity/FileVersion.java
- [x] T003 [P] Update FileEntity to add currentVersionId and versionCount fields in backend/src/main/java/com/cms/entity/FileEntity.java
- [x] T004 [P] Create FileVersionRepository with query methods (findByFileIdOrderByVersionNumberDesc, findByFileIdAndUuid, findTopByFileIdOrderByVersionNumberDesc) in backend/src/main/java/com/cms/repository/FileVersionRepository.java

---

## Phase 2: Backend Service & Controller

**Purpose**: Implement version business logic and REST endpoints

- [x] T005 Create FileVersionDto (response DTO with id, versionNumber, fileName, sizeBytes, mimeType, checksumSha256, changeNote, uploadedBy, createdAt, isCurrent) in backend/src/main/java/com/cms/dto/file/FileVersionDto.java
- [x] T006 Implement FileVersionService (uploadNewVersion, listVersions with pagination, getVersion, downloadVersion presigned URL, restoreVersion, compareVersions) in backend/src/main/java/com/cms/service/FileVersionService.java
- [x] T007 Implement FileVersionController with all endpoints per contract (POST upload, GET list, GET detail, GET download, POST restore, GET compare) in backend/src/main/java/com/cms/controller/FileVersionController.java
- [x] T008 Update FileService.createFileRecord to also create version 1 entry and set current_version_id on file in backend/src/main/java/com/cms/service/FileService.java
- [x] T009 Update FileService.permanentDelete to also delete all version records and version storage objects in backend/src/main/java/com/cms/service/FileService.java

---

## Phase 3: Frontend

**Purpose**: Add version API client, types, and UI component

- [x] T010 Add FileVersion type to frontend/src/types/file.ts
- [x] T011 Create fileVersions API client (uploadVersion, listVersions, getVersion, downloadVersion, restoreVersion, compareVersions) in frontend/src/api/fileVersions.ts
- [x] T012 Create FileVersionHistory component (version timeline list with upload/restore/download actions) in frontend/src/components/FileVersionHistory.tsx
- [x] T013 Integrate FileVersionHistory into FileDetailPanel (show version count badge, expandable version history section) in frontend/src/components/FileDetailPanel.tsx

---

## Phase 4: Polish

**Purpose**: Audit logging and validation

- [x] T014 Add audit logging for version operations (upload_version, restore_version, download_version) via AuditService in FileVersionService
- [x] T015 Update SecurityConfig to allow version endpoints for users with FILE_UPLOAD and FILE_DOWNLOAD permissions

---

## Dependencies & Execution Order

- **Phase 1**: No dependencies — start immediately
- **Phase 2**: Depends on Phase 1
- **Phase 3**: Depends on Phase 2 (needs API endpoints)
- **Phase 4**: Depends on Phase 2
- Tasks marked [P] can run in parallel within their phase
