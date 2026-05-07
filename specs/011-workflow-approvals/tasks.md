# Tasks: Document Workflow & Approvals Engine

**Input**: Design documents from `/specs/011-workflow-approvals/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization — database migration, shared enums, base DTOs

- [x] T001 Create Flyway migration V18__workflow_approvals.sql adding workflow_state column to files table, workflow_transitions, approval_requests, approval_decisions, and workflow_triggers tables in backend/src/main/resources/db/migration/V18__workflow_approvals.sql
- [x] T002 [P] Create WorkflowState enum (DRAFT, REVIEW, APPROVED, PUBLISHED, ARCHIVED) in backend/src/main/java/com/cms/entity/WorkflowState.java
- [x] T003 [P] Create workflow TypeScript types (WorkflowState, WorkflowTransition, ApprovalRequest, ApprovalDecision, WorkflowTrigger) in frontend/src/types/workflow.ts

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities and state machine logic that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create WorkflowTransition entity (file_id, from_state, to_state, actor_id, comment, approval_request_id, created_at) in backend/src/main/java/com/cms/entity/WorkflowTransition.java
- [x] T005 [P] Create ApprovalRequest entity (file_id, submitter_id, workspace_id, status, from_state, to_state, created_at, completed_at) in backend/src/main/java/com/cms/entity/ApprovalRequest.java
- [x] T006 [P] Create ApprovalDecision entity (approval_request_id, reviewer_id, decision, comment, decided_at) in backend/src/main/java/com/cms/entity/ApprovalDecision.java
- [x] T007 [P] Create WorkflowTrigger entity (workspace_id, name, trigger_state, trigger_type, config JSON, enabled, created_by) in backend/src/main/java/com/cms/entity/WorkflowTrigger.java
- [x] T008 Create WorkflowTransitionRepository with findByFileIdOrderByCreatedAtDesc in backend/src/main/java/com/cms/repository/WorkflowTransitionRepository.java
- [x] T009 [P] Create ApprovalRequestRepository with findByFileIdAndStatus, findByWorkspaceIdAndStatus in backend/src/main/java/com/cms/repository/ApprovalRequestRepository.java
- [x] T010 [P] Create ApprovalDecisionRepository with findByApprovalRequestId, findByReviewerIdAndDecision in backend/src/main/java/com/cms/repository/ApprovalDecisionRepository.java
- [x] T011 [P] Create WorkflowTriggerRepository with findByWorkspaceIdAndTriggerStateAndEnabled in backend/src/main/java/com/cms/repository/WorkflowTriggerRepository.java
- [x] T012 Add workflow_state field (WorkflowState enum, default DRAFT) to existing FileEntity in backend/src/main/java/com/cms/entity/FileEntity.java
- [x] T013 Create WorkflowStateMachine utility with getAllowedTransitions(state) and isValidTransition(from, to) in backend/src/main/java/com/cms/service/WorkflowStateMachine.java

**Checkpoint**: Foundation ready — entities, repositories, and state machine utility available for all user stories

---

## Phase 3: User Story 1 - Define Document Lifecycle States (Priority: P1) 🎯 MVP

**Goal**: Enable state transitions (Draft→Review→Approved→Published→Archived) with validation and audit logging

**Independent Test**: Upload a file (gets DRAFT state), transition to REVIEW, verify transition recorded in history, verify invalid transitions rejected

### Implementation for User Story 1

- [x] T014 [US1] Create WorkflowTransitionRequest DTO (targetState, comment) in backend/src/main/java/com/cms/dto/workflow/WorkflowTransitionRequest.java
- [x] T015 [P] [US1] Create BulkTransitionRequest DTO (fileIds, targetState, comment) in backend/src/main/java/com/cms/dto/workflow/BulkTransitionRequest.java
- [x] T016 [P] [US1] Create WorkflowTransitionResponse DTO (id, fileId, fromState, toState, actorId, actorName, comment, createdAt) in backend/src/main/java/com/cms/dto/workflow/WorkflowTransitionResponse.java
- [x] T017 [P] [US1] Create WorkflowStateResponse DTO (currentState, allowedTransitions, requiresApproval, hasActiveApproval, activeApprovalId) in backend/src/main/java/com/cms/dto/workflow/WorkflowStateResponse.java
- [x] T018 [US1] Implement WorkflowService with transition(), bulkTransition(), getHistory(), getState() methods in backend/src/main/java/com/cms/service/WorkflowService.java
- [x] T019 [US1] Implement WorkflowController with POST /files/{fileId}/workflow/transition, POST /files/bulk-workflow/transition, GET /files/{fileId}/workflow/history, GET /files/{fileId}/workflow/state in backend/src/main/java/com/cms/controller/WorkflowController.java
- [x] T020 [US1] Create workflow API client with transitionFile(), bulkTransition(), getHistory(), getState() in frontend/src/api/workflow.ts
- [x] T021 [US1] Create WorkflowStateBadge component showing current state with color-coded indicator in frontend/src/components/WorkflowStateBadge.tsx
- [x] T022 [US1] Create WorkflowTransitionMenu component with allowed transitions as action buttons in frontend/src/components/WorkflowTransitionMenu.tsx
- [x] T023 [US1] Integrate WorkflowStateBadge and WorkflowTransitionMenu into FileDetailPanel in frontend/src/components/FileDetailPanel.tsx
- [x] T024 [US1] Add workflowState field to file list display in FileList component in frontend/src/components/FileList.tsx

**Checkpoint**: Documents can transition through lifecycle states. Invalid transitions blocked. History recorded.

---

## Phase 4: User Story 2 - Submit Documents for Approval (Priority: P1)

**Goal**: Authors submit documents for approval with designated reviewers; auto-transition on all-approved, return to DRAFT on rejection

**Independent Test**: Submit a file for approval with 2 reviewers, both approve, document auto-transitions to APPROVED state

### Implementation for User Story 2

- [x] T025 [US2] Create ApprovalSubmitRequest DTO (reviewerIds, comment) in backend/src/main/java/com/cms/dto/workflow/ApprovalSubmitRequest.java
- [x] T026 [P] [US2] Create ApprovalDecisionRequest DTO (decision, comment) in backend/src/main/java/com/cms/dto/workflow/ApprovalDecisionRequest.java
- [x] T027 [P] [US2] Create ApprovalRequestResponse DTO (id, fileId, fileName, submitterName, status, reviewers list, createdAt, completedAt) in backend/src/main/java/com/cms/dto/workflow/ApprovalRequestResponse.java
- [x] T028 [P] [US2] Create ApprovalDecisionResponse DTO (id, approvalRequestId, reviewerName, decision, comment, decidedAt, approvalStatus, counts) in backend/src/main/java/com/cms/dto/workflow/ApprovalDecisionResponse.java
- [x] T029 [US2] Implement ApprovalService with submitForApproval(), decide(), cancel(), getApproval(), listWorkspaceApprovals() in backend/src/main/java/com/cms/service/ApprovalService.java
- [x] T030 [US2] Implement ApprovalController with POST /files/{fileId}/approvals, GET /workspaces/{wsId}/approvals, GET /approvals/{id}, POST /approvals/{id}/decisions, POST /approvals/{id}/cancel in backend/src/main/java/com/cms/controller/ApprovalController.java
- [x] T031 [US2] Create approvals API client with submitForApproval(), decide(), cancel(), getApproval(), listPending() in frontend/src/api/approvals.ts
- [x] T032 [US2] Create ApprovalSubmitDialog component with workspace member selection for reviewers in frontend/src/components/ApprovalSubmitDialog.tsx
- [x] T033 [US2] Integrate approval submission into WorkflowTransitionMenu (show "Submit for Approval" when transition requires approval) in frontend/src/components/WorkflowTransitionMenu.tsx

**Checkpoint**: Approval lifecycle works end-to-end. Submissions create pending requests, decisions auto-complete them.

---

## Phase 5: User Story 3 - Review and Act on Approval Requests (Priority: P2)

**Goal**: Reviewers see pending approvals in dashboard and can approve/reject with comments

**Independent Test**: Reviewer logs in, sees pending approval in list, approves with comment, approval count updates

### Implementation for User Story 3

- [x] T034 [US3] Create ApprovalDecisionPanel component with approve/reject buttons and comment field in frontend/src/components/ApprovalDecisionPanel.tsx
- [x] T035 [P] [US3] Create PendingApprovalsWidget component showing count and recent pending items for dashboard in frontend/src/components/PendingApprovalsWidget.tsx
- [x] T036 [US3] Create PendingApprovalsPage with filterable list of all pending approvals for current user in frontend/src/pages/PendingApprovalsPage.tsx
- [x] T037 [US3] Add route /approvals to React Router for PendingApprovalsPage in frontend/src/App.tsx
- [x] T038 [US3] Integrate PendingApprovalsWidget into DashboardPage in frontend/src/pages/DashboardPage.tsx
- [x] T039 [US3] Add notification creation on approval request submission and decision in ApprovalService in backend/src/main/java/com/cms/service/ApprovalService.java

**Checkpoint**: Reviewers have full UI to discover, view, and act on pending approvals.

---

## Phase 6: User Story 4 - Configure Workflow Triggers (Priority: P2)

**Goal**: Workspace admins configure NOTIFICATION and PREREQUISITE triggers that fire on state entry

**Independent Test**: Admin creates a prerequisite trigger requiring "department" metadata before PUBLISHED, document without metadata is blocked from transitioning

### Implementation for User Story 4

- [x] T040 [US4] Create TriggerCreateRequest DTO (name, triggerState, triggerType, config, enabled) in backend/src/main/java/com/cms/dto/workflow/TriggerCreateRequest.java
- [x] T041 [P] [US4] Create TriggerResponse DTO (id, name, triggerState, triggerType, config, enabled, createdBy, createdAt) in backend/src/main/java/com/cms/dto/workflow/TriggerResponse.java
- [x] T042 [US4] Implement TriggerService with create(), update(), delete(), toggle(), listByWorkspace(), and executeTriggers(fileId, targetState) in backend/src/main/java/com/cms/service/TriggerService.java
- [x] T043 [US4] Integrate TriggerService.executeTriggers() into WorkflowService.transition() to evaluate triggers before state change in backend/src/main/java/com/cms/service/WorkflowService.java
- [x] T044 [US4] Implement TriggerController with POST/GET/PUT/DELETE/PATCH for /workspaces/{wsId}/workflow-triggers in backend/src/main/java/com/cms/controller/TriggerController.java
- [x] T045 [US4] Create triggers API client with create(), list(), update(), delete(), toggle() in frontend/src/api/triggers.ts
- [x] T046 [US4] Create WorkflowTriggersPage for workspace admin to manage triggers (list, create, edit, toggle) in frontend/src/pages/WorkflowTriggersPage.tsx
- [x] T047 [US4] Add route /workspace/:id/workflow-triggers to React Router for trigger management in frontend/src/App.tsx

**Checkpoint**: Triggers fire on state transitions — PREREQUISITE blocks invalid transitions, NOTIFICATION sends alerts.

---

## Phase 7: User Story 5 - View Workflow History and Audit Trail (Priority: P3)

**Goal**: Users view complete transition history for any document with timeline display

**Independent Test**: Open workflow history for a document that went Draft→Review→Approved→Published, see all 3 transitions with timestamps and actors

### Implementation for User Story 5

- [x] T048 [US5] Create WorkflowHistoryPanel component showing chronological timeline of transitions with actors and comments in frontend/src/components/WorkflowHistoryPanel.tsx
- [x] T049 [US5] Integrate WorkflowHistoryPanel into FileDetailPanel (tab or expandable section) in frontend/src/components/FileDetailPanel.tsx
- [x] T050 [US5] Display approval decisions inline within history timeline when transition was from approval in frontend/src/components/WorkflowHistoryPanel.tsx

**Checkpoint**: Full audit trail visible per document with all transitions, approvals, and comments.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Security hardening, error handling, and validation across all stories

- [x] T051 [P] Add validation annotations (@NotNull, @NotBlank, @Size) to all DTOs in backend/src/main/java/com/cms/dto/
- [x] T052 [P] Add error handling for workflow-specific exceptions (InvalidTransitionException, ApprovalAlreadyExistsException) in backend/src/main/java/com/cms/exception/
- [x] T053 Ensure workspace-level authorization checks in all controllers (verify user belongs to workspace) in backend/src/main/java/com/cms/controller/
- [x] T054 [P] Add loading, error, and empty states to all workflow frontend components in frontend/src/components/
- [x] T055 Run quickstart.md validation to verify end-to-end workflow functionality

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (migration must run first) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2 — state machine and entities must exist
- **User Story 2 (Phase 4)**: Depends on Phase 2 + partial Phase 3 (WorkflowService must exist for approval auto-transition)
- **User Story 3 (Phase 5)**: Depends on Phase 4 (approval endpoints must exist for reviewer UI)
- **User Story 4 (Phase 6)**: Depends on Phase 3 (triggers integrate into WorkflowService.transition)
- **User Story 5 (Phase 7)**: Depends on Phase 3 (history endpoint exists) — can proceed in parallel with Phase 4-6
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Lifecycle States)**: Foundation only — fully independent
- **US2 (Submit for Approval)**: Requires US1 WorkflowService for auto-transition call
- **US3 (Review Approvals)**: Requires US2 approval endpoints — reviewer UI needs backend
- **US4 (Configure Triggers)**: Requires US1 WorkflowService for trigger integration
- **US5 (Workflow History)**: Requires US1 history endpoint — can be done in parallel with US2-US4

### Within Each User Story

- DTOs before services
- Services before controllers
- Backend API before frontend API client
- Frontend API client before UI components
- Core logic before integration/polish

### Parallel Opportunities

- Phase 1: T002 and T003 can run in parallel (different languages/repos)
- Phase 2: T005, T006, T007 can run in parallel (different entity files); T009, T010, T011 in parallel (different repos)
- Phase 3: T014-T017 DTOs in parallel, then T018-T019 sequentially, then T020-T024 frontend in parallel
- Phase 4: T026-T028 DTOs in parallel, then T029-T030, then T031-T033
- Phase 5: T034 and T035 in parallel
- Phase 6: T040 and T041 in parallel, then T042-T044 sequentially, then T045-T047 frontend

---

## Parallel Example: Phase 3 (User Story 1)

```bash
# Batch 1: All DTOs in parallel
Task T014: Create WorkflowTransitionRequest DTO
Task T015: Create BulkTransitionRequest DTO
Task T016: Create WorkflowTransitionResponse DTO
Task T017: Create WorkflowStateResponse DTO

# Batch 2: Service (depends on DTOs + entities)
Task T018: Implement WorkflowService

# Batch 3: Controller (depends on service)
Task T019: Implement WorkflowController

# Batch 4: Frontend in parallel
Task T020: Create workflow API client
Task T021: Create WorkflowStateBadge
Task T022: Create WorkflowTransitionMenu

# Batch 5: Integration (depends on components)
Task T023: Integrate into FileDetailPanel
Task T024: Add to FileList
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (migration + enum)
2. Complete Phase 2: Foundational (entities + repos + state machine)
3. Complete Phase 3: User Story 1 (transition logic + UI)
4. **STOP and VALIDATE**: Upload a file, transition through states, verify history
5. Deploy/demo if ready — documents can already flow through lifecycle

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test transitions → Deploy (MVP!)
3. Add User Story 2 → Test approvals → Deploy (core workflow)
4. Add User Story 3 → Test reviewer UI → Deploy (complete approval flow)
5. Add User Story 4 → Test triggers → Deploy (automation)
6. Add User Story 5 → Test history → Deploy (audit trail)
7. Each story adds value without breaking previous stories

### Suggested MVP Scope

**MVP = Phase 1 + Phase 2 + Phase 3 (User Story 1)**
- Documents have workflow states
- Users can transition between states
- Invalid transitions are blocked
- Transition history is recorded
- UI shows state badge and transition actions

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- WorkflowState enum is shared — defined once in Phase 1 Setup
- Existing FileEntity gets workflow_state column added — migration handles default 'DRAFT' for existing files
