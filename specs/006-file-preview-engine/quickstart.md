# Quickstart: File Preview Engine

**Feature**: 006-file-preview-engine  
**Prerequisites**: Steps 1–5 completed (multi-tenant, folders, files, versioning, RBAC/sharing)

## Development Setup

### 1. Start infrastructure

```bash
cd docker
docker compose up -d mysql redis minio
```

### 2. Backend

```bash
cd backend
mvn compile
# Flyway auto-runs V006 migration on startup
mvn spring-boot:run
```

Backend runs on http://localhost:8080

### 3. Worker (Python)

```bash
cd worker
pip install -r requirements.txt
python worker.py
```

Worker requires LibreOffice and FFmpeg installed locally for Office/video processing:
```bash
# macOS
brew install libreoffice ffmpeg

# Ubuntu/Debian
sudo apt install libreoffice ffmpeg

# Docker (handled by Dockerfile)
apk add --no-cache libreoffice ffmpeg
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173 (dev) or http://localhost:3000 (Docker)

New dependency to install:
```bash
npm install pdfjs-dist
```

## Key Workflows

### File Upload → Preview Generation

1. User uploads file via existing upload API
2. Backend saves file to MinIO, creates DB record
3. Backend dispatches `{action: "preview"}` message to Redis `file:process` queue
4. Worker picks up job:
   - **Image**: Resize to 256x256 thumbnail → store in MinIO
   - **PDF**: Extract first page as thumbnail + render all pages as images
   - **Video**: Extract frame at 2s → store as thumbnail
   - **Office**: LibreOffice convert to PDF → render pages as images
5. Worker updates `previews` table with storage keys and status=COMPLETED
6. Frontend fetches `GET /api/v1/files/{id}/preview` → gets presigned URLs

### Preview Display in Browser

1. User clicks file in workspace → PreviewModal opens
2. Frontend calls `GET /api/v1/files/{id}/preview`
3. Based on file type:
   - **PDF**: Load with pdf.js, render pages on canvas
   - **Image**: Display with zoom/pan controls
   - **Video**: HTML5 `<video>` with presigned URL source
   - **Office**: Display pre-rendered page images with navigation

### Comment Flow

1. User opens file preview → MetadataPanel loads
2. Frontend calls `GET /api/v1/files/{id}/comments`
3. User types comment → `POST /api/v1/files/{id}/comments`
4. Comment appears in list immediately (optimistic update)

## Testing

### Backend
```bash
cd backend
mvn test
```

### Worker
```bash
cd worker
pytest tests/
```

### Manual E2E
1. Upload a PDF, image, docx, xlsx, pptx, and mp4
2. Wait 30s for worker processing
3. Click each file → verify preview renders
4. Check folder view → verify thumbnails display
5. Open preview → post a comment → verify it persists

## Docker (Full Stack)

```bash
cd docker
docker compose up -d --build
```

All services start including the worker with LibreOffice and FFmpeg pre-installed.

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| PREVIEW_MAX_FILE_SIZE_MB | 100 | Max file size for preview generation |
| PREVIEW_THUMBNAIL_SIZE | 256 | Thumbnail width/height in pixels |
| PREVIEW_PAGE_DPI | 150 | DPI for PDF/Office page rendering |
| PREVIEW_MAX_PAGES | 100 | Max pages to render for a document |
| PREVIEW_PRESIGN_EXPIRY_HOURS | 1 | Presigned URL expiry time |
