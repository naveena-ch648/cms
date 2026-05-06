# Feature Specification: File Versioning

**Feature Branch**: `004-file-versioning`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: Build file versioning. Each file supports multiple versions with history, restore, and comparison. Track uploader and timestamps.

## User Scenarios & Testing

### User Story 1 - Upload New Version (Priority: P1)

A user uploads a new version of an existing file. The system stores the new content as a separate version while preserving the previous version. The file's metadata (name, folder location) remains the same, but version number increments. The uploader and timestamp are recorded for the new version.

**Independent Test**: Upload a file "report.pdf". Upload a new version of the same file. Verify version history shows v1 and v2 with different timestamps and uploaders.

**Acceptance Scenarios**:

1. **Given** a user has an existing file in a folder, **When** they upload a new version via the version upload endpoint, **Then** the file's current content is replaced with the new upload and a version record is created for the previous content.
2. **Given** a file has version 1, **When** a user uploads a new version, **Then** the version number increments to 2 and both versions are accessible in the history.
3. **Given** a user uploads a new version, **When** the upload completes, **Then** the version record stores the uploader's identity, timestamp, file size, and storage key.
4. **Given** a file with multiple versions, **When** a user views the file details, **Then** the current version number is displayed.

---

### User Story 2 - View Version History (Priority: P1)

A user views the complete version history of a file showing all past versions in reverse chronological order. Each version entry shows version number, uploader name, upload timestamp, file size, and optional change note.

**Independent Test**: Upload a file and create 3 versions. View version history and verify all 3 versions are listed with correct metadata in reverse chronological order.

**Acceptance Scenarios**:

1. **Given** a file with multiple versions, **When** a user requests the version history, **Then** all versions are returned in descending order by version number.
2. **Given** a version entry, **When** displayed in the UI, **Then** it shows version number, uploader name, timestamp, file size, and change note (if provided).
3. **Given** a file with many versions, **When** listing the history, **Then** the response is paginated.

---

### User Story 3 - Restore Previous Version (Priority: P1)

A user restores a previous version of a file, making its content the current active version. This creates a new version entry (not overwriting history) that is effectively a copy of the restored version's content.

**Independent Test**: Upload v1 of a file, then v2, then v3. Restore v1. Verify the file now shows v4 with the same content/size as v1, and v1-v3 remain in history.

**Acceptance Scenarios**:

1. **Given** a file with versions 1, 2, and 3, **When** the user restores version 1, **Then** a new version 4 is created with the same content as version 1.
2. **Given** a restore operation, **When** it completes, **Then** the original version history remains intact (no versions are deleted).
3. **Given** a restore operation, **When** it completes, **Then** the version record notes it was "Restored from version X".

---

### User Story 4 - Download Specific Version (Priority: P2)

A user downloads any specific version of a file from the version history, not just the current version.

**Independent Test**: Upload 3 versions of a file. Download version 2 specifically. Verify the downloaded content matches what was uploaded as version 2.

**Acceptance Scenarios**:

1. **Given** a file with multiple versions, **When** a user requests download of a specific version, **Then** the content of that version is returned.
2. **Given** a version download request, **When** the version exists, **Then** a presigned URL for the version's storage key is returned.

---

### User Story 5 - Compare Versions (Priority: P3)

A user compares two versions of a file by viewing their metadata side-by-side (size, uploader, timestamp, checksum). For text-based files, a diff view shows changes between versions.

**Independent Test**: Upload two text file versions with different content. Compare them and verify metadata differences are shown.

**Acceptance Scenarios**:

1. **Given** two versions of a file, **When** the user requests a comparison, **Then** metadata differences (size, uploader, timestamp) are displayed side-by-side.
2. **Given** two versions selected for comparison, **When** the system returns comparison data, **Then** it includes download URLs for both versions so the client can render diffs.

---

## Non-Functional Requirements

- **NFR-1**: Version history retrieval must respond within 200ms for files with up to 100 versions.
- **NFR-2**: Uploading a new version must not be significantly slower than a regular file upload (< 500ms additional overhead).
- **NFR-3**: Storage usage from versions counts against the organization's quota.
- **NFR-4**: Deleting a file (trash) preserves all versions; permanent delete removes all versions from storage.
