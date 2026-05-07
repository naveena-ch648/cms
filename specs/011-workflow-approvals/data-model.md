# Data Model: Document Workflow & Approvals Engine

**Feature**: 011-workflow-approvals  
**Date**: 2026-05-06

---

## Entities

### WorkflowState (Enum on FileEntity)

Added as a column to the existing `files` table.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| workflow_state | ENUM('DRAFT','REVIEW','APPROVED','PUBLISHED','ARCHIVED') | NOT NULL, DEFAULT 'DRAFT' | Current lifecycle state |

**Index**: (workspace_id, workflow_state) — for filtered file listing by state.

---

### WorkflowTransition

Records every state change for audit trail purposes.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Document that transitioned |
| from_state | VARCHAR(20) | NOT NULL | State before transition |
| to_state | VARCHAR(20) | NOT NULL | State after transition |
| actor_id | BIGINT | FK → users.id, NOT NULL | Who performed/triggered the transition |
| comment | TEXT | NULLABLE | Optional comment on transition |
| approval_request_id | BIGINT | FK → approval_requests.id, NULLABLE | If transition resulted from approval completion |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When transition occurred |

**Indexes**: 
- (file_id, created_at) — for per-file history ordered by time
- (actor_id) — for "my transitions" queries

---

### ApprovalRequest

A request for reviewers to approve a document's state transition.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files.id, NOT NULL | Document under review |
| submitter_id | BIGINT | FK → users.id, NOT NULL | Who submitted for approval |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Workspace scope |
| status | ENUM('PENDING','APPROVED','REJECTED','CANCELLED') | NOT NULL, DEFAULT 'PENDING' | Overall request status |
| from_state | VARCHAR(20) | NOT NULL | State when submitted (e.g., REVIEW) |
| to_state | VARCHAR(20) | NOT NULL | Target state on approval (e.g., APPROVED) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Submission time |
| completed_at | TIMESTAMP | NULLABLE | When request was resolved |

**Indexes**:
- (file_id, status) — find active approval for a file
- (workspace_id, status) — list pending approvals in workspace
- (submitter_id, status) — user's submitted approvals

---

### ApprovalDecision

An individual reviewer's vote on an approval request.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| approval_request_id | BIGINT | FK → approval_requests.id, NOT NULL | Parent request |
| reviewer_id | BIGINT | FK → users.id, NOT NULL | Designated reviewer |
| decision | ENUM('PENDING','APPROVED','REJECTED') | NOT NULL, DEFAULT 'PENDING' | Reviewer's decision |
| comment | TEXT | NULLABLE | Reviewer's comment |
| decided_at | TIMESTAMP | NULLABLE | When decision was made |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When reviewer was assigned |

**Indexes**:
- (approval_request_id, decision) — count approvals/rejections
- (reviewer_id, decision) — reviewer's pending items

**Unique constraint**: (approval_request_id, reviewer_id) — one decision per reviewer per request.

---

### WorkflowTrigger

A configured automation that fires on state transitions.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | VARCHAR(36) | UNIQUE, NOT NULL | Public identifier |
| workspace_id | BIGINT | FK → workspaces.id, NOT NULL | Workspace scope |
| name | VARCHAR(100) | NOT NULL | Display name |
| trigger_state | VARCHAR(20) | NOT NULL | State that activates this trigger |
| trigger_type | ENUM('NOTIFICATION','PREREQUISITE') | NOT NULL | Type of automation |
| config | JSON | NOT NULL | Type-specific configuration |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | Active/inactive toggle |
| created_by | BIGINT | FK → users.id, NOT NULL | Admin who created |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update |

**Indexes**:
- (workspace_id, trigger_state, enabled) — find active triggers for a state

**Config JSON examples**:
- NOTIFICATION: `{"recipients": "all_reviewers", "message_template": "Document {fileName} entered Review"}`
- PREREQUISITE: `{"require_metadata_fields": ["department", "classification"]}`

---

## Relationships

```
files (1) ──── (N) workflow_transitions
files (1) ──── (N) approval_requests
approval_requests (1) ──── (N) approval_decisions
workspaces (1) ──── (N) workflow_triggers
workspaces (1) ──── (N) approval_requests
users (1) ──── (N) workflow_transitions [as actor]
users (1) ──── (N) approval_requests [as submitter]
users (1) ──── (N) approval_decisions [as reviewer]
```

---

## State Machine: Allowed Transitions

```
DRAFT → REVIEW
REVIEW → APPROVED (requires approval gate)
APPROVED → PUBLISHED
PUBLISHED → ARCHIVED
ARCHIVED → DRAFT (re-enter lifecycle)
```

Additional admin transitions (workspace admin only):
- Any state → DRAFT (administrative reset)
- REVIEW → DRAFT (reject without approval flow)

---

## Validation Rules

- **WorkflowTransition**: from_state/to_state must be a valid pair from the state machine
- **ApprovalRequest**: Can only be created when file is in REVIEW state; only one active (PENDING) request per file at a time
- **ApprovalDecision**: reviewer_id must be a workspace member; decision can only change from PENDING
- **WorkflowTrigger**: trigger_state must be a valid WorkflowState; config must validate against trigger_type schema
- **Bulk transitions**: All files must be in the same source state and target must be a valid transition from that state
