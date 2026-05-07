# Tasks: Admin Console

**Input**: Design documents from `/specs/014-admin-console/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-api.md, quickstart.md

**Tests**: Not explicitly requested in spec — test tasks omitted.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration, new DTOs, new service and controller scaffolding

- [x] T001 Create Flyway migration V21 with analytics performance indices in backend/src/main/resources/db/migration/V21__admin_analytics_indices.sql
- [x] T002 [P] Create AdminAnalyticsResponse DTO with summary, roleDistribution, uploadTrend, storageTrend, topActiveUsers in backend/src/main/java/com/cms/dto/admin/AdminAnalyticsResponse.java
- [x] T003 [P] Create StorageQuotaUpdateRequest DTO with maxStorageBytes, maxFileSizeBytes, allowedExtensions, blockedExtensions, trashRetentionDays and validation in backend/src/main/java/com/cms/dto/admin/StorageQuotaUpdateRequest.java
- [x] T004 [P] Create StorageQuotaDetailResponse DTO with all quota fields plus usedPercent and warning in backend/src/main/java/com/cms/dto/admin/StorageQuotaDetailResponse.java
- [x] T005 [P] Create BulkUserActionRequest and BulkUserActionResponse DTOs with userIds, action, roleId, results in backend/src/main/java/com/cms/dto/admin/BulkUserActionRequest.java and backend/src/main/java/com/cms/dto/admin/BulkUserActionResponse.java
- [x] T006 [P] Create admin API client with getAnalytics, getStorageQuota, updateStorageQuota, bulkUserAction methods in frontend/src/api/admin.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend services and controller that all admin UI pages depend on

**⚠️ CRITICAL**: No frontend admin page work can begin until this phase is complete

- [x] T007 Create AdminAnalyticsService with getAnalytics method: query user counts by status, file counts, storage usage, upload trends (daily for N days), storage trends, role distribution, active users (last 30d), top active users; cache in Redis with 5-min TTL key admin:analytics:{orgId}:{days} in backend/src/main/java/com/cms/service/AdminAnalyticsService.java
- [x] T008 Add updateQuota method to StorageQuotaService: accept StorageQuotaUpdateRequest, validate fields (positive values, mutual exclusivity of allowed/blocked extensions, trashRetentionDays 1-365), update entity, return response with warning if maxStorage < usedStorage in backend/src/main/java/com/cms/service/StorageQuotaService.java
- [x] T009 Add self-deactivation check in UserService.deactivate(): if current authenticated user ID equals target user ID, throw BusinessRuleException preventing self-deactivation in backend/src/main/java/com/cms/service/UserService.java
- [x] T010 Create AdminController with endpoints: GET /api/v1/admin/analytics (view-audit-log permission), GET /api/v1/admin/storage-quota (manage-policies), PUT /api/v1/admin/storage-quota (manage-policies), POST /api/v1/admin/users/bulk-action (manage-users) in backend/src/main/java/com/cms/controller/AdminController.java
- [x] T011 Implement bulk user action logic in AdminController or a new AdminUserService: iterate userIds, apply action (CHANGE_ROLE/ACTIVATE/DEACTIVATE), skip self for deactivate, collect per-user results with success/failure reasons, log audit events for each successful operation in backend/src/main/java/com/cms/controller/AdminController.java

**Checkpoint**: Backend APIs ready — all admin endpoints functional and testable via curl/Postman

---

## Phase 3: User Story 6 — Admin Navigation & Layout (Priority: P1) 🎯 MVP

**Goal**: Admin console shell with sub-navigation, access-controlled routing, and admin link in main sidebar

**Independent Test**: Login as admin → see Admin link in sidebar → click → see admin sub-nav. Login as non-admin → no Admin link visible. Direct /admin URL → access denied for non-admin.

### Implementation for User Story 6

- [x] T012 Create AdminLayout component with left sidebar navigation (Users, Roles, Groups, Storage & Policies, Analytics sections), active section highlighting, and content area in frontend/src/components/admin/AdminLayout.tsx
- [x] T013 Create AdminPage that wraps AdminLayout and renders the active admin section based on URL hash or state; default to Users section in frontend/src/pages/AdminPage.tsx
- [x] T014 Add /admin route to App.tsx with admin role check (authUser.organizationRole === 'Admin'), redirect non-admin users to dashboard; add Admin nav link to sidebar visible only for admin users in frontend/src/App.tsx

**Checkpoint**: Admin console accessible at /admin with working sub-navigation; non-admins blocked

---

## Phase 4: User Story 1 — User Management Dashboard (Priority: P1)

**Goal**: Paginated, searchable user list with create, edit, role change, password reset, activate/deactivate, and bulk operations

**Independent Test**: Navigate to Admin → Users → search users, create new user, change role, deactivate, bulk select and change role

### Implementation for User Story 1

- [x] T015 [US1] Create UserManagement component with: paginated table (name, email, role, status, last login, created date), search input filtering by name/email, status filter dropdown, pagination controls in frontend/src/components/admin/UserManagement.tsx
- [x] T016 [US1] Add Create User dialog to UserManagement: form with firstName, lastName, email, password, role selector; calls usersApi.create on submit; refreshes list in frontend/src/components/admin/UserManagement.tsx
- [x] T017 [US1] Add Edit User dialog to UserManagement: pre-filled form for firstName, lastName; calls usersApi.update on submit in frontend/src/components/admin/UserManagement.tsx
- [x] T018 [US1] Add role change, password reset, activate/deactivate actions to UserManagement: inline action buttons per row; role change shows role selector dropdown; password reset shows new password input; deactivate shows confirmation dialog; calls respective usersApi methods in frontend/src/components/admin/UserManagement.tsx
- [x] T019 [US1] Add bulk actions to UserManagement: row checkboxes for multi-select, bulk action toolbar (Change Role, Activate, Deactivate), calls adminApi.bulkUserAction; displays results summary with success/failure counts in frontend/src/components/admin/UserManagement.tsx
- [x] T020 [US1] Wire UserManagement into AdminLayout/AdminPage as the Users section content in frontend/src/pages/AdminPage.tsx

**Checkpoint**: Full user CRUD + bulk operations working; audit events logged for all admin user changes

---

## Phase 5: User Story 2 — Role & Permission Management (Priority: P1)

**Goal**: Role list with user counts, create/edit/delete custom roles, permission selector, permission matrix view

**Independent Test**: Navigate to Admin → Roles → view all roles with user counts, create custom role with selected permissions, view permission matrix

### Implementation for User Story 2

- [x] T021 [P] [US2] Create RoleManagement component with: role list table (name, description, type system/custom, user count, permissions count), system role badge, action buttons (edit/delete for custom only) in frontend/src/components/admin/RoleManagement.tsx
- [x] T022 [US2] Add Create/Edit Role dialog to RoleManagement: name and description inputs, permission checkboxes grouped by category, calls rolesApi.create or rolesApi.update; disable editing for system roles in frontend/src/components/admin/RoleManagement.tsx
- [x] T023 [US2] Add Delete Role action to RoleManagement: confirmation dialog, prevent deletion if role has assigned users (show error), calls rolesApi.delete in frontend/src/components/admin/RoleManagement.tsx
- [x] T024 [US2] Add Permission Matrix view to RoleManagement: toggle between list and matrix view; matrix shows grid of roles (columns) vs permissions (rows) with checkmarks; read-only visualization in frontend/src/components/admin/RoleManagement.tsx
- [x] T025 [US2] Wire RoleManagement into AdminLayout/AdminPage as the Roles section content in frontend/src/pages/AdminPage.tsx

**Checkpoint**: Full role management working; custom role CRUD, permission matrix visible, system roles read-only

---

## Phase 6: User Story 3 — Group Management (Priority: P2)

**Goal**: Group list with member counts, create/edit/delete groups, add/remove members

**Independent Test**: Navigate to Admin → Groups → create group, add members, verify member count, remove member

### Implementation for User Story 3

- [x] T026 [P] [US3] Create GroupManagement component with: group list table (name, description, member count, created date), search input, action buttons (edit, delete, manage members) in frontend/src/components/admin/GroupManagement.tsx
- [x] T027 [US3] Add Create/Edit Group dialog to GroupManagement: name and description inputs, calls groupsApi.create or groupsApi.update in frontend/src/components/admin/GroupManagement.tsx
- [x] T028 [US3] Add Members Management panel to GroupManagement: expandable member list per group, user search/autocomplete for adding members, remove member button, calls groupsApi.addMember and groupsApi.removeMember in frontend/src/components/admin/GroupManagement.tsx
- [x] T029 [US3] Add Delete Group action with confirmation dialog, calls groupsApi.delete in frontend/src/components/admin/GroupManagement.tsx
- [x] T030 [US3] Wire GroupManagement into AdminLayout/AdminPage as the Groups section content in frontend/src/pages/AdminPage.tsx

**Checkpoint**: Full group management working; create/edit/delete groups, add/remove members

---

## Phase 7: User Story 4 — Storage Quota & Policy Administration (Priority: P2)

**Goal**: Storage usage display, quota configuration form, organization policy editor

**Independent Test**: Navigate to Admin → Storage & Policies → see usage bar, update max file size, update blocked extensions, update password policy

### Implementation for User Story 4

- [x] T031 [P] [US4] Create StoragePolicies component with two sections: Storage Quota and Organization Policies; calls adminApi.getStorageQuota and policiesApi on mount in frontend/src/components/admin/StoragePolicies.tsx
- [ ] T032 [US4] Implement Storage Quota section in StoragePolicies: visual usage bar (used/max bytes with percentage), editable fields for maxStorageBytes (with human-readable size input), maxFileSizeBytes, allowedExtensions, blockedExtensions (comma-separated input), trashRetentionDays; Save button calls adminApi.updateStorageQuota; display warning if quota below usage in frontend/src/components/admin/StoragePolicies.tsx
- [x] T033 [US4] Implement Organization Policies section in StoragePolicies: form fields for password_min_length, password_require_uppercase, password_require_lowercase, password_require_number, password_require_special (checkboxes), session_timeout_minutes; Save button calls organizationApi.updatePolicies with current org ID in frontend/src/components/admin/StoragePolicies.tsx
- [x] T034 [US4] Wire StoragePolicies into AdminLayout/AdminPage as the Storage & Policies section content in frontend/src/pages/AdminPage.tsx

**Checkpoint**: Storage quota and policies fully editable; changes persist and enforce on next file operation

---

## Phase 8: User Story 5 — System Analytics Dashboard (Priority: P2)

**Goal**: Org-wide analytics summary cards, upload trend chart, storage growth chart, top active users, date range filter

**Independent Test**: Navigate to Admin → Analytics → verify summary cards match current org state, view trend charts

### Implementation for User Story 5

- [x] T035 [P] [US5] Create AnalyticsDashboard component with: days selector (7/14/30/60/90), calls adminApi.getAnalytics on mount and on days change in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T036 [US5] Implement summary cards in AnalyticsDashboard: Total Users (with active/inactive/locked breakdown), Total Files, Storage Used (percentage bar), Active Users (last 30 days), Total Workspaces in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T037 [US5] Implement Role Distribution section in AnalyticsDashboard: horizontal bar chart showing user count per role in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T038 [US5] Implement Upload Trend chart in AnalyticsDashboard: simple bar chart showing daily upload counts for the selected period using inline SVG or div-based bars in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T039 [US5] Implement Storage Growth trend in AnalyticsDashboard: line visualization showing cumulative storage usage over the selected period in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T040 [US5] Implement Top Active Users section in AnalyticsDashboard: table showing top 10 users by action count with name and count in frontend/src/components/admin/AnalyticsDashboard.tsx
- [x] T041 [US5] Wire AnalyticsDashboard into AdminLayout/AdminPage as the Analytics section content in frontend/src/pages/AdminPage.tsx

**Checkpoint**: Full analytics dashboard with summary cards, charts, and top users; data refreshes with date range change

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Security hardening, UX polish, validation

- [x] T042 Add loading states (spinner/skeleton), error states (retry button), and empty states to all admin components in frontend/src/components/admin/UserManagement.tsx, RoleManagement.tsx, GroupManagement.tsx, StoragePolicies.tsx, AnalyticsDashboard.tsx
- [x] T043 Add success/error toast notifications for all create/update/delete operations across admin components in frontend/src/components/admin/UserManagement.tsx, RoleManagement.tsx, GroupManagement.tsx, StoragePolicies.tsx
- [x] T044 [P] Docker rebuild and end-to-end verification: rebuild frontend and backend containers, run quickstart.md test scenarios in docker/docker-compose.yml

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (DTOs needed by services/controller) — BLOCKS all frontend work
- **US6 Navigation (Phase 3)**: Depends on Phase 2 — BLOCKS all other user story phases (provides the shell)
- **US1 Users (Phase 4)**: Depends on Phase 3 (navigation shell)
- **US2 Roles (Phase 5)**: Depends on Phase 3 — can run in parallel with US1
- **US3 Groups (Phase 6)**: Depends on Phase 3 — can run in parallel with US1, US2
- **US4 Storage (Phase 7)**: Depends on Phase 2 (storage quota API) and Phase 3 (navigation)
- **US5 Analytics (Phase 8)**: Depends on Phase 2 (analytics API) and Phase 3 (navigation)
- **Polish (Phase 9)**: Depends on all user stories being complete

### User Story Dependencies

- **US6 — Admin Navigation (P1)**: Structural foundation — MUST be first after backend
- **US1 — User Management (P1)**: Independent after US6
- **US2 — Role Management (P1)**: Independent after US6 — can parallel with US1
- **US3 — Group Management (P2)**: Independent after US6 — can parallel with US1/US2
- **US4 — Storage & Policies (P2)**: Independent after US6 — can parallel with US1/US2/US3
- **US5 — System Analytics (P2)**: Independent after US6 — can parallel with all others

### Within Each User Story

- Component creation before feature additions
- Core list/display before CRUD dialogs
- CRUD before bulk/advanced operations
- Wire into AdminPage after component is complete

### Parallel Opportunities

- T002, T003, T004, T005, T006 (all Setup DTOs + API client) — parallel
- T007, T008, T009 (services, different files) — parallel after DTOs
- T021, T026, T031, T035 (initial component scaffolding for US2-US5) — parallel after US6
- After Phase 3 (US6), ALL user stories (US1-US5) can proceed in parallel

---

## Parallel Example: After Phase 3 (US6 Navigation)

```
# All of these can run simultaneously after Phase 3:
Developer A: T015-T020 (US1 User Management)
Developer B: T021-T025 (US2 Role Management)
Developer C: T026-T030 (US3 Group Management)
Developer D: T031-T034 (US4 Storage & Policies)
Developer E: T035-T041 (US5 Analytics Dashboard)
```

---

## Implementation Strategy

### MVP First (US6 + US1 Only)

1. Complete Phase 1: Setup (T001-T006)
2. Complete Phase 2: Foundational backend (T007-T011)
3. Complete Phase 3: US6 Admin Navigation (T012-T014)
4. Complete Phase 4: US1 User Management (T015-T020)
5. **STOP and VALIDATE**: Admin console accessible, user CRUD working
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Backend APIs ready
2. US6 Navigation → Admin shell ready
3. US1 Users → MVP! User management working
4. US2 Roles → Role management added
5. US3 Groups → Group management added
6. US4 Storage → Storage/policy config added
7. US5 Analytics → Full analytics dashboard
8. Polish → Loading states, notifications, final verification

---

## Notes

- This feature is **primarily frontend** — most backend APIs already exist
- 3 new backend endpoints: analytics, storage quota (GET/PUT), bulk user action
- 1 Flyway migration (V21) — performance indices only, no new tables
- Existing API clients (usersApi, rolesApi, groupsApi, organizationApi) are reused directly
- New admin.ts API client is only for the 3 new endpoints
- All inline styling using existing CSSProperties patterns (no CSS framework)
- Admin access gated by `organizationRole === 'Admin'` check in frontend
