# Implementation Plan: Integrations & Sync

**Branch**: `015-integrations-sync` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/015-integrations-sync/spec.md`

## Summary

Build Google Drive integration (import/export/sync) and a webhook event delivery system. Users can connect their Google Drive via OAuth2, browse and import files, export CMS files back to Drive, and set up bidirectional folder sync. Administrators can configure webhooks that receive real-time event notifications with HMAC signature verification, retry logic, and delivery history tracking.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (sync workers), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Boot 3.3.5, Google Drive API v3 (google-api-services-drive), Spring Data JPA, Spring Data Redis, React 18, Axios 1.7.7, Vite 6; Python: google-auth, google-api-python-client, redis-py, boto3  
**Storage**: MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V22), Redis 7 (port 6379) for job queues and webhook delivery scheduling, MinIO (file content)  
**Testing**: JUnit 5 + Mockito (backend), pytest (workers), Vitest (frontend)  
**Target Platform**: Docker containers (linux/amd64), accessed via browser  
**Project Type**: Web service (multi-tenant SaaS)  
**Performance Goals**: Webhook delivery <30s from trigger event; import 100 files in <5 minutes; sync detection within configured interval  
**Constraints**: Google Drive API rate limit (12,000 requests/minute/project); webhook retries must use exponential backoff; OAuth tokens encrypted at rest  
**Scale/Scope**: Multi-tenant; per-organization Google OAuth credentials; webhook delivery workers horizontally scalable

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular connector/webhook services with clear interfaces |
| II. Testing Standards | ✅ PASS | Mock Google Drive API; test webhook delivery + retries |
| III. UX Consistency | ✅ PASS | Import/export dialogs follow existing CMS patterns; feedback states |
| IV. Performance & Scalability | ✅ PASS | Async workers for import/sync; Redis queue for webhook delivery |
| V. Reliability & Fault Tolerance | ✅ PASS | Retry with backoff; dead-letter for failed webhooks; idempotent sync |
| VI. Security & Compliance | ✅ PASS | OAuth tokens encrypted; webhook secrets HMAC-SHA256; audit logged |
| VII. Data & AI Governance | ✅ PASS | File lineage tracked from external source |
| VIII. Observability | ✅ PASS | Delivery logs; sync job history; error tracking |
| IX. Developer Experience | ✅ PASS | Local dev with mock Google API; Docker single-command |
| X. Continuous Improvement | ✅ PASS | Metrics on delivery success rate, sync latency |

**Gate Result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/015-integrations-sync/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── integrations-api.md
│   └── webhooks-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   ├── IntegrationController.java      # OAuth connect/disconnect, import/export
│   │   ├── WebhookController.java          # CRUD webhooks, delivery history
│   │   └── SyncController.java             # Sync link CRUD, status
│   ├── dto/
│   │   ├── integration/                    # Request/response DTOs
│   │   └── webhook/                        # Request/response DTOs
│   ├── entity/
│   │   ├── IntegrationConnection.java
│   │   ├── Webhook.java
│   │   ├── WebhookDelivery.java
│   │   ├── SyncLink.java
│   │   └── SyncJob.java
│   ├── repository/
│   │   ├── IntegrationConnectionRepository.java
│   │   ├── WebhookRepository.java
│   │   ├── WebhookDeliveryRepository.java
│   │   ├── SyncLinkRepository.java
│   │   └── SyncJobRepository.java
│   └── service/
│       ├── IntegrationService.java         # Google Drive operations
│       ├── WebhookService.java             # Webhook CRUD + dispatch
│       ├── WebhookDeliveryService.java     # Async delivery with retries
│       └── SyncService.java                # Sync orchestration
├── src/main/resources/db/migration/
│   └── V22__integrations_webhooks.sql
└── src/test/java/com/cms/

frontend/
├── src/
│   ├── api/
│   │   ├── integrations.ts
│   │   └── webhooks.ts
│   ├── components/integrations/
│   │   ├── GoogleDriveConnect.tsx
│   │   ├── DriveFileBrowser.tsx
│   │   ├── ImportDialog.tsx
│   │   ├── ExportDialog.tsx
│   │   └── SyncSetupDialog.tsx
│   ├── components/admin/
│   │   ├── WebhookManagement.tsx
│   │   └── SyncDashboard.tsx
│   └── pages/
│       └── IntegrationsPage.tsx

worker/
├── sync_worker.py                          # Google Drive sync processor
└── webhook_worker.py                       # Webhook delivery processor
```

**Structure Decision**: Extends existing multi-module layout (backend/frontend/worker). New entities and services added to backend. Sync and webhook delivery workers added to the Python worker module.

## Complexity Tracking

> No violations requiring justification.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
