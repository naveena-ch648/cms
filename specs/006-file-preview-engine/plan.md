# Implementation Plan: File Preview Engine

**Branch**: `006-file-preview-engine` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-file-preview-engine/spec.md`

## Summary

Build a file preview engine supporting PDF, images, video, Word, Excel, and PowerPoint files. Server-side Python workers generate previews and thumbnails stored in MinIO. React frontend renders previews using pdf.js for PDFs, native elements for images/video, and pre-rendered images for Office documents. Redis queues dispatch async preview generation jobs triggered on file upload.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (worker), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Boot, React, pdf.js, LibreOffice (headless), Pillow, python-pptx, redis, boto3  
**Storage**: MySQL 8.0 (metadata), MinIO (previews/thumbnails), Redis (job queue)  
**Testing**: JUnit 5 (backend), pytest (worker), manual/Vite (frontend)  
**Target Platform**: Docker Compose (Linux containers), browser (Chrome/Firefox/Safari)  
**Project Type**: Web application (3-tier: frontend + backend + worker)  
**Performance Goals**: PDF/image preview <3s, Office preview <10s, 50 concurrent preview jobs  
**Constraints**: 100MB max file size for preview, 256x256px thumbnails, generated previews cached in MinIO  
**Scale/Scope**: Multi-tenant CMS, ~3 new React components, 1 new API controller, 2 new worker processors

## Constitution Check

*GATE: Constitution is a placeholder template — no project-specific gates defined. Proceeding.*

No violations. The project constitution has not been customized with principles/gates.

## Project Structure

### Documentation (this feature)

```text
specs/006-file-preview-engine/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── preview-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   └── PreviewController.java        # Preview/thumbnail API endpoints
│   ├── dto/preview/
│   │   ├── PreviewDto.java               # Preview response DTO
│   │   ├── PreviewJobDto.java            # Job status DTO
│   │   └── ThumbnailDto.java             # Thumbnail response DTO
│   ├── entity/
│   │   ├── Preview.java                  # Preview entity
│   │   ├── PreviewJob.java               # Job tracking entity
│   │   └── Comment.java                  # File comment entity
│   ├── repository/
│   │   ├── PreviewRepository.java
│   │   ├── PreviewJobRepository.java
│   │   └── CommentRepository.java
│   └── service/
│       ├── PreviewService.java           # Preview business logic
│       ├── PreviewJobDispatcher.java     # Redis queue publisher
│       └── CommentService.java           # Comment CRUD
├── src/main/resources/db/migration/
│   └── V006__preview_system.sql          # Schema migration

frontend/
├── src/
│   ├── api/
│   │   ├── previews.ts                   # Preview API client
│   │   └── comments.ts                   # Comments API client
│   ├── components/preview/
│   │   ├── PreviewModal.tsx              # Full-screen preview overlay
│   │   ├── PdfViewer.tsx                 # PDF renderer (pdf.js)
│   │   ├── ImageViewer.tsx               # Image with zoom/pan
│   │   ├── VideoPlayer.tsx               # Video player wrapper
│   │   ├── OfficeViewer.tsx              # Office doc preview (rendered images)
│   │   ├── PreviewToolbar.tsx            # Zoom/page controls
│   │   └── MetadataPanel.tsx             # Side panel with metadata + comments
│   └── types/
│       └── preview.ts                    # Preview type definitions

worker/
├── processors/
│   ├── preview_pdf.py                    # PDF → page images
│   ├── preview_office.py                 # Office → PDF → images (LibreOffice)
│   ├── preview_video.py                  # Video → thumbnail frame
│   └── preview_image.py                  # Image → thumbnail resize
├── Dockerfile                            # Updated with LibreOffice + ffmpeg
└── requirements.txt                      # Updated dependencies
```

**Structure Decision**: Existing 3-tier architecture (backend/frontend/worker) is maintained. Preview-specific code is added within existing directory conventions.

## Complexity Tracking

No constitution violations to justify.
