# Tasks: Workspace Folder System

**Input**: Design documents from `/specs/002-workspace-folder-system/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema and new folder permissions seed data

- [x] T001 Create Flyway migration V4__create_folder_tables.sql with folders, folder_permissions, folder_favorites, folder_recents tables in backend/src/main/resources/db/migration/V4__create_folder_tables.sql
- [x] T002 Create Flyway migration V5__seed_folder_permissions.sql to seed folder-related permissions (manage-folders, view-folders) in backend/src/main/resources/db/migration/V5__seed_folder_permissions.sql

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: JPA entities, repositories, DTOs, and shared types that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Create Folder entity with self-referencing parent_id, workspace relationship, status enum, uuid, and sort_order in backend/src/main/java/com/cms/entity/Folder.java
- [x] T004 [P] Create FolderPermission entity with folder, user, group, and role relationships in backend/src/main/java/com/cms/entity/FolderPermission.java
- [x] T005 [P] Create FolderFavorite entity with user and folder relationships in backend/src/main/java/com/cms/entity/FolderFavorite.java
- [x] T006 [P] Create FolderRecent entity with user, folder, and accessed_at timestamp in backend/src/main/java/com/cms/entity/FolderRecent.java
- [x] T007 [P] Create FolderRepository with findByWorkspaceId, findByParentId, findByUuid, ancestor CTE query, and descendant check in backend/src/main/java/com/cms/repository/FolderRepository.java
- [x] T008 [P] Create FolderPermissionRepository with findByFolderIdAndUserId, findByFolderIdAndGroupId, findByFolderId in backend/src/main/java/com/cms/repository/FolderPermissionRepository.java
- [x] T009 [P] Create FolderFavoriteRepository with findByUserIdAndFolderId, findByUserIdAndFolder_WorkspaceId in backend/src/main/java/com/cms/repository/FolderFavoriteRepository.java
- [x] T010 [P] Create FolderRecentRepository with findByUserIdAndFolderId, findTopByUserIdAndFolder_WorkspaceId in backend/src/main/java/com/cms/repository/FolderRecentRepository.java
- [x] T011 [P] Create CreateFolderRequest DTO with name, parentId, sortOrder validation in backend/src/main/java/com/cms/dto/folder/CreateFolderRequest.java
- [x] T012 [P] Create UpdateFolderRequest DTO with optional name and sortOrder in backend/src/main/java/com/cms/dto/folder/UpdateFolderRequest.java
- [x] T013 [P] Create MoveFolderRequest DTO with targetParentId and sortOrder in backend/src/main/java/com/cms/dto/folder/MoveFolderRequest.java
- [x] T014 [P] Create FolderResponse DTO with from() mapper including breadcrumbs array in backend/src/main/java/com/cms/dto/folder/FolderResponse.java
- [x] T015 [P] Create FolderTreeResponse DTO with id, name, parentId, sortOrder, childCount for flat tree list in backend/src/main/java/com/cms/dto/folder/FolderTreeResponse.java
- [x] T016 [P] Create FolderPermissionRequest DTO with userId, groupId, roleId in backend/src/main/java/com/cms/dto/folder/FolderPermissionRequest.java
- [x] T017 [P] Create FolderPermissionResponse DTO with inherited flag and inheritedFrom source in backend/src/main/java/com/cms/dto/folder/FolderPermissionResponse.java
- [x] T018 [P] Create folder TypeScript types (Folder, FolderTreeNode, BreadcrumbItem, FolderPermission, FolderFavorite, FolderRecent) in frontend/src/types/folder.ts
- [x] T019 [P] Create folders API service with CRUD, move, favorites, recents, and permissions endpoints in frontend/src/api/folders.ts

**Checkpoint**: Foundation ready — all entities, repositories, DTOs, and frontend types available for user story implementation

---

## Phase 3: User Story 1 — Folder CRUD & Hierarchy Navigation (Priority: P1) 🎯 MVP

**Goal**: Users can create/rename/delete folders, see a hierarchical tree sidebar, and navigate via breadcrumbs

**Independent Test**: Create workspace → create nested folders → verify tree renders → click breadcrumbs → rename folder → delete folder

### Implementation for User Story 1

- [x] T020 [US1] Implement FolderService with create(), getByUuid(), listByWorkspace(), getChildren(), update(), delete(), getAncestorPath() methods and folder name validation in backend/src/main/java/com/cms/service/FolderService.java
- [x] T021 [US1] Add Redis caching to FolderService for folder tree (cache key folder_tree:{workspaceId}, TTL 10min, invalidation on mutation) in backend/src/main/java/com/cms/service/FolderService.java
- [x] T022 [US1] Implement FolderController with POST create, GET list, GET by id, GET children, PUT update, DELETE endpoints under /api/v1/workspaces/{workspaceId}/folders in backend/src/main/java/com/cms/controller/FolderController.java
- [x] T023 [US1] Create FolderTree React component that builds tree from flat folder list, supports expand/collapse, and highlights selected folder in frontend/src/components/FolderTree.tsx
- [x] T024 [US1] Create FolderTreeNode recursive component with expand/collapse toggle, click-to-navigate, and folder icon in frontend/src/components/FolderTreeNode.tsx
- [x] T025 [US1] Create Breadcrumbs component showing clickable path segments from workspace root to current folder with truncation for deep paths in frontend/src/components/Breadcrumbs.tsx
- [x] T026 [US1] Create FolderContextMenu component with New Folder, Rename, Delete actions in frontend/src/components/FolderContextMenu.tsx
- [x] T027 [US1] Create WorkspacePage that integrates FolderSidebar (tree) with main content area showing folder children and Breadcrumbs in frontend/src/pages/WorkspacePage.tsx
- [x] T028 [US1] Add /workspaces/:workspaceId route to App.tsx pointing to WorkspacePage in frontend/src/App.tsx

**Checkpoint**: User Story 1 complete — folders can be created, renamed, deleted, navigated via tree and breadcrumbs

---

## Phase 4: User Story 2 — Drag-Drop Folder Reorganization (Priority: P2)

**Goal**: Users can drag folders to reorganize the hierarchy with circular move prevention

**Independent Test**: Drag folder to new parent → tree updates → attempt circular move → rejected with error

### Implementation for User Story 2

- [x] T029 [US2] Add move() method to FolderService with circular move detection (ancestor walk), sibling name conflict resolution, parent_id update, and cache invalidation in backend/src/main/java/com/cms/service/FolderService.java
- [x] T030 [US2] Add PUT /move endpoint to FolderController that accepts MoveFolderRequest and returns updated folder in backend/src/main/java/com/cms/controller/FolderController.java
- [x] T031 [US2] Add HTML5 drag-and-drop handlers (onDragStart, onDragOver, onDrop) to FolderTreeNode with drag source tracking and drop target highlighting in frontend/src/components/FolderTreeNode.tsx
- [x] T032 [US2] Add client-side circular move validation to FolderTree using local tree data for instant feedback before API call in frontend/src/components/FolderTree.tsx
- [x] T033 [US2] Add touch event handlers (touchstart, touchmove, touchend) to FolderTreeNode for mobile drag-drop support in frontend/src/components/FolderTreeNode.tsx

**Checkpoint**: User Story 2 complete — drag-drop reorganization works with circular move prevention

---

## Phase 5: User Story 4 — Folder Permission Inheritance (Priority: P2)

**Goal**: Folder-level RBAC with inheritance, explicit overrides, and permission-filtered tree views

**Independent Test**: Assign role on folder → verify inherited on subfolder → override on subfolder → remove parent role → verify access scoping

### Implementation for User Story 4

- [x] T034 [US4] Implement FolderPermissionService with assignPermission(), removePermission(), getEffectivePermission() (walk-up resolution with Redis cache), listPermissions() (showing inherited vs explicit) in backend/src/main/java/com/cms/service/FolderPermissionService.java
- [x] T035 [US4] Add Redis caching for permission resolution with key folder_perm:{userId}:{folderId}, TTL 5min, and invalidation on permission change in backend/src/main/java/com/cms/service/FolderPermissionService.java
- [x] T036 [US4] Add permission filtering to FolderService.listByWorkspace() to return only folders where user has at least view access in backend/src/main/java/com/cms/service/FolderService.java
- [x] T037 [US4] Add permission endpoints to FolderController: GET /permissions, POST /permissions, DELETE /permissions/{id} under /api/v1/workspaces/{workspaceId}/folders/{folderId} in backend/src/main/java/com/cms/controller/FolderController.java
- [x] T038 [US4] Add @PreAuthorize checks to FolderController methods using folder-level permission evaluation in backend/src/main/java/com/cms/controller/FolderController.java

**Checkpoint**: User Story 4 complete — permission inheritance works, tree filtered by access, inherited vs explicit clearly distinguished

---

## Phase 6: User Story 3 — Favorites & Recent Items (Priority: P3)

**Goal**: Users can favorite folders and see recently visited folders in the sidebar

**Independent Test**: Navigate to folder → appears in recents → star folder → appears in favorites → unstar → removed from favorites

### Implementation for User Story 3

- [x] T039 [US3] Add favorite and recent methods to FolderService: addFavorite(), removeFavorite(), listFavorites(), recordVisit(), listRecents() with 10-item cap enforcement in backend/src/main/java/com/cms/service/FolderService.java
- [x] T040 [US3] Add favorite and recent endpoints to FolderController: POST/DELETE /favorite, GET /favorites, POST /visit, GET /recents in backend/src/main/java/com/cms/controller/FolderController.java
- [x] T041 [US3] Create FolderSidebar component with three sections: Favorites (starred folders), Recent (last 10 visited), and Folder Tree in frontend/src/components/FolderSidebar.tsx
- [x] T042 [US3] Add star icon toggle button to FolderTreeNode and FolderSidebar for favorite/unfavorite action in frontend/src/components/FolderTreeNode.tsx
- [x] T043 [US3] Add automatic visit recording to WorkspacePage when user navigates to a folder (calls POST /visit) in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: User Story 3 complete — favorites and recents sections populated and interactive in sidebar

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Audit logging, security hardening, and validation

- [x] T044 [P] Add audit logging for all folder operations (create, rename, move, delete, permission change) using existing AuditService in backend/src/main/java/com/cms/service/FolderService.java
- [x] T045 [P] Add audit logging for permission operations in FolderPermissionService using existing AuditService in backend/src/main/java/com/cms/service/FolderPermissionService.java
- [x] T046 [P] Add folder name validation utility (non-empty, max 255 chars, no path separators, case-insensitive uniqueness check) in backend/src/main/java/com/cms/service/FolderService.java
- [x] T047 [P] Add loading states, error toasts, and empty-state placeholders to FolderTree, FolderSidebar, and WorkspacePage in frontend/src/components/FolderTree.tsx
- [x] T048 [P] Add lazy-loading support to FolderTree for folders with 100+ children (load on expand via /children endpoint) in frontend/src/components/FolderTree.tsx
- [x] T049 Run quickstart.md validation — verify backend compiles, frontend builds, Docker Compose starts, and folder CRUD works end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migrations must exist before entities)
- **US1 (Phase 3)**: Depends on Phase 2 — BLOCKS all other user stories
- **US2 (Phase 4)**: Depends on Phase 3 (needs FolderService and FolderTree to extend)
- **US4 (Phase 5)**: Depends on Phase 2 — can run in parallel with Phase 3/4 for backend, but permission filtering integrates with FolderService
- **US3 (Phase 6)**: Depends on Phase 2 — can run in parallel with Phase 4/5 for backend
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Core — must complete first (provides FolderService, FolderController, FolderTree base)
- **US2 (P2)**: Extends US1 — adds move logic to FolderService and drag-drop to FolderTree
- **US4 (P2)**: Extends foundational — adds FolderPermissionService; integrates permission filtering into FolderService.listByWorkspace()
- **US3 (P3)**: Independent backend — adds favorite/recent methods; extends FolderSidebar and FolderTreeNode UI

### Within Each User Story

- Backend service before controller
- Controller before frontend components
- Core component before integration into page

### Parallel Opportunities

- All foundational tasks (T003–T019) are parallelizable — different files with no dependencies
- US4 backend (T034–T036) can start in parallel with US2 frontend (T031–T033) after US1 completes
- US3 backend (T039–T040) can start in parallel with US4 integration (T037–T038)
- All polish tasks (T044–T048) are parallelizable

---

## Parallel Example: Phase 2 (Foundational)

```
# All entity files in parallel:
T003: Folder.java
T004: FolderPermission.java
T005: FolderFavorite.java
T006: FolderRecent.java

# All repository files in parallel:
T007: FolderRepository.java
T008: FolderPermissionRepository.java
T009: FolderFavoriteRepository.java
T010: FolderRecentRepository.java

# All DTO files in parallel:
T011-T017: All DTO classes

# Frontend types + API in parallel:
T018: folder.ts types
T019: folders.ts API service
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (2 migration files)
2. Complete Phase 2: Foundational (17 files — entities, repos, DTOs, frontend types)
3. Complete Phase 3: User Story 1 (9 tasks — service, controller, tree, breadcrumbs, page)
4. **STOP and VALIDATE**: Create folders, navigate tree, verify breadcrumbs
5. Deploy/demo — folders are usable

### Incremental Delivery

1. Setup + Foundational → Schema and types ready
2. Add US1 (CRUD + tree + breadcrumbs) → Test independently → **MVP!**
3. Add US2 (drag-drop) → Test move operations → Deploy
4. Add US4 (permissions) → Test inheritance → Deploy
5. Add US3 (favorites + recents) → Test sidebar → Deploy
6. Polish → Audit, validation, lazy-loading, error states → Final release
