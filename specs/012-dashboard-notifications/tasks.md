# Tasks: Dashboard & Notifications

**Input**: Design documents from `/specs/012-dashboard-notifications/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/dashboard-api.md

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration and new entity/repository creation shared across all user stories

- [X] T001 Create Flyway migration V19 with activity_events and user_alerts tables, indexes, and notification type enum update in backend/src/main/resources/db/migration/V19__dashboard_activity_alerts.sql
- [X] T002 [P] Create ActivityEvent entity with enum ActionType (FILE_UPLOADED, FILE_DOWNLOADED, FILE_SHARED, FILE_MOVED, FILE_DELETED, FOLDER_CREATED, COMMENT_ADDED, APPROVAL_SUBMITTED, APPROVAL_DECIDED, WORKFLOW_TRANSITIONED) in backend/src/main/java/com/cms/entity/ActivityEvent.java
- [X] T003 [P] Create UserAlert entity with enums AlertType (STORAGE_WARNING, STORAGE_CRITICAL, LINK_EXPIRING, UPLOAD_FAILED, SYSTEM_ANNOUNCEMENT) and Severity (INFO, WARNING, CRITICAL) in backend/src/main/java/com/cms/entity/UserAlert.java
- [X] T004 [P] Create ActivityEventRepository with findByWorkspaceIdIn(List<Long>, Pageable) and findByActorId(Long, Pageable) in backend/src/main/java/com/cms/repository/ActivityEventRepository.java
- [X] T005 [P] Create UserAlertRepository with findByUserIdAndDismissedFalse(Long), findByUuid(String) in backend/src/main/java/com/cms/repository/UserAlertRepository.java
- [X] T006 Add FILE_SHARED and WORKFLOW_TRANSITIONED to Notification.Type enum in backend/src/main/java/com/cms/entity/Notification.java
- [X] T007 [P] Create frontend TypeScript types for dashboard (DashboardSummary, RecentFile, ActivityEvent, SharedItem, Alert) in frontend/src/types/dashboard.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core services that multiple user stories depend on — DashboardService, ActivityEventService, AlertService

**⚠️ CRITICAL**: No user story UI work can begin until this phase is complete

- [X] T008 Create ActivityEventService with recordEvent(actorId, actionType, targetType, targetId, targetName, workspaceId, orgId, metadata) and getActivityFeed(workspaceIds, Pageable) in backend/src/main/java/com/cms/service/ActivityEventService.java
- [X] T009 Create AlertService with generateAlerts(userId, orgId), dismiss(alertUuid, userId), getActiveAlerts(userId) in backend/src/main/java/com/cms/service/AlertService.java
- [X] T010 Create DashboardService with getSummary(userId, orgId), getRecentFiles(userId, workspaceIds, limit), getSharedItems(userId, direction, limit) in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T011 [P] Create DashboardSummaryDto (recentFilesCount, unreadNotifications, pendingApprovals, storageUsedBytes, storageMaxBytes, storagePercentage, activeAlertsCount) in backend/src/main/java/com/cms/dto/dashboard/DashboardSummaryDto.java
- [X] T012 [P] Create RecentFileDto (id, name, mimeType, sizeBytes, workspaceId, workspaceName, folderId, folderPath, lastAccessedAt, updatedAt) in backend/src/main/java/com/cms/dto/dashboard/RecentFileDto.java
- [X] T013 [P] Create ActivityEventDto (id, actorName, actionType, targetType, targetId, targetName, workspaceName, metadata, createdAt) in backend/src/main/java/com/cms/dto/dashboard/ActivityEventDto.java
- [X] T014 [P] Create SharedItemDto (id, fileName, fileId, sharedBy, sharedWith, sharedAt, expiresAt, type) in backend/src/main/java/com/cms/dto/dashboard/SharedItemDto.java
- [X] T015 [P] Create AlertDto (id, alertType, severity, title, message, targetType, targetId, createdAt) in backend/src/main/java/com/cms/dto/dashboard/AlertDto.java

**Checkpoint**: Foundation ready — backend services can serve data to all dashboard endpoints.

---

## Phase 3: User Story 1 - Recent Files & Activity Feed (Priority: P1) 🎯 MVP

**Goal**: Users see their recently accessed files and a chronological activity feed on the dashboard

**Independent Test**: User logs in, dashboard shows up to 10 recent files (clickable) and an activity feed with uploads, shares, approvals across their workspaces

### Implementation for User Story 1

- [X] T016 [US1] Add findRecentByUserWorkspaces query (files ordered by lastAccessedAt DESC, limited) to FileRepository in backend/src/main/java/com/cms/repository/FileRepository.java
- [X] T017 [US1] Implement DashboardService.getRecentFiles() using FileRepository query + workspace membership resolution in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T018 [US1] Implement DashboardService.getActivityFeed() delegating to ActivityEventService with workspace filtering in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T019 [US1] Integrate ActivityEventService.recordEvent() calls into FileService (upload, download, move, delete) in backend/src/main/java/com/cms/service/FileService.java
- [X] T020 [US1] Create DashboardController with GET /dashboard/summary, GET /dashboard/recent-files, GET /dashboard/activity in backend/src/main/java/com/cms/controller/DashboardController.java
- [X] T021 [US1] Add Redis caching (2-min TTL) for dashboard summary in DashboardService in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T022 [US1] Create dashboard API client with getSummary(), getRecentFiles(), getActivity() in frontend/src/api/dashboard.ts
- [X] T023 [US1] Create RecentFilesWidget component displaying file list with name, type icon, workspace, timestamp, and click-to-navigate in frontend/src/components/dashboard/RecentFilesWidget.tsx
- [X] T024 [US1] Create ActivityFeedWidget component with chronological event list, action icons, and "load more" pagination in frontend/src/components/dashboard/ActivityFeedWidget.tsx
- [X] T025 [US1] Integrate RecentFilesWidget and ActivityFeedWidget into DashboardPage replacing current minimal layout in frontend/src/pages/DashboardPage.tsx

**Checkpoint**: Dashboard shows recent files and activity feed. Users can click files to navigate. MVP complete.

---

## Phase 4: User Story 4 - Notifications Management (Priority: P1)

**Goal**: Users see notification badge in header, can open notification panel, view paginated list, mark as read individually/bulk, and click to navigate

**Independent Test**: Trigger an approval request, see unread badge increment, open panel, see notification, mark as read, badge decrements, click to navigate to approval

### Implementation for User Story 4

- [X] T026 [US4] Add optional type filter parameter to NotificationController GET /notifications endpoint in backend/src/main/java/com/cms/controller/NotificationController.java
- [X] T027 [US4] Add findByRecipientIdAndType query method to NotificationRepository in backend/src/main/java/com/cms/repository/NotificationRepository.java
- [X] T028 [US4] Update NotificationService.getNotifications() to accept optional type filter in backend/src/main/java/com/cms/service/NotificationService.java
- [X] T029 [US4] Create NotificationBell component with unread count badge (polls /notifications/count every 30s) in frontend/src/components/NotificationBell.tsx
- [X] T030 [US4] Create NotificationPanel component with paginated notification list, mark-as-read buttons, and click-to-navigate in frontend/src/components/dashboard/NotificationPanel.tsx
- [X] T031 [US4] Integrate NotificationBell into app layout header (visible on all authenticated pages) in frontend/src/App.tsx
- [X] T032 [US4] Create notifications API client (getNotifications, getUnreadCount, markAsRead, markAllRead) in frontend/src/api/notifications.ts

**Checkpoint**: Notification bell shows unread count globally. Panel opens with list. Users can manage read state and navigate.

---

## Phase 5: User Story 2 - Storage Usage Widget (Priority: P2)

**Goal**: Users see storage consumption with visual progress bar, quota limit, percentage, and warning state when >80%

**Independent Test**: User with files sees storage bar at correct percentage, user at >80% sees warning color/text

### Implementation for User Story 2

- [X] T033 [US2] Implement DashboardService.getStorageSummary() using existing StorageQuotaService.getQuotaForOrg() in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T034 [US2] Add storage data (usedBytes, maxBytes, percentage) to DashboardController GET /dashboard/summary response in backend/src/main/java/com/cms/controller/DashboardController.java
- [X] T035 [US2] Create StorageUsageWidget with progress bar, used/max labels, percentage, and warning state (>80% yellow, >95% red) in frontend/src/components/dashboard/StorageUsageWidget.tsx
- [X] T036 [US2] Integrate StorageUsageWidget into DashboardPage in frontend/src/pages/DashboardPage.tsx

**Checkpoint**: Storage usage displayed with visual indicator and warning states.

---

## Phase 6: User Story 3 - Shared Items Widget (Priority: P2)

**Goal**: Users see files shared with them and files they've shared, with tabs to toggle direction

**Independent Test**: User with shared links sees "Shared with me" items with sharer name and date; switches to "Shared by me" tab to see outgoing shares

### Implementation for User Story 3

- [X] T037 [US3] Implement DashboardService.getSharedItems() querying SharedLinkRepository for shared-by-me and file permissions for shared-with-me in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T038 [US3] Add GET /dashboard/shared endpoint (direction, limit params) to DashboardController in backend/src/main/java/com/cms/controller/DashboardController.java
- [X] T039 [US3] Add getShared(direction, limit) to frontend dashboard API client in frontend/src/api/dashboard.ts
- [X] T040 [US3] Create SharedItemsWidget with "With Me" / "By Me" tabs, item list with file name, person, date, and click navigation in frontend/src/components/dashboard/SharedItemsWidget.tsx
- [X] T041 [US3] Integrate SharedItemsWidget into DashboardPage in frontend/src/pages/DashboardPage.tsx

**Checkpoint**: Shared items visible with bidirectional views and navigation.

---

## Phase 7: User Story 5 - Pending Approvals Summary (Priority: P3)

**Goal**: Dashboard shows pending approval counts and recent pending items with quick navigation

**Independent Test**: User with pending approvals sees count badge and top 3 items; clicks to navigate to approvals page

### Implementation for User Story 5

- [X] T042 [US5] Ensure existing PendingApprovalsWidget integrates with enhanced dashboard layout (already exists from feature 011) in frontend/src/pages/DashboardPage.tsx
- [X] T043 [US5] Add pending approvals count (awaiting my review + my submissions pending) to DashboardService.getSummary() in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T044 [US5] Expose pendingApprovals count in GET /dashboard/summary response in backend/src/main/java/com/cms/controller/DashboardController.java

**Checkpoint**: Approval counts shown on dashboard with navigation to full approvals page.

---

## Phase 8: User Story 6 - System Alerts (Priority: P3)

**Goal**: Users see dismissible system alerts (storage warnings, expiring links) on the dashboard

**Independent Test**: User at >80% storage sees storage warning alert; user with link expiring in <24h sees expiry alert; dismissing removes alert

### Implementation for User Story 6

- [X] T045 [US6] Implement AlertService.generateAlerts() — check storage quota (>80% → STORAGE_WARNING, >95% → STORAGE_CRITICAL), check SharedLinkRepository for links expiring within 24h in backend/src/main/java/com/cms/service/AlertService.java
- [X] T046 [US6] Implement AlertService.dismiss() and getActiveAlerts() using UserAlertRepository in backend/src/main/java/com/cms/service/AlertService.java
- [X] T047 [US6] Add GET /dashboard/alerts and POST /dashboard/alerts/{alertId}/dismiss to DashboardController in backend/src/main/java/com/cms/controller/DashboardController.java
- [X] T048 [US6] Add getAlerts() and dismissAlert(id) to frontend dashboard API client in frontend/src/api/dashboard.ts
- [X] T049 [US6] Create AlertsWidget with color-coded alert cards (warning=yellow, critical=red), dismiss buttons in frontend/src/components/dashboard/AlertsWidget.tsx
- [X] T050 [US6] Integrate AlertsWidget at top of DashboardPage (above other widgets) in frontend/src/pages/DashboardPage.tsx

**Checkpoint**: Alerts display for storage and link expiry conditions. Dismissal persists.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Activity event integration across services, cache invalidation, and final layout

- [X] T051 [P] Integrate ActivityEventService.recordEvent() into sharing service (file share events) in backend/src/main/java/com/cms/service/SharedLinkService.java
- [X] T052 [P] Integrate ActivityEventService.recordEvent() into ApprovalService (approval submitted/decided) in backend/src/main/java/com/cms/service/ApprovalService.java
- [X] T053 [P] Integrate ActivityEventService.recordEvent() into WorkflowService (workflow transition) in backend/src/main/java/com/cms/service/WorkflowService.java
- [X] T054 [P] Integrate ActivityEventService.recordEvent() into collaboration service (comment added) in backend/src/main/java/com/cms/service/CommentService.java
- [X] T055 Add Redis cache invalidation for dashboard summary on file upload and share events in backend/src/main/java/com/cms/service/DashboardService.java
- [X] T056 [P] Add loading skeletons and error states to all dashboard widgets in frontend/src/components/dashboard/
- [X] T057 Finalize DashboardPage responsive grid layout with all widgets positioned per design in frontend/src/pages/DashboardPage.tsx
- [X] T058 Run quickstart.md validation to verify end-to-end dashboard functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migration + entities must exist) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — DashboardService and DTOs must exist
- **User Story 4 (Phase 4)**: Depends on Phase 2 — can proceed in parallel with Phase 3
- **User Story 2 (Phase 5)**: Depends on Phase 2 — can proceed in parallel with Phases 3-4
- **User Story 3 (Phase 6)**: Depends on Phase 2 — can proceed in parallel with Phases 3-5
- **User Story 5 (Phase 7)**: Depends on Phase 3 (DashboardController must exist for summary endpoint)
- **User Story 6 (Phase 8)**: Depends on Phase 2 + AlertService
- **Polish (Phase 9)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Recent Files & Activity)**: Foundation only — fully independent, creates DashboardController
- **US4 (Notifications)**: Foundation only — uses existing notification infrastructure
- **US2 (Storage Usage)**: Foundation only — uses existing StorageQuotaService
- **US3 (Shared Items)**: Foundation only — uses existing SharedLinkRepository
- **US5 (Pending Approvals)**: Requires US1 (adds to DashboardController created in US1)
- **US6 (System Alerts)**: Foundation only — creates AlertService + endpoint

### Parallel Opportunities per Phase

- **Phase 1**: T002, T003, T004, T005, T007 can all run in parallel
- **Phase 2**: T011-T015 (DTOs) can all run in parallel; T008-T010 (services) are sequential
- **Phases 3-6**: US1, US4, US2, US3 can proceed in parallel after Phase 2
- **Phase 9**: T051-T054 (activity integrations) and T056 (loading states) can all run in parallel

### Implementation Strategy

**MVP (Phase 1-3)**: Setup + Foundation + Recent Files & Activity = functional dashboard with core value  
**Increment 2 (Phase 4)**: Add notification management  
**Increment 3 (Phases 5-6)**: Add storage and shared items widgets  
**Increment 4 (Phases 7-8)**: Add approvals summary and alerts  
**Final (Phase 9)**: Polish, activity integrations, and layout finalization
