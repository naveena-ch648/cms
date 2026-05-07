# Tasks: Audit Logging & Compliance

**Input**: Design documents from `/specs/013-audit-compliance/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/audit-api.md

**Tests**: Not explicitly requested in the spec — test tasks omitted.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Exact file paths included in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database migration, new entities, and shared enums/DTOs

- [X] T001 Create Flyway migration V20__audit_compliance.sql in backend/src/main/resources/db/migration/V20__audit_compliance.sql
- [X] T002 [P] Create AuditCategory enum and AuditEventType enum in backend/src/main/java/com/cms/entity/AuditCategory.java and backend/src/main/java/com/cms/entity/AuditEventType.java
- [X] T003 [P] Create @Audited annotation in backend/src/main/java/com/cms/annotation/Audited.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extend existing AuditEvent entity, enhance AuditService with async + OpenSearch indexing, create new entities

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Enhance AuditEvent entity with new fields (category, outcome, user_agent, actor_name, resource_name, workspace_id) in backend/src/main/java/com/cms/entity/AuditEvent.java
- [X] T005 [P] Create ComplianceReport entity in backend/src/main/java/com/cms/entity/ComplianceReport.java
- [X] T006 [P] Create AuditAlertRule entity in backend/src/main/java/com/cms/entity/AuditAlertRule.java
- [X] T007 [P] Create AuditAlertInstance entity in backend/src/main/java/com/cms/entity/AuditAlertInstance.java
- [X] T008 [P] Create ComplianceReportRepository in backend/src/main/java/com/cms/repository/ComplianceReportRepository.java
- [X] T009 [P] Create AuditAlertRuleRepository in backend/src/main/java/com/cms/repository/AuditAlertRuleRepository.java
- [X] T010 [P] Create AuditAlertInstanceRepository in backend/src/main/java/com/cms/repository/AuditAlertInstanceRepository.java
- [X] T011 Enhance AuditEventRepository with custom query methods (findByOrg, findByCategory, etc.) in backend/src/main/java/com/cms/repository/AuditEventRepository.java
- [X] T012 Enhance AuditService with @Async event capture, OpenSearch indexing, and Redis buffering in backend/src/main/java/com/cms/service/AuditService.java
- [X] T013 Create AuditSearchService for OpenSearch audit_events index management and querying in backend/src/main/java/com/cms/service/AuditSearchService.java
- [X] T014 Create AuditAspect AOP interceptor for @Audited annotation in backend/src/main/java/com/cms/aspect/AuditAspect.java
- [X] T015 [P] Create audit TypeScript types in frontend/src/types/audit.ts
- [X] T016 [P] Create audit API client in frontend/src/api/audit.ts

**Checkpoint**: Foundation ready — entities, repositories, enhanced AuditService with OpenSearch, AOP aspect all in place

---

## Phase 3: User Story 1 — Browse & Search Audit Logs (Priority: P1) 🎯 MVP

**Goal**: Admins can view, search, and filter the audit log with full-text search, faceted filtering, and event detail drill-down.

**Independent Test**: Navigate to audit page, see paginated events, search by user, filter by category/date, click event for full details.

### Implementation for User Story 1

- [X] T017 [P] [US1] Create AuditEventDto in backend/src/main/java/com/cms/dto/audit/AuditEventDto.java
- [X] T018 [P] [US1] Create AuditEventDetailDto in backend/src/main/java/com/cms/dto/audit/AuditEventDetailDto.java
- [X] T019 [P] [US1] Create AuditSearchRequest DTO in backend/src/main/java/com/cms/dto/audit/AuditSearchRequest.java
- [X] T020 [P] [US1] Create AuditSearchResponse DTO in backend/src/main/java/com/cms/dto/audit/AuditSearchResponse.java
- [X] T021 [P] [US1] Create AuditStatsDto in backend/src/main/java/com/cms/dto/audit/AuditStatsDto.java
- [X] T022 [US1] Implement search/filter logic in AuditSearchService (OpenSearch BoolQuery with category, eventType, userId, dateRange, outcome, full-text) in backend/src/main/java/com/cms/service/AuditSearchService.java
- [X] T023 [US1] Implement GET /api/audit/events endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T024 [US1] Implement GET /api/audit/events/{id} endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T025 [US1] Implement GET /api/audit/stats endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T026 [P] [US1] Create AuditSearchBar component in frontend/src/components/audit/AuditSearchBar.tsx
- [X] T027 [P] [US1] Create AuditFilters component (category, eventType, user, date range, outcome) in frontend/src/components/audit/AuditFilters.tsx
- [X] T028 [P] [US1] Create AuditLogTable component with pagination in frontend/src/components/audit/AuditLogTable.tsx
- [X] T029 [P] [US1] Create AuditEventDetail component (full detail view with metadata) in frontend/src/components/audit/AuditEventDetail.tsx
- [X] T030 [US1] Create AuditPage composing search, filters, table, and detail panel in frontend/src/pages/AuditPage.tsx
- [X] T031 [US1] Add AuditPage route and navigation link (admin-only) in frontend/src/App.tsx

**Checkpoint**: Admin can browse, search, and filter audit events with full-text search and see event details.

---

## Phase 4: User Story 2 — Automatic Event Capture (Priority: P1)

**Goal**: All significant user and system actions are automatically captured in the audit log without manual instrumentation per feature.

**Independent Test**: Perform any trackable action (login, file upload, permission change) and verify it appears in the audit log within 5 seconds.

### Implementation for User Story 2

- [X] T032 [US2] Add @Audited annotations to AuthController methods (login, logout) in backend/src/main/java/com/cms/controller/AuthController.java
- [X] T033 [US2] Add @Audited annotations to FileController methods (upload, delete, move, download) in backend/src/main/java/com/cms/controller/FileController.java
- [X] T034 [P] [US2] Add @Audited annotations to FolderController methods (create, delete, move) in backend/src/main/java/com/cms/controller/FolderController.java
- [X] T035 [P] [US2] Add @Audited annotations to UserController and RoleController methods (role assign/revoke) in backend/src/main/java/com/cms/controller/UserController.java
- [X] T036 [P] [US2] Add @Audited annotations to SharedLinkController methods (create, revoke) in backend/src/main/java/com/cms/controller/SharedLinkController.java
- [X] T037 [P] [US2] Add @Audited annotations to WorkflowController and ApprovalController methods in backend/src/main/java/com/cms/controller/WorkflowController.java
- [X] T038 [US2] Implement Redis event buffer and retry scheduler for OpenSearch failures in backend/src/main/java/com/cms/service/AuditService.java

**Checkpoint**: All major actions are automatically captured — login, file ops, permissions, sharing, workflows all create audit events.

---

## Phase 5: User Story 3 — Compliance Report Generation (Priority: P2)

**Goal**: Admins can request and download formatted compliance reports (CSV) for specific time periods and event categories.

**Independent Test**: Select date range and report type, request generation, wait for completion, download CSV file.

### Implementation for User Story 3

- [X] T039 [P] [US3] Create ComplianceReportRequest DTO in backend/src/main/java/com/cms/dto/audit/ComplianceReportRequest.java
- [X] T040 [P] [US3] Create ComplianceReportDto in backend/src/main/java/com/cms/dto/audit/ComplianceReportDto.java
- [X] T041 [US3] Create ComplianceReportService with async CSV generation in backend/src/main/java/com/cms/service/ComplianceReportService.java
- [X] T042 [US3] Implement POST /api/audit/reports endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T043 [US3] Implement GET /api/audit/reports endpoint (list reports) in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T044 [US3] Implement GET /api/audit/reports/{uuid}/download endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T045 [P] [US3] Create ComplianceReportDialog component (date range picker, report type selector) in frontend/src/components/audit/ComplianceReportDialog.tsx
- [X] T046 [US3] Add report list and download buttons to AuditPage in frontend/src/pages/AuditPage.tsx

**Checkpoint**: Admins can generate, list, and download compliance reports as CSV files.

---

## Phase 6: User Story 4 — Audit Log Retention & Integrity (Priority: P2)

**Goal**: Audit logs are immutable and retained for 365 days. No user can delete or modify entries.

**Independent Test**: Attempt DELETE/PUT on audit events via API — verify 403/405 response. Verify no repository delete methods exist.

### Implementation for User Story 4

- [X] T047 [US4] Add security configuration to reject DELETE/PUT requests on /api/audit/events/** in backend/src/main/java/com/cms/config/SecurityConfig.java
- [X] T048 [US4] Add scheduled retention cleanup job (delete events older than 365 days from MySQL and OpenSearch) in backend/src/main/java/com/cms/service/AuditService.java
- [X] T049 [US4] Add OpenSearch index lifecycle configuration (365-day retention) in backend/src/main/java/com/cms/service/AuditSearchService.java

**Checkpoint**: Audit entries are immutable via API. Retention is enforced automatically.

---

## Phase 7: User Story 5 — Real-Time Audit Alerts (Priority: P3)

**Goal**: Security admins are notified when suspicious patterns occur (failed logins, bulk deletions) based on configurable threshold rules.

**Independent Test**: Trigger 5+ failed logins in 5 minutes, verify alert notification is created for org admins.

### Implementation for User Story 5

- [X] T050 [P] [US5] Create AuditAlertRuleDto in backend/src/main/java/com/cms/dto/audit/AuditAlertRuleDto.java
- [X] T051 [P] [US5] Create AuditAlertInstanceDto in backend/src/main/java/com/cms/dto/audit/AuditAlertInstanceDto.java
- [X] T052 [US5] Create AuditAlertService with Redis sliding window counters and threshold evaluation in backend/src/main/java/com/cms/service/AuditAlertService.java
- [X] T053 [US5] Integrate AuditAlertService into AuditService event capture (check thresholds on each event) in backend/src/main/java/com/cms/service/AuditService.java
- [X] T054 [US5] Implement CRUD endpoints for alert rules (GET/POST/PUT/DELETE /api/audit/alerts/rules) in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T055 [US5] Implement GET /api/audit/alerts and POST /api/audit/alerts/{uuid}/acknowledge endpoints in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T056 [US5] Implement GET /api/audit/alerts/{uuid}/events endpoint in AuditController in backend/src/main/java/com/cms/controller/AuditController.java
- [X] T057 [US5] Integrate NotificationService to send alert notifications to org admins in backend/src/main/java/com/cms/service/AuditAlertService.java
- [X] T058 [P] [US5] Create AlertRulesPanel component (list rules, create/edit/delete) in frontend/src/components/audit/AlertRulesPanel.tsx
- [X] T059 [US5] Add alerts tab with triggered alerts list and acknowledge action to AuditPage in frontend/src/pages/AuditPage.tsx

**Checkpoint**: Alert rules can be configured, thresholds are evaluated in real-time, and notifications fire to admins.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, hardening, and validation

- [X] T060 [P] Seed default alert rules (failed login spike, bulk deletion) in V20 migration in backend/src/main/resources/db/migration/V20__audit_compliance.sql
- [X] T061 [P] Add audit page link to sidebar/navigation for admin users in frontend/src/components/FolderSidebar.tsx
- [X] T062 Validate all @Audited annotations fire correctly for each controller action (end-to-end check)
- [X] T063 [P] Add loading, error, and empty states to all audit frontend components
- [X] T064 Run quickstart.md validation (ensure all endpoints respond correctly)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migration must run first) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — delivers search/browse MVP
- **User Story 2 (Phase 4)**: Depends on Phase 2 — can run in parallel with US1
- **User Story 3 (Phase 5)**: Depends on Phase 2 — can run in parallel with US1/US2
- **User Story 4 (Phase 6)**: Depends on Phase 2 — can run in parallel with US1/US2/US3
- **User Story 5 (Phase 7)**: Depends on Phase 2 + T012 (enhanced AuditService) — can run in parallel with US1/US3/US4
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Browse & Search)**: Phase 2 only — independent
- **US2 (Automatic Capture)**: Phase 2 only — independent (populates data for US1)
- **US3 (Compliance Reports)**: Phase 2 only — independent (uses OpenSearch data)
- **US4 (Retention & Integrity)**: Phase 2 only — independent
- **US5 (Alerts)**: Phase 2 + AuditService enhancement (T012) — hooks into event capture flow

### Parallel Opportunities

```bash
# After Phase 2 completes, these can all start in parallel:
Phase 3 (US1): Search UI + endpoints
Phase 4 (US2): @Audited annotations on controllers
Phase 5 (US3): Report generation
Phase 6 (US4): Immutability enforcement
Phase 7 (US5): Alert rules + threshold detection
```

---

## Parallel Example: User Story 1

```bash
# Launch all DTOs together:
T017: AuditEventDto
T018: AuditEventDetailDto
T019: AuditSearchRequest
T020: AuditSearchResponse
T021: AuditStatsDto

# Launch all frontend components together:
T026: AuditSearchBar
T027: AuditFilters
T028: AuditLogTable
T029: AuditEventDetail
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

1. Complete Phase 1: Setup (migration)
2. Complete Phase 2: Foundational (entities, services, AOP)
3. Complete Phase 3: US1 — Browse & Search (admin UI)
4. Complete Phase 4: US2 — Automatic Capture (populates the log)
5. **STOP and VALIDATE**: Admin can see all actions in searchable audit log
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 + US2 → Functional audit log with search (MVP!)
3. US3 → Compliance reports (CSV export)
4. US4 → Retention enforcement
5. US5 → Real-time alerts
6. Polish → Final hardening

### Suggested MVP Scope

- Phase 1 + Phase 2 + Phase 3 + Phase 4 = Working searchable audit log with automatic event capture
