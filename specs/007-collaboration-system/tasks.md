# Tasks: Collaboration System

**Input**: Design documents from `/specs/007-collaboration-system/`  
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/comments-api.md, contracts/tasks-api.md, contracts/notifications-api.md, contracts/activity-api.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration, new entities, repositories, and DTOs shared across all user stories

- [X] T001 Create Flyway migration V007__collaboration_system.sql (alter comments, create mentions, tasks, notifications tables) in backend/src/main/resources/db/migration/V007__collaboration_system.sql
- [X] T002 [P] Extend Comment entity with nullable folder_id field and Folder relationship in backend/src/main/java/com/cms/entity/Comment.java
- [X] T003 [P] Create Mention entity in backend/src/main/java/com/cms/entity/Mention.java
- [X] T004 [P] Create Task entity with status enum (OPEN/DONE) in backend/src/main/java/com/cms/entity/Task.java
- [X] T005 [P] Create Notification entity with type enum (MENTION/TASK_ASSIGNED/TASK_COMPLETED) in backend/src/main/java/com/cms/entity/Notification.java
- [X] T006 [P] Create MentionRepository in backend/src/main/java/com/cms/repository/MentionRepository.java
- [X] T007 [P] Create TaskRepository with queries for file, assignee, and status filters in backend/src/main/java/com/cms/repository/TaskRepository.java
- [X] T008 [P] Create NotificationRepository with queries for recipient and read status in backend/src/main/java/com/cms/repository/NotificationRepository.java
- [X] T009 [P] Create TaskDto with static from() factory method in backend/src/main/java/com/cms/dto/collaboration/TaskDto.java
- [X] T010 [P] Create NotificationDto with static from() factory method in backend/src/main/java/com/cms/dto/collaboration/NotificationDto.java
- [X] T011 [P] Create ActivityEventDto in backend/src/main/java/com/cms/dto/collaboration/ActivityEventDto.java
- [X] T012 [P] Create collaboration TypeScript types in frontend/src/types/collaboration.ts
- [X] T013 [P] Extend CommentRepository with folder queries (findByFolderIdAndParentIsNull) in backend/src/main/java/com/cms/repository/CommentRepository.java

**Checkpoint**: Database schema deployed, all entities mapped, repositories ready. Foundation complete for all user stories.

---

## Phase 2: Foundational Services (Blocking Prerequisites)

**Purpose**: Core services that multiple user stories depend on

**⚠️ CRITICAL**: Must complete before user story phases

- [X] T014 Implement MentionService (parse @[userId] from content, create Mention records, trigger notifications) in backend/src/main/java/com/cms/service/MentionService.java
- [X] T015 Implement NotificationService (create, list paginated, mark read, mark all read, get unread count with Redis cache) in backend/src/main/java/com/cms/service/NotificationService.java
- [X] T016 Create NotificationController with GET /notifications, GET /notifications/count, PATCH /{id}/read, POST /read-all endpoints in backend/src/main/java/com/cms/controller/NotificationController.java
- [X] T017 [P] Create notifications API client in frontend/src/api/notifications.ts
- [X] T018 [P] Create tasks API client in frontend/src/api/tasks.ts
- [X] T019 [P] Create activity API client in frontend/src/api/activity.ts

**Checkpoint**: Notification infrastructure operational. API clients ready for UI integration.

---

## Phase 3: User Story 1 - File Comments & Discussions (Priority: P1) 🎯 MVP

**Goal**: Users can post threaded comments on files and view discussion history

**Independent Test**: Open a file → post a comment → verify it appears → reply → verify threading → delete → verify removal

### Implementation for User Story 1

- [X] T020 [US1] Extend CommentService to support folder_id parameter and file access validation in backend/src/main/java/com/cms/service/CommentService.java
- [X] T021 [US1] Extend CommentController with GET/POST/DELETE /files/{fileId}/comments and GET /files/{fileId}/comments/count endpoints in backend/src/main/java/com/cms/controller/CommentController.java
- [X] T022 [US1] Extend comments API client with getCommentCount() in frontend/src/api/comments.ts
- [X] T023 [US1] Create CommentItem component displaying author, content, timestamp, reply button, and delete button in frontend/src/components/collaboration/CommentItem.tsx
- [X] T024 [US1] Create CommentPanel component with comment list, pagination (infinite scroll), and new comment input in frontend/src/components/collaboration/CommentPanel.tsx
- [X] T025 [US1] Create CollaborationSidebar as tabbed panel (Comments/Tasks/Activity) in frontend/src/components/collaboration/CollaborationSidebar.tsx
- [X] T026 [US1] Integrate CollaborationSidebar into WorkspacePage, triggered by file selection in frontend/src/pages/WorkspacePage.tsx

**Checkpoint**: File comments with threading and deletion work end-to-end. Comment count badge visible. MVP deliverable.

---

## Phase 4: User Story 2 - @Mentions & Notifications (Priority: P2)

**Goal**: Users can @mention workspace members, mentioned users receive in-app notifications

**Independent Test**: Post comment with @[userId] → mentioned user sees notification count increase → click notification → navigate to file

### Implementation for User Story 2

- [X] T027 [US2] Integrate MentionService into CommentService.createComment() to extract and store mentions after comment creation in backend/src/main/java/com/cms/service/CommentService.java
- [X] T028 [US2] Add workspace member search endpoint GET /workspaces/{id}/members for mention autocomplete in backend/src/main/java/com/cms/controller/WorkspaceController.java
- [X] T029 [US2] Update CommentDto to include mentions list (userId, name) extracted from Mention records in backend/src/main/java/com/cms/dto/preview/CommentDto.java
- [X] T030 [US2] Create MentionInput component (textarea with @-trigger autocomplete dropdown) in frontend/src/components/collaboration/MentionInput.tsx
- [X] T031 [US2] Integrate MentionInput into CommentPanel replacing plain textarea in frontend/src/components/collaboration/CommentPanel.tsx
- [X] T032 [US2] Create NotificationBell component showing unread count (polls /notifications/count every 30s) in frontend/src/components/collaboration/NotificationBell.tsx
- [X] T033 [US2] NotificationDropdown integrated into NotificationBell component in frontend/src/components/collaboration/NotificationBell.tsx
- [X] T034 [US2] Integrate NotificationBell into DashboardPage header in frontend/src/pages/DashboardPage.tsx

**Checkpoint**: @mentions trigger notifications, bell shows count, clicking navigates to source. Mention text is highlighted in comments.

---

## Phase 5: User Story 3 - File Tasks (Priority: P2)

**Goal**: Users can create tasks on files, assign to team members, track status, view personal task list

**Independent Test**: Create task with assignee and due date → assignee sees in "My Tasks" → mark complete → verify status change

### Implementation for User Story 3

- [X] T035 [US3] Implement TaskService (create, list by file, list by assignee, update status, delete) in backend/src/main/java/com/cms/service/TaskService.java
- [X] T036 [US3] Create TaskController with GET/POST /files/{fileId}/tasks, PATCH/DELETE /tasks/{taskId}, GET /tasks/my in backend/src/main/java/com/cms/controller/TaskController.java
- [X] T037 [US3] Integrate NotificationService into TaskService to notify assignee on task creation and creator on task completion in backend/src/main/java/com/cms/service/TaskService.java
- [X] T038 [US3] TaskItem component integrated into TaskPanel displaying title, assignee, due date, status toggle, overdue indicator in frontend/src/components/collaboration/TaskPanel.tsx
- [X] T039 [US3] TaskForm integrated into TaskPanel (title input, due date picker, description textarea) in frontend/src/components/collaboration/TaskPanel.tsx
- [X] T040 [US3] Create TaskPanel component with task list, create form, and status toggle in frontend/src/components/collaboration/TaskPanel.tsx
- [X] T041 [US3] Integrate TaskPanel as second tab in CollaborationSidebar in frontend/src/components/collaboration/CollaborationSidebar.tsx

**Checkpoint**: Tasks can be created, assigned, completed, and viewed per file. Assignees are notified.

---

## Phase 6: User Story 4 - Activity Timeline (Priority: P3)

**Goal**: Users can view a chronological feed of all file actions (uploads, comments, tasks, shares)

**Independent Test**: Perform actions on a file (upload, comment, task) → open activity tab → verify all events appear in order

### Implementation for User Story 4

- [X] T042 [US4] Activity queries implemented via AuditEventRepository.findByResourceTypeAndResourceId in backend/src/main/java/com/cms/repository/AuditEventRepository.java
- [X] T043 [US4] Create ActivityController with GET /files/{fileId}/activity and GET /folders/{folderId}/activity in backend/src/main/java/com/cms/controller/ActivityController.java
- [X] T044 [US4] CommentService already logs audit events (COMMENT_CREATED, COMMENT_DELETED) in backend/src/main/java/com/cms/service/CommentService.java
- [X] T045 [US4] ActivityItem rendering integrated into ActivityTimeline component in frontend/src/components/collaboration/ActivityTimeline.tsx
- [X] T046 [US4] Create ActivityTimeline component with event list, category color, relative timestamps, and pagination in frontend/src/components/collaboration/ActivityTimeline.tsx
- [X] T047 [US4] Integrate ActivityTimeline as third tab in CollaborationSidebar in frontend/src/components/collaboration/CollaborationSidebar.tsx

**Checkpoint**: Activity timeline shows all file events with filtering. All collaboration actions generate audit trail.

---

## Phase 7: User Story 5 - Folder Discussion Threads (Priority: P3)

**Goal**: Users can start threaded discussions on folders with the same comment/mention features as files

**Independent Test**: Navigate to folder → open discussion panel → post comment with @mention → verify threading and notification

### Implementation for User Story 5

- [X] T048 [US5] Extend CommentController with GET/POST/DELETE /folders/{folderId}/comments and GET /folders/{folderId}/comments/count endpoints in backend/src/main/java/com/cms/controller/CommentController.java
- [X] T049 [US5] Extend CommentService.createComment() to accept folderId (mutually exclusive with fileId) and validate folder access in backend/src/main/java/com/cms/service/CommentService.java
- [X] T050 [US5] Add folder discussion panel trigger to FolderSidebar or FolderContextMenu in frontend/src/components/FolderSidebar.tsx
- [X] T051 [US5] Reuse CommentPanel with folderMode prop passing folderId instead of fileId in frontend/src/components/collaboration/CommentPanel.tsx

**Checkpoint**: Folder-level discussions work with same threading and mention features as file comments.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, loading states, security hardening, performance

- [X] T052 [P] Add loading, empty, and error states to all collaboration panels (CommentPanel, TaskPanel, ActivityTimeline) in frontend/src/components/collaboration/
- [X] T053 [P] Add file/folder access permission check to CommentController and TaskController before all operations in backend controllers
- [X] T054 [P] Add Redis cache invalidation for notification unread count on create/read operations in backend/src/main/java/com/cms/service/NotificationService.java
- [X] T055 [P] Add input validation (content length, title length, description length) to CommentService and TaskService in backend services
- [X] T056 Update SecurityConfig to permit notification and task endpoints for authenticated users in backend/src/main/java/com/cms/config/SecurityConfig.java
- [X] T057 Run quickstart.md validation — verify full flow: comment → mention → notification → task → activity timeline

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (Foundational Services)**: Depends on Phase 1 completion — BLOCKS all user stories
- **User Stories (Phase 3–7)**: All depend on Phase 2 completion
  - US1 (Comments) is MVP — implement first
  - US2 (Mentions) depends on US1 (extends CommentService)
  - US3 (Tasks) can parallel with US2 (independent service)
  - US4 (Activity) depends on US1 + US3 (needs audit events from both)
  - US5 (Folder Discussions) depends on US1 (extends same service)
- **Phase 8 (Polish)**: Depends on all user stories being complete

### Parallel Execution Examples

**Within Phase 3 (US1)**:
- T023 + T024 can start in parallel (separate components)
- T021 blocks T022 (API must exist before client)
- T25 blocks T26 (sidebar must exist before integration)

**Between User Stories**:
- US2 (T027–T034) and US3 (T035–T041) can execute in parallel after US1 is complete
- US4 (T042–T047) depends on audit events from US1 + US3

### Implementation Strategy

1. **MVP**: Phase 1 + 2 + 3 → File comments with threading (core collaboration)
2. **Enhanced**: Phase 4 + 5 → Mentions with notifications + Task management
3. **Complete**: Phase 6 + 7 + 8 → Activity timeline + Folder discussions + Polish
