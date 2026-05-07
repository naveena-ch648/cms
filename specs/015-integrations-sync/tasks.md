# Tasks: Integrations & Sync

**Input**: Design documents from `/specs/015-integrations-sync/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependencies, and database schema

- [X] T001 Add Google Drive API and encryption dependencies to backend/pom.xml
- [X] T002 [P] Add integration configuration properties to backend/src/main/resources/application.yml
- [X] T003 [P] Create Flyway migration V22__integrations_webhooks.sql in backend/src/main/resources/db/migration/V22__integrations_webhooks.sql
- [X] T004 [P] Create frontend API client for integrations in frontend/src/api/integrations.ts
- [X] T005 [P] Create frontend API client for webhooks in frontend/src/api/webhooks.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities, repositories, and shared utilities that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 [P] Create IntegrationConnection entity in backend/src/main/java/com/cms/entity/IntegrationConnection.java
- [X] T007 [P] Create Webhook entity in backend/src/main/java/com/cms/entity/Webhook.java
- [X] T008 [P] Create WebhookDelivery entity in backend/src/main/java/com/cms/entity/WebhookDelivery.java
- [X] T009 [P] Create SyncLink entity in backend/src/main/java/com/cms/entity/SyncLink.java
- [X] T010 [P] Create SyncJob entity in backend/src/main/java/com/cms/entity/SyncJob.java
- [X] T011 [P] Create IntegrationConnectionRepository in backend/src/main/java/com/cms/repository/IntegrationConnectionRepository.java
- [X] T012 [P] Create WebhookRepository in backend/src/main/java/com/cms/repository/WebhookRepository.java
- [X] T013 [P] Create WebhookDeliveryRepository in backend/src/main/java/com/cms/repository/WebhookDeliveryRepository.java
- [X] T014 [P] Create SyncLinkRepository in backend/src/main/java/com/cms/repository/SyncLinkRepository.java
- [X] T015 [P] Create SyncJobRepository in backend/src/main/java/com/cms/repository/SyncJobRepository.java
- [X] T016 Create IntegrationTokenEncryptor utility (AES-256-GCM) in backend/src/main/java/com/cms/service/IntegrationTokenEncryptor.java
- [X] T017 [P] Create integration request/response DTOs in backend/src/main/java/com/cms/dto/integration/
- [X] T018 [P] Create webhook request/response DTOs in backend/src/main/java/com/cms/dto/webhook/

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Google Drive Import (Priority: P1) 🎯 MVP

**Goal**: Users can connect their Google Drive, browse files, and import them into CMS folders

**Independent Test**: Connect Google Drive → browse folders → select files → verify they appear in CMS with correct metadata

### Implementation for User Story 1

- [X] T019 [US1] Implement IntegrationService (OAuth flow, token management, Drive API calls) in backend/src/main/java/com/cms/service/IntegrationService.java
- [X] T020 [US1] Implement IntegrationController (connect, callback, connections list, disconnect, browse, import) in backend/src/main/java/com/cms/controller/IntegrationController.java
- [X] T021 [US1] Add import job processing to Python worker in worker/sync_worker.py (download from Drive → upload to MinIO → create file record)
- [X] T022 [P] [US1] Create GoogleDriveConnect component in frontend/src/components/integrations/GoogleDriveConnect.tsx
- [X] T023 [P] [US1] Create DriveFileBrowser component in frontend/src/components/integrations/DriveFileBrowser.tsx
- [X] T024 [US1] Create ImportDialog component in frontend/src/components/integrations/ImportDialog.tsx
- [X] T025 [US1] Create IntegrationsPage with connection management in frontend/src/pages/IntegrationsPage.tsx
- [X] T026 [US1] Add IntegrationsPage route to frontend/src/App.tsx

**Checkpoint**: User Story 1 fully functional — users can import from Google Drive

---

## Phase 4: User Story 2 — Webhook Event System (Priority: P1)

**Goal**: Admins can configure webhooks and CMS events are delivered to registered endpoints with signatures and retries

**Independent Test**: Register webhook URL → upload a file → verify endpoint receives signed payload within 30s

### Implementation for User Story 2

- [X] T027 [US2] Implement WebhookService (CRUD, event matching, dispatch to queue) in backend/src/main/java/com/cms/service/WebhookService.java
- [X] T028 [US2] Implement WebhookController (create, list, get, update, delete, test, deliveries, retry) in backend/src/main/java/com/cms/controller/WebhookController.java
- [X] T029 [US2] Create webhook_worker.py (consume from Redis queue, deliver with HMAC-SHA256, retry logic) in worker/webhook_worker.py
- [X] T030 [US2] Add webhook event dispatch hooks to existing file/folder/workflow services in backend (fire events on upload, delete, move, etc.)
- [X] T031 [P] [US2] Create WebhookManagement admin component in frontend/src/components/admin/WebhookManagement.tsx
- [X] T032 [US2] Add WebhookManagement to admin routes in frontend/src/pages/AdminPage.tsx

**Checkpoint**: User Story 2 fully functional — webhooks fire on CMS events

---

## Phase 5: User Story 3 — Export to Google Drive (Priority: P2)

**Goal**: Users can export CMS files back to their connected Google Drive

**Independent Test**: Select CMS files → choose Drive destination → verify files appear in Google Drive

### Implementation for User Story 3

- [X] T033 [US3] Add export endpoint to IntegrationController in backend/src/main/java/com/cms/controller/IntegrationController.java
- [X] T034 [US3] Add export logic to IntegrationService (upload files to Drive with conflict handling) in backend/src/main/java/com/cms/service/IntegrationService.java
- [X] T035 [US3] Add export job processing to Python worker in worker/sync_worker.py (download from MinIO → upload to Drive)
- [X] T036 [US3] Create ExportDialog component in frontend/src/components/integrations/ExportDialog.tsx
- [X] T037 [US3] Add export action to FileList context menu in frontend/src/components/FileList.tsx

**Checkpoint**: User Story 3 fully functional — users can export files to Drive

---

## Phase 6: User Story 4 — Google Drive Sync (Priority: P2)

**Goal**: Users can set up ongoing folder sync between CMS and Drive with conflict resolution

**Independent Test**: Create sync link → modify file in Drive → verify change appears in CMS within interval

### Implementation for User Story 4

- [X] T038 [US4] Implement SyncService (sync link CRUD, scheduler, conflict resolution) in backend/src/main/java/com/cms/service/SyncService.java
- [X] T039 [US4] Implement SyncController (create/list/update/delete sync links, job history) in backend/src/main/java/com/cms/controller/SyncController.java
- [X] T040 [US4] Implement SyncSchedulerService (@Scheduled, checks due sync links, enqueues jobs) in backend/src/main/java/com/cms/service/SyncSchedulerService.java
- [X] T041 [US4] Add bidirectional sync processing to worker/sync_worker.py (compare states, transfer deltas, handle conflicts)
- [X] T042 [P] [US4] Create SyncSetupDialog component in frontend/src/components/integrations/SyncSetupDialog.tsx
- [X] T043 [P] [US4] Create SyncDashboard admin component in frontend/src/components/admin/SyncDashboard.tsx
- [X] T044 [US4] Add SyncDashboard to admin routes in frontend/src/pages/AdminPage.tsx

**Checkpoint**: User Story 4 fully functional — bidirectional sync working

---

## Phase 7: User Story 5 — Webhook Management (Priority: P3)

**Goal**: Admins can edit, disable, test, and delete webhooks with full delivery visibility

**Independent Test**: Edit webhook URL → disable webhook → trigger event → verify no delivery → re-enable → trigger → verify delivery resumes

### Implementation for User Story 5

- [X] T045 [US5] Add webhook test delivery endpoint logic (send sample payload, return result) in backend/src/main/java/com/cms/service/WebhookService.java
- [X] T046 [US5] Add auto-disable logic (10 consecutive failures → disable + notification) in worker/webhook_worker.py
- [X] T047 [US5] Add delivery history detail view and manual retry UI in frontend/src/components/admin/WebhookManagement.tsx

**Checkpoint**: User Story 5 fully functional — complete webhook lifecycle management

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Docker, integration, and final validation

- [X] T048 [P] Update docker/docker-compose.yml with integration environment variables (GOOGLE_DRIVE_CLIENT_ID, CLIENT_SECRET, ENCRYPTION_KEY)
- [X] T049 [P] Update worker/Dockerfile and worker entry point to run webhook_worker and sync_worker processes
- [X] T050 Add integration and webhook navigation links to frontend sidebar/header
- [X] T051 Run quickstart.md validation (full flow: connect Drive, import, webhook delivery, sync)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–7)**: All depend on Foundational phase completion
  - US1 (Google Drive Import) and US2 (Webhooks) can proceed in parallel
  - US3 (Export) depends on US1 (reuses IntegrationService + OAuth connection)
  - US4 (Sync) depends on US1 (reuses IntegrationService + OAuth connection)
  - US5 (Webhook Management) depends on US2 (extends WebhookService)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational → **MVP candidate**
- **User Story 2 (P1)**: Can start after Foundational → **Independent of US1**
- **User Story 3 (P2)**: Requires US1 complete (reuses OAuth + Drive service)
- **User Story 4 (P2)**: Requires US1 complete (reuses OAuth + Drive service)
- **User Story 5 (P3)**: Requires US2 complete (extends webhook management)

### Within Each User Story

- Backend service before controller
- Controller before frontend components
- Worker processing can run in parallel with frontend
- Core implementation before integration with other stories

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational entities/repos/DTOs marked [P] can run in parallel
- US1 and US2 can proceed entirely in parallel after Foundational
- Within US1: GoogleDriveConnect and DriveFileBrowser components [P]
- Within US4: SyncSetupDialog and SyncDashboard components [P]
- Within US2: WebhookManagement component [P] with worker

---

## Parallel Example: User Story 1

```bash
# Launch all foundational entities in parallel:
Task: T006 "IntegrationConnection entity"
Task: T007 "Webhook entity"
Task: T008 "WebhookDelivery entity"
Task: T009 "SyncLink entity"
Task: T010 "SyncJob entity"

# Then all repositories in parallel:
Task: T011-T015 "All repositories"

# Then encryption utility + DTOs in parallel:
Task: T016 "IntegrationTokenEncryptor"
Task: T017 "Integration DTOs"
Task: T018 "Webhook DTOs"

# Then US1 implementation:
Task: T019 "IntegrationService" (sequential - core logic)
Task: T020 "IntegrationController" (depends on T019)
Task: T021 "Worker import processing" (parallel with frontend)
Task: T022 + T023 "Frontend connect + browser" (parallel)
Task: T024 "ImportDialog" (depends on T023)
Task: T025 "IntegrationsPage" (depends on T022-T024)
Task: T026 "Route" (depends on T025)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Google Drive Import)
4. **STOP and VALIDATE**: Test import flow end-to-end
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add US1 (Import) + US2 (Webhooks) in parallel → Test independently → Deploy (MVP!)
3. Add US3 (Export) → Test independently → Deploy
4. Add US4 (Sync) → Test independently → Deploy
5. Add US5 (Webhook Management) → Test independently → Deploy
6. Polish phase → Final validation

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Google Drive Import)
   - Developer B: User Story 2 (Webhooks)
3. After US1 complete:
   - Developer A: User Story 3 (Export) then User Story 4 (Sync)
4. After US2 complete:
   - Developer B: User Story 5 (Webhook Management)
5. Both developers: Polish phase
