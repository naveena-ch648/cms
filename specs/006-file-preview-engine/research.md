# Research: File Preview Engine

**Feature**: 006-file-preview-engine  
**Date**: 2026-05-06  
**Status**: Complete

## R1: PDF Rendering in Browser

**Decision**: Use pdf.js (Mozilla) via `pdfjs-dist` npm package

**Rationale**:
- Industry standard for browser-based PDF rendering
- Open source (Apache 2.0), actively maintained by Mozilla
- Renders PDF pages to canvas with full text layer support
- Supports zoom, page navigation, text selection natively
- Works client-side — no server rendering needed for PDF display
- Already used by Firefox, VS Code PDF viewer, and thousands of web apps

**Alternatives considered**:
- **react-pdf** (wrapper around pdf.js): Adds React abstraction but limited customization. Opted for direct pdf.js for more control over zoom/page UX.
- **Server-side PDF→image conversion**: Unnecessary overhead since pdf.js renders natively. Would increase storage and latency.
- **Google Docs Viewer / Office Online embed**: Requires internet access, doesn't work for private/on-prem files.

**Implementation notes**:
- Install `pdfjs-dist` package
- Use canvas rendering with `pdfjsLib.getDocument()` API
- Lazy-load pages on scroll for performance
- Generate PDF first-page thumbnail server-side for folder listings

---

## R2: Office Document Conversion (Word, Excel, PPT)

**Decision**: Use LibreOffice Headless in the Python worker container to convert Office docs to PDF, then render pages as images

**Rationale**:
- LibreOffice supports .docx, .xlsx, .pptx with high fidelity formatting
- Runs in Docker without a display server (`libreoffice --headless --convert-to pdf`)
- No external API calls or licensing costs
- Mature, battle-tested in document conversion pipelines
- PDF output can be rendered page-by-page in the same pdf.js viewer OR pre-rendered to images

**Alternatives considered**:
- **Apache POI (Java)**: Only extracts data, doesn't render visuals. Would need custom rendering logic.
- **Aspose**: Commercial license ($$$), proprietary. Overkill for preview.
- **Google Docs API**: Requires Google Cloud account, doesn't work offline/on-prem.
- **OnlyOffice**: Full editor suite, heavy for preview-only use case.

**Implementation notes**:
- Worker Dockerfile adds: `RUN apk add --no-cache libreoffice`
- Conversion pipeline: `.docx → .pdf → page images (Pillow/pdf2image)`
- Store rendered page images in MinIO under `previews/{fileId}/{versionId}/page-{n}.png`
- Excel: Convert to PDF (each sheet as a page)
- PPT: Convert to PDF (each slide as a page)

---

## R3: Video Thumbnail Extraction

**Decision**: Use FFmpeg to extract a frame at 2 seconds (or first keyframe) as the thumbnail

**Rationale**:
- FFmpeg is the universal video processing tool
- Single command extracts a frame: `ffmpeg -i input.mp4 -ss 00:00:02 -frames:v 1 thumb.jpg`
- Supports all common formats (MP4, WebM, MOV, AVI, MKV)
- Lightweight — only extracts one frame, doesn't transcode
- Already available in Alpine Linux packages

**Alternatives considered**:
- **OpenCV (Python)**: Heavier dependency for a single-frame extraction task. Better suited for analysis pipelines.
- **MoviePy**: Python wrapper around FFmpeg with overhead. Direct FFmpeg is simpler.
- **Browser-native (video element + canvas)**: Only works client-side, can't generate thumbnails at upload time for folder views.

**Implementation notes**:
- Worker Dockerfile adds: `RUN apk add --no-cache ffmpeg`
- Extract frame → resize to 256x256 → store in MinIO under `thumbnails/{fileId}/{versionId}.jpg`
- For video preview in browser: stream directly from MinIO presigned URL using HTML5 `<video>` element (no server transcoding needed)

---

## R4: Image Thumbnail Generation

**Decision**: Use Pillow (Python Imaging Library) to resize images to 256x256 thumbnails

**Rationale**:
- Already in use in the worker (watermark processor uses Pillow)
- Fast, lightweight, supports JPEG/PNG/GIF/WebP/TIFF
- `Image.thumbnail((256, 256))` maintains aspect ratio
- Can handle EXIF orientation correction

**Alternatives considered**:
- **ImageMagick**: External process call, heavier than Pillow for simple resize.
- **Sharp (Node.js)**: Would require a separate Node worker or moving thumbnail logic to backend.

**Implementation notes**:
- Resize maintaining aspect ratio, pad or crop to 256x256
- Output format: JPEG (quality 85) for photos, PNG for graphics with transparency
- Store under `thumbnails/{fileId}/{versionId}.jpg`

---

## R5: Preview Storage Structure in MinIO

**Decision**: Store previews and thumbnails in dedicated bucket paths within the existing organization bucket

**Rationale**:
- Reuses existing MinIO infrastructure and bucket-per-org pattern
- Presigned URLs provide secure time-limited access (same pattern as file downloads)
- Separating previews from originals avoids accidental deletion/modification

**Storage layout**:
```
{org-bucket}/
├── files/            # Original uploaded files (existing)
├── thumbnails/       # 256x256 thumbnails
│   └── {fileUuid}/
│       └── {versionId}.jpg
├── previews/         # Full preview assets
│   └── {fileUuid}/
│       └── {versionId}/
│           ├── page-1.png
│           ├── page-2.png
│           └── ...
└── watermarked/      # Watermarked files (existing)
```

**Alternatives considered**:
- **Separate preview bucket**: Adds bucket management complexity. Single bucket with path separation is simpler.
- **Inline with file storage**: Mixing previews with originals complicates cleanup and quota tracking.
- **CDN/cache layer**: Over-engineering for current scale. Presigned URLs with MinIO are sufficient.

---

## R6: Preview Job Queue Design

**Decision**: Extend existing Redis `file:process` queue with new action types (`preview`, `thumbnail`)

**Rationale**:
- Worker already consumes `file:process` queue with action-based dispatch
- Adding new actions keeps the architecture consistent
- Deduplication via Redis SET to prevent duplicate processing

**Job message format**:
```json
{
  "fileId": "uuid",
  "versionId": "uuid", 
  "organizationId": "uuid",
  "action": "preview",
  "mimeType": "application/pdf",
  "_retries": 0
}
```

**Alternatives considered**:
- **Separate queue per action**: More complex, harder to manage priorities. Single queue with action routing is sufficient.
- **Database-backed job table**: Adds DB polling overhead. Redis BRPOP is immediate and proven in this codebase.

---

## R7: Comment System (Inline Preview)

**Decision**: Simple threaded comments stored in MySQL, displayed in preview side panel. Full collaboration deferred to Step 7.

**Rationale**:
- Comments are file-scoped (not page/annotation-scoped) for now
- Simple parent-child threading is sufficient for review workflows
- MySQL storage with JPA entity is consistent with existing patterns
- Step 7 will enhance with mentions, tasks, activity feeds

**Implementation notes**:
- `comments` table: id, uuid, file_id, user_id, parent_id, content, created_at
- REST API: GET/POST/DELETE on `/api/v1/files/{fileId}/comments`
- Frontend: Simple comment list in MetadataPanel component
