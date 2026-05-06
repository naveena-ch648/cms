# Tasks: RBAC & Sharing System

**Input**: Design documents from `/specs/005-rbac-sharing/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓, contracts/ ✓, quickstart.md ✓

**Tests**: Not explicitly requested in spec — test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migrations and shared type definitions

- [ ] T001 Create migration `backend/src/main/resources/db/migration/V10__add_permission_inheritance.sql` to add `is_override` column to `folder_permissions` table
- [ ] T002 Create migration `backend/src/main/resources/db/migration/V11__create_shared_links_tables.sql` with `shared_links` and `shared_link_accesses` tables and indexes
- [ ] T003 [P] Create permission DTO package `backend/src/main/java/com/cms/dto/permission/` with request/response classes
- [ ] T004 [P] Create sharing DTO package `backend/src/main/java/com/cms/dto/sharing/` with request/response classes
- [ ] T005 [P] Create frontend type definitions in `frontend/src/types/permission.ts`
- [ ] T006 [P] Create frontend type definitions in `frontend/src/types/sharing.ts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Extend `backend/src/main/java/com/cms/entity/FolderPermission.java` with `isOverride` field (Boolean, default FALSE)
- [ ] T008 Create `backend/src/main/java/com/cms/entity/SharedLink.java` entity with all fields from data-model (uuid, token, resourceType, fileId, folderId, createdBy, workspaceId, passwordHash, expiresAt, allowDownload, watermarkEnabled, maxViews, viewCount, status, lastAccessedAt)
- [ ] T009 Create `backend/src/main/java/com/cms/entity/SharedLinkAccess.java` entity with fields (sharedLink, accessedAt, ipAddress, userAgent)
- [ ] T010 [P] Create `backend/src/main/java/com/cms/repository/SharedLinkRepository.java` with queries: findByToken, findByCreatedByAndWorkspaceId, findByWorkspaceIdAndStatus
- [ ] T011 [P] Create `backend/src/main/java/com/cms/repository/SharedLinkAccessRepository.java` with queries: findBySharedLinkId (paginated)
- [ ] T012 Extend `backend/src/main/java/com/cms/repository/FolderPermissionRepository.java` with ancestry-based queries: findByFolderIdInAndUserId, findByFolderIdInAndGroupIdIn

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — File & Folder Permission Management (Priority: P1) 🎯 MVP

**Goal**: Implement folder-level RBAC with inheritance resolution and navigation filtering

**Independent Test**: Admin assigns "Editor" permission to User B on "Project X" folder. User B can upload/edit files in that folder but cannot manage permissions. User C (with no permission) cannot see the folder at all.

### Implementation for User Story 1

- [ ] T013 [US1] Extend `backend/src/main/java/com/cms/service/FolderPermissionService.java` with `resolveEffectivePermission(userId, folderId)` method implementing closest-ancestor-wins algorithm (walk ancestry path, check direct + group permissions, cache result in Redis with 5min TTL)
- [ ] T014 [US1] Implement cache invalidation in `FolderPermissionService` — on permission change, invalidate `folder_perm:{userId}:{folderId}` for affected folder and all descendants
- [ ] T015 [US1] Add `assignPermission(folderUuid, userUuid/groupUuid, roleUuid, isOverride)` method to `FolderPermissionService`
- [ ] T016 [US1] Add `removePermission(folderUuid, permissionId)` method to `FolderPermissionService`
- [ ] T017 [US1] Create `backend/src/main/java/com/cms/service/PermissionFilterService.java` with `filterFolders(List<Folder>, userId)` and `filterFiles(List<FileEntity>, userId)` methods using batch permission resolution
- [ ] T018 [US1] Create `backend/src/main/java/com/cms/controller/PermissionController.java` with endpoints: GET/POST/DELETE `/api/v1/folders/{folderUuid}/permissions` and GET `/api/v1/folders/{folderUuid}/effective-permission`
- [ ] T019 [US1] Create `backend/src/main/java/com/cms/security/PermissionInterceptor.java` — Spring interceptor that filters folder/file listings on responses from `GET /api/v1/workspaces/{id}/folders`, `GET /api/v1/folders/{id}/children`, `GET /api/v1/folders/{id}/files`
- [ ] T020 [US1] Register `PermissionInterceptor` in `backend/src/main/java/com/cms/config/SecurityConfig.java` or WebMvcConfigurer
- [ ] T021 [US1] Create `frontend/src/api/permissions.ts` with functions: listPermissions, assignPermission, removePermission, getEffectivePermission
- [ ] T022 [US1] Create `frontend/src/components/PermissionDialog.tsx` — modal for assigning/viewing/removing folder permissions (user/group selector, role dropdown)

**Checkpoint**: Permission assignment, inheritance resolution, and navigation filtering are fully functional

---

## Phase 4: User Story 2 — Permission Inheritance Override (Priority: P1)

**Goal**: Allow explicit overrides on inherited permissions at any hierarchy level

**Independent Test**: User has "Editor" on "Department" folder (inherited to all subfolders). Admin sets "Viewer" override on "Confidential" subfolder. User can edit files in "Department" but only view files in "Confidential".

### Implementation for User Story 2

- [ ] T023 [US2] Update `resolveEffectivePermission` in `backend/src/main/java/com/cms/service/FolderPermissionService.java` to prioritize `is_override = TRUE` entries at each level (closest override wins over inherited non-override)
- [ ] T024 [US2] Add `setOverride(folderUuid, userUuid/groupUuid, roleUuid)` and `removeOverride(folderUuid, permissionId)` methods to `FolderPermissionService`
- [ ] T025 [US2] Update `PermissionController` POST endpoint to accept `isOverride` flag and validate override semantics
- [ ] T026 [US2] Update `frontend/src/components/PermissionDialog.tsx` to show inherited vs override status and allow toggling override on existing permissions

**Checkpoint**: Override permissions work at any hierarchy level; removing override reverts to inherited permission

---

## Phase 5: User Story 3 — External Sharing via Secure Links (Priority: P2)

**Goal**: Enable creating secure, configurable share links for files/folders accessible without authentication

**Independent Test**: User creates a share link for "Proposal.pdf" with password and 7-day expiry. External person accesses link, enters password, views document. After 7 days, link no longer works.

### Implementation for User Story 3

- [ ] T027 [US3] Create `backend/src/main/java/com/cms/service/SharedLinkService.java` with `createLink(request, userId)` — generates cryptographic token (SecureRandom 32 bytes hex), bcrypt hashes password if set, validates creator has Editor+ permission
- [ ] T028 [US3] Add `accessLink(token)` method to `SharedLinkService` — validates token exists, checks status/expiry, returns resource metadata or password-required flag, increments viewCount
- [ ] T029 [US3] Add `verifyPassword(token, password)` method to `SharedLinkService` — bcrypt compares, returns session token for protected downloads
- [ ] T030 [US3] Add `revokeLink(uuid, userId)` method to `SharedLinkService` — sets status=REVOKED, invalidates Redis cache
- [ ] T031 [US3] Add `generateDownloadUrl(token, session)` method to `SharedLinkService` — generates MinIO pre-signed URL (15min expiry) if allowDownload=true
- [ ] T032 [US3] Create `backend/src/main/java/com/cms/controller/SharedLinkController.java` with authenticated endpoints: POST/GET/PATCH/DELETE `/api/v1/share-links`
- [ ] T033 [US3] Create public endpoints in `SharedLinkController`: GET `/api/share/{token}`, POST `/api/share/{token}/verify`, GET `/api/share/{token}/download`
- [ ] T034 [US3] Configure Spring Security in `SecurityConfig.java` to permit `/api/share/**` without authentication
- [ ] T035 [US3] Add Redis caching for share link metadata with key `share_link:{token}`, 2min TTL, invalidate on update/revoke
- [ ] T036 [US3] Create `frontend/src/api/sharing.ts` with functions: createShareLink, listShareLinks, updateShareLink, revokeShareLink, getShareLinkAccesses
- [ ] T037 [US3] Create `frontend/src/components/ShareLinkDialog.tsx` — modal for creating/editing share links (password, expiry, download toggle, watermark toggle)

**Checkpoint**: Share links can be created, accessed publicly with password/expiry validation, and revoked

---

## Phase 6: User Story 4 — Watermark on Shared Content (Priority: P3)

**Goal**: Apply watermark overlay on shared content when watermark option is enabled

**Independent Test**: User shares a PDF with watermark enabled. External viewer previews the PDF and sees a diagonal watermark with the link ID.

### Implementation for User Story 4

- [ ] T038 [US4] Add watermark job type to `worker/worker.py` — listen for `watermark` jobs on Redis queue
- [ ] T039 [US4] Create `worker/processors/watermark.py` — applies diagonal text watermark to images (Pillow) and PDFs (reportlab/PyPDF) with link ID as text
- [ ] T040 [US4] Update `SharedLinkService.accessLink()` to trigger watermark job when `watermarkEnabled=true` and return watermarked preview URL
- [ ] T041 [US4] Update `SharedLinkService.generateDownloadUrl()` to serve watermarked file version when `watermarkEnabled=true`

**Checkpoint**: Watermarked previews and downloads work for shared content with watermark enabled

---

## Phase 7: User Story 5 — Share Link Management Dashboard (Priority: P2)

**Goal**: Provide a dashboard for users to view, manage, and track their share links

**Independent Test**: User views their share links dashboard. They see 3 active links with view counts. They revoke one link and extend another's expiry by 7 days.

### Implementation for User Story 5

- [ ] T042 [US5] Add `listLinks(userId, workspaceUuid, status, pageable)` method to `SharedLinkService` — returns paginated links; workspace admins see all links
- [ ] T043 [US5] Add `updateLink(uuid, userId, updateRequest)` method to `SharedLinkService` — update password, expiry, allowDownload, watermark; reactivate expired links when expiry extended
- [ ] T044 [US5] Add `getAccessLog(uuid, userId, pageable)` method to `SharedLinkService` — returns paginated access events
- [ ] T045 [US5] Add GET `/api/v1/share-links/{uuid}/accesses` endpoint to `SharedLinkController`
- [ ] T046 [US5] Create `frontend/src/components/ShareLinkDashboard.tsx` — list view with status badges, view counts, last accessed time, actions (revoke, edit, view accesses)
- [ ] T047 [US5] Integrate `ShareLinkDashboard` into workspace page navigation in `frontend/src/pages/WorkspacePage.tsx`

**Checkpoint**: Users can view, manage, and track all their share links from the dashboard

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T048 [P] Add audit logging for permission changes (assign/revoke/override) in `FolderPermissionService` using existing `AuditService`
- [ ] T049 [P] Add audit logging for share link lifecycle events (create/revoke/access) in `SharedLinkService`
- [ ] T050 Add cleanup job for expired share links — update status to EXPIRED for links past `expires_at` (scheduled task or worker job)
- [ ] T051 Run quickstart.md validation flows against running application

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migrations must exist before entities reference new columns)
- **User Story 1 (Phase 3)**: Depends on Phase 2 — BLOCKS Phase 4
- **User Story 2 (Phase 4)**: Depends on Phase 3 (extends US1's permission resolution logic)
- **User Story 3 (Phase 5)**: Depends on Phase 2 only — can run in parallel with Phase 3/4
- **User Story 4 (Phase 6)**: Depends on Phase 5 (needs share link infrastructure)
- **User Story 5 (Phase 7)**: Depends on Phase 5 (needs share link infrastructure)
- **Polish (Phase 8)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Foundation → US1 (independent)
- **US2 (P1)**: Foundation → US1 → US2 (extends US1's resolution algorithm)
- **US3 (P2)**: Foundation → US3 (independent of US1/US2)
- **US4 (P3)**: Foundation → US3 → US4 (needs share link service)
- **US5 (P2)**: Foundation → US3 → US5 (needs share link service)

### Parallel Opportunities

- **Phase 1**: T003, T004, T005, T006 can all run in parallel
- **Phase 2**: T010, T011 can run in parallel; T008, T009 can run in parallel
- **Phase 3 + Phase 5**: US1 and US3 can be worked on simultaneously after Foundation
- **Phase 6 + Phase 7**: US4 and US5 can be worked on simultaneously after US3

---

## Parallel Example: After Foundation

```
Stream A (Permission RBAC):          Stream B (Sharing):
  Phase 3: US1 Permission Mgmt        Phase 5: US3 Share Links
  Phase 4: US2 Override Logic          Phase 6: US4 Watermark
                                       Phase 7: US5 Dashboard
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (migrations + DTOs)
2. Complete Phase 2: Foundational (entities + repositories)
3. Complete Phase 3: US1 — Permission Management
4. Complete Phase 4: US2 — Override Logic
5. **STOP and VALIDATE**: Test inheritance resolution independently
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Permission assignment + filtering works (MVP!)
3. Add US2 → Override support complete
4. Add US3 → External sharing via links
5. Add US5 → Dashboard for link management
6. Add US4 → Watermark (optional polish)

### Parallel Team Strategy

With two developers:

1. Both complete Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 → US2 (permission stream)
   - Developer B: US3 → US5 → US4 (sharing stream)
3. Stories complete and integrate independently
