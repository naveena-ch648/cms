# Research: File Upload & Storage System

**Feature**: 003-file-upload-storage  
**Date**: 2026-05-05

---

## R1: Object Storage — MinIO (S3-Compatible)

**Decision**: MinIO as the S3-compatible object storage backend.

**Rationale**:
- User explicitly specified MinIO (S3-compatible) as the storage backend
- MinIO provides full S3 API compatibility, enabling future migration to AWS S3 or any S3-compatible service with zero code changes
- Self-hosted, fits the existing Docker Compose infrastructure pattern
- Supports multipart uploads natively (S3 multipart upload API)
- Supports presigned URLs for direct browser-to-storage uploads
- Free, open-source, production-grade

**Alternatives Considered**:
- AWS S3: Cloud-dependent, costs money for development, but MinIO's S3 compatibility means migration is trivial
- Local filesystem: No scalability, no multi-node support, no built-in multipart uploads

**Integration Pattern**:
- Spring Boot backend uses AWS SDK for Java v2 (`software.amazon.awssdk:s3`) configured to point at MinIO endpoint
- MinIO runs as a Docker Compose service alongside MySQL and Redis
- Bucket-per-organization strategy for tenant isolation
- Object key format: `{org_id}/{workspace_id}/{folder_path}/{file_uuid}_{filename}`

---

## R2: Chunked/Resumable Upload Strategy

**Decision**: Server-managed chunked upload with Redis-tracked sessions.

**Rationale**:
- User specified "Support chunked uploads" and spec requires resumable uploads (FR-005 through FR-008)
- Chunk size: 5 MB (MinIO/S3 multipart minimum), configurable up to 100 MB
- Backend initiates S3 multipart upload, returns upload session ID + presigned URLs for each chunk
- Frontend uploads chunks directly to MinIO via presigned URLs (reduces backend bandwidth)
- Backend tracks chunk completion in Redis with 24h TTL (matches FR-008 expiry)
- On completion, backend calls S3 CompleteMultipartUpload and writes metadata to MySQL
- On expiry, a scheduled job calls S3 AbortMultipartUpload and cleans Redis

**Alternatives Considered**:
- tus.io protocol: Adds protocol complexity; S3 multipart is already resumable and well-supported
- Backend-proxied chunks: Doubles bandwidth usage; presigned URLs let frontend upload directly to MinIO
- Client-side assembly: Security risk, defeats server-side validation

**Flow**:
1. Frontend: `POST /api/files/upload/initiate` → receives `uploadSessionId`, chunk count, presigned URLs
2. Frontend: uploads chunks directly to MinIO presigned URLs (parallel, up to 3 concurrent)
3. Frontend: `POST /api/files/upload/complete` with session ID → backend verifies all chunks, calls CompleteMultipartUpload
4. For small files (<100 MB): single `POST /api/files/upload` with multipart/form-data to backend (proxied to MinIO)

---

## R3: Async Processing — Redis Queue → Python Workers

**Decision**: Redis Lists (BRPOP) as job queue, Python worker processes for post-upload tasks.

**Rationale**:
- User explicitly specified "Redis queue → Python workers"
- Redis is already in the stack; using Redis Lists (LPUSH/BRPOP) as a lightweight job queue avoids adding RabbitMQ/Kafka
- Python workers handle post-upload processing: thumbnail generation, metadata extraction, file type validation
- Workers run as a separate Docker Compose service
- Dead-letter pattern: failed jobs moved to `{queue}:dead` after 3 retries with exponential backoff

**Worker Responsibilities**:
1. **Thumbnail generation**: For images (JPEG, PNG, GIF, WebP) — uses Pillow
2. **Metadata extraction**: File type validation (python-magic), image dimensions, PDF page count
3. **Virus/malware scan placeholder**: Hook point for future integration (out of scope per spec)

**Queue Design**:
- Queue name: `file:process` (Redis List)
- Job payload: JSON with `{fileId, orgId, action, metadata}`
- Worker count: configurable (default 2 workers per container)
- Retry: 3 attempts, exponential backoff (5s, 25s, 125s)
- Dead-letter: `file:process:dead`

**Alternatives Considered**:
- Spring Boot @Async: No language separation, Python has better libraries for image/file processing
- Celery: Heavier framework, requires Redis/RabbitMQ broker setup — Redis Lists are simpler for this use case
- AWS SQS: Cloud-dependent, overkill for current scale

---

## R4: File Metadata Storage in MySQL

**Decision**: Store all file metadata in MySQL with JPA entities; file content in MinIO.

**Rationale**:
- User specified "Store metadata in MySQL"
- Consistent with existing data model (organizations, workspaces, folders in MySQL)
- File table references folder (from 002), workspace, and organization
- Upload session tracking in Redis (ephemeral) with completed file records in MySQL (permanent)
- Trash/soft-delete with scheduled cleanup aligns with existing `status` ENUM pattern

**Schema Strategy**:
- `files` table: Core file metadata (name, size, MIME type, storage key, status, folder reference)
- `upload_sessions` table: Persistent record of chunked uploads for audit trail (Redis handles active state)
- `trash_entries` view or status-based query on files table
- Storage quota tracked via aggregate queries + Redis cache for hot path

---

## R5: Frontend Upload UX — React with Drag-and-Drop

**Decision**: Native HTML5 File API + drag-and-drop with custom React upload manager.

**Rationale**:
- Existing React 18 frontend with Axios for API calls
- HTML5 `ondragover`/`ondrop` events for drag-and-drop zone
- `FileReader` + chunking logic for large files
- XMLHttpRequest (not Axios) for upload progress tracking (`onUploadProgress` with real percentage)
- Upload queue manager component: manages concurrent uploads, pause/resume, retry
- No additional library needed (react-dropzone is lightweight alternative if needed)

**UX Pattern**:
- Upload button + drag-and-drop zone in folder content area
- Upload progress panel (bottom drawer): shows all active/queued/completed uploads
- Individual progress bars per file with pause/resume/cancel controls
- Duplicate filename detection with rename/replace/skip dialog

---

## R6: Security & Permission Model

**Decision**: Extend existing folder permission system to file operations.

**Rationale**:
- File operations inherit folder permissions (from 002-workspace-folder-system)
- Upload requires `FILE_UPLOAD` permission on target folder
- Download requires `FILE_DOWNLOAD` permission
- Delete/move/rename requires `FILE_MANAGE` permission
- New permissions seeded via Flyway migration
- Presigned URLs include time-limited tokens (15 min for upload, 1 hour for download)
- File type restrictions enforced at organization policy level (existing `policies` JSON field)

---

## R7: SFTP — Deferred (P3)

**Decision**: Defer SFTP implementation to a follow-up iteration.

**Rationale**:
- Spec marks SFTP as P3 priority
- Requires a separate SFTP server process (Apache MINA SSHD or similar)
- Complex virtual filesystem mapping
- P1 (Web UI upload, chunked upload) and P2 (API upload, download/preview, file management) cover 90%+ of use cases
- Architecture will accommodate SFTP by exposing the same FileService interface

---

## R8: Preview Strategy

**Decision**: Frontend-rendered previews using presigned download URLs.

**Rationale**:
- Images: `<img>` tag with presigned URL (JPEG, PNG, GIF, WebP)
- PDFs: `<iframe>` or `<embed>` with presigned URL (browser's native PDF renderer)
- Text: Fetch content via API, render in `<pre>` block
- No server-side rendering needed for these types
- Presigned URLs expire after 1 hour, regenerated on demand
- Thumbnails (generated by Python workers) cached in MinIO for folder listing previews
