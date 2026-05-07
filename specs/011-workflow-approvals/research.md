# Research: Document Workflow & Approvals Engine

**Feature**: 011-workflow-approvals  
**Date**: 2026-05-06  
**Purpose**: Resolve technical unknowns and document design decisions

---

## R1: State Machine Pattern for Document Lifecycle

**Decision**: Enum-based state machine with transition validation in service layer

**Rationale**: The workflow has a fixed set of 5 states with well-defined transitions. A simple enum + allowed-transitions map in the service layer provides clarity without over-engineering. No external state machine library is needed for a linear lifecycle with one loop-back (Archived → Draft).

**Alternatives considered**:
- Spring State Machine: Too heavy for 5 fixed states; adds complexity and configuration overhead
- Database-driven state definitions: Over-engineered for fixed states; useful if custom states are needed later
- Event sourcing: Provides audit trail but adds infrastructure complexity; simple transition logging suffices

**Implementation approach**:
- `WorkflowState` enum: DRAFT, REVIEW, APPROVED, PUBLISHED, ARCHIVED
- `WorkflowStateMachine` utility class with `getAllowedTransitions(currentState)` returning valid next states
- Transition validation in `WorkflowService.transition()` before persisting

---

## R2: Approval Request Model

**Decision**: Separate `approval_requests` and `approval_decisions` tables with auto-transition on all-approved

**Rationale**: Separating the request (one per submission) from individual decisions (one per reviewer) enables clean tracking of multi-reviewer approvals. When all decisions are APPROVED, the service auto-transitions the document. Any REJECTED decision immediately returns to Draft.

**Alternatives considered**:
- Single approval table with JSON reviewers field: Harder to query individual reviewer status
- Approval as a state (PENDING_APPROVAL): Adds a 6th state, conflicts with the spec's 5-state model; instead, approval is a gate within the Review→Approved transition

**Implementation approach**:
- `approval_requests` table: links file to reviewers, tracks overall status (PENDING, APPROVED, REJECTED, CANCELLED)
- `approval_decisions` table: individual reviewer decisions with comment
- On each decision, check if all reviewers have decided → auto-complete

---

## R3: Workflow Triggers Architecture

**Decision**: Database-stored trigger definitions with synchronous execution in the transition service

**Rationale**: Triggers are workspace-scoped configurations that fire on state entry. For v1, synchronous execution within the transition call is simplest and ensures prerequisite triggers can block transitions. Notification triggers are fire-and-forget (logged on failure, don't block).

**Alternatives considered**:
- Redis queue for async triggers: Adds latency for prerequisite validation; good for future scaling
- Spring Event system: Good for decoupling but makes prerequisite blocking harder
- Webhook-based triggers: Over-engineered for internal triggers; useful for external integrations later

**Implementation approach**:
- `workflow_triggers` table: workspace_id, trigger_state, trigger_type (NOTIFICATION, PREREQUISITE), config JSON
- `WorkflowTriggerService.executeTriggers(fileId, newState)` called during transition
- PREREQUISITE triggers throw exception if validation fails (blocking the transition)
- NOTIFICATION triggers send notifications via existing notification infrastructure

---

## R4: Notification Integration

**Decision**: Reuse existing Redis-based notification system from collaboration feature

**Rationale**: The collaboration system (feature 007) already has a notification count in Redis and notification delivery. Workflow notifications (approval request, approval/rejection outcome) are new notification types using the same infrastructure.

**Alternatives considered**:
- Email-only notifications: Not real-time; doesn't leverage existing in-app system
- WebSocket push: Nice-to-have but not required for v1; Redis notification counter suffices

**Implementation approach**:
- Add APPROVAL_REQUEST, APPROVAL_APPROVED, APPROVAL_REJECTED notification types
- Reuse existing notification creation pattern from CollaborationService

---

## R5: Workflow State Storage on FileEntity

**Decision**: Add `workflow_state` column to existing `files` table

**Rationale**: Every file has exactly one workflow state. Adding a column to the existing files table avoids JOIN overhead for the most common query (list files with their state). The default value is 'DRAFT'.

**Alternatives considered**:
- Separate `file_workflow_state` table: Unnecessary indirection for a 1:1 relationship
- Storing in metadata/tags: Conflates user metadata with system workflow state

**Implementation approach**:
- `ALTER TABLE files ADD COLUMN workflow_state VARCHAR(20) NOT NULL DEFAULT 'DRAFT'`
- Add index on (workspace_id, workflow_state) for filtered queries

---

## R6: Bulk State Transitions

**Decision**: Service method that validates and transitions multiple files atomically

**Rationale**: Spec requires bulk transitions (FR-018). A transactional bulk method ensures either all files transition or none do (if any file fails validation).

**Alternatives considered**:
- Individual transitions with partial success: More complex error reporting; spec doesn't require partial success
- Async bulk via queue: Adds latency; not needed for reasonable batch sizes (≤100 files)

**Implementation approach**:
- `WorkflowService.bulkTransition(fileIds, targetState, userId)` with @Transactional
- Validate all files are in a valid source state before transitioning any
- Return list of transitioned file IDs

---

## R7: Workflow History / Audit Trail

**Decision**: Dedicated `workflow_transitions` table (not reusing generic audit_events)

**Rationale**: Workflow history needs specific fields (from_state, to_state, comment, approval_request_id) and is queried per-file. A dedicated table with proper indexing is more efficient than filtering a generic audit table.

**Alternatives considered**:
- Reuse audit_events table: Would require JSON extraction for workflow-specific fields; slower for per-file queries
- Event sourcing: Overkill; simple append-only transitions table provides the same audit value

**Implementation approach**:
- `workflow_transitions` table: file_id, from_state, to_state, actor_id, comment, approval_request_id (nullable), created_at
- Index on file_id for per-file history queries
- Populated on every state change (including auto-transitions from approvals)
