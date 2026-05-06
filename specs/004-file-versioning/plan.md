# Implementation Plan: File Versioning

**Branch**: `004-file-versioning` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)

## Summary

Add file versioning to the existing file management system. Each file supports multiple versions with history tracking, restore capability, and version comparison. Versions are stored as separate objects in MinIO with metadata in a `file_versions` table. The file record tracks the current active version. Storage quota is updated to account for all version sizes.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Boot 3.3.5, Spring Data JPA, React 18, Axios  
**Storage**: MySQL 8.0 (version metadata), MinIO (version content)  
**Testing**: JUnit 5 + Spring Boot Test (backend), Vitest (frontend)  
**Constraints**: Version storage counts against org quota. Max 100 versions per file (configurable).

## Project Structure

### Source Code Changes

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   └── FileVersionController.java    # Version CRUD endpoints
│   ├── dto/
│   │   └── file/
│   │       └── FileVersionDto.java       # Version response DTO
│   ├── entity/
│   │   └── FileVersion.java             # Version JPA entity
│   ├── repository/
│   │   └── FileVersionRepository.java    # Version data access
│   └── service/
│       └── FileVersionService.java       # Version business logic
├── src/main/resources/db/migration/
│   └── V9__create_file_versions_table.sql

frontend/
├── src/
│   ├── api/
│   │   └── fileVersions.ts              # Version API client
│   ├── components/
│   │   └── FileVersionHistory.tsx        # Version timeline component
│   └── types/
│       └── file.ts                       # + FileVersion type
```

## API Design

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/files/{fileId}/versions | Upload new version |
| GET | /api/files/{fileId}/versions | List version history (paginated) |
| GET | /api/files/{fileId}/versions/{versionId} | Get version details |
| GET | /api/files/{fileId}/versions/{versionId}/download | Download specific version |
| POST | /api/files/{fileId}/versions/{versionId}/restore | Restore a version |
| GET | /api/files/{fileId}/versions/compare?v1={id}&v2={id} | Compare two versions |

## Key Decisions

1. **Version storage strategy**: Each version is a separate object in MinIO. Storage key pattern: `{org_id}/{workspace_id}/{file_uuid}/versions/v{number}_{original_name}`
2. **Restore creates new version**: Restoring does not rewrite history. It creates a new version that copies the content of the restored version.
3. **Quota impact**: All versions count against storage quota. Deleting old versions is a future feature.
4. **Backward compatibility**: Existing file upload flow creates v1 automatically. The `files` table gets `current_version_id` and `version_count` columns (nullable for migration).
