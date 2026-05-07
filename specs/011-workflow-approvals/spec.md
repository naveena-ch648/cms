# Feature Specification: Document Workflow & Approvals Engine

**Feature Branch**: `011-workflow-approvals`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build workflow engine. Support document lifecycle (Draft → Review → Approved → Published → Archived). Enable approvals, reviewers, and triggers."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define Document Lifecycle States (Priority: P1)

A workspace admin defines the document lifecycle workflow for their workspace. The system provides the standard state progression (Draft → Review → Approved → Published → Archived). Documents are assigned a workflow state and can transition between states according to allowed transitions.

**Why this priority**: Without defined lifecycle states and transitions, no other workflow functionality (approvals, triggers) can operate. This is the foundation of the entire feature.

**Independent Test**: Admin views the workflow configuration, a user creates a document which starts in "Draft" state, and manually transitions it to "Review" — the state change is recorded and visible.

**Acceptance Scenarios**:

1. **Given** a workspace with workflows enabled, **When** a new file is uploaded, **Then** the file's workflow state is set to "Draft" by default.
2. **Given** a document in "Draft" state, **When** the document owner transitions it to "Review", **Then** the state changes to "Review" and the transition is recorded in the activity log.
3. **Given** a document in "Review" state, **When** a user attempts to transition directly to "Published" (skipping "Approved"), **Then** the system rejects the transition as invalid.
4. **Given** a document in "Archived" state, **When** a user with appropriate permission transitions it back to "Draft", **Then** the document re-enters the lifecycle at "Draft" state.

---

### User Story 2 - Submit Documents for Approval (Priority: P1)

A document author submits a document for approval by transitioning it from "Review" to the approval stage. The system requires one or more designated reviewers to approve before the document can move to the next state. Authors can select reviewers from workspace members.

**Why this priority**: Approvals are the core business value of a workflow engine — ensuring documents are reviewed and authorized before publication.

**Independent Test**: User submits a document for approval, selects two reviewers, both reviewers approve, and the document automatically transitions to "Approved" state.

**Acceptance Scenarios**:

1. **Given** a document in "Review" state, **When** the author requests approval and selects reviewers, **Then** an approval request is created and each reviewer is notified.
2. **Given** a pending approval with two required reviewers, **When** both reviewers approve, **Then** the document automatically transitions to "Approved" state.
3. **Given** a pending approval, **When** any reviewer rejects the document, **Then** the document returns to "Draft" state with the rejection reason recorded.
4. **Given** a pending approval, **When** the submitter cancels the approval request, **Then** the request is cancelled and the document remains in "Review" state.
5. **Given** a pending approval, **When** a reviewer who was not designated attempts to approve, **Then** the system rejects the action.

---

### User Story 3 - Review and Act on Approval Requests (Priority: P2)

A designated reviewer sees pending approval requests in their dashboard and can approve or reject documents with comments. Reviewers can view the document, add comments, and make their decision.

**Why this priority**: Without reviewer-facing UI and actions, the approval workflow cannot complete. This provides the other side of the approval interaction.

**Independent Test**: Reviewer logs in, sees a pending approval in their list, opens the document, adds a comment, and approves it — the approval count updates.

**Acceptance Scenarios**:

1. **Given** a user designated as a reviewer, **When** they view their pending approvals list, **Then** they see all documents awaiting their review with submission date and submitter name.
2. **Given** a reviewer viewing a pending approval, **When** they approve with a comment, **Then** the approval is recorded, the comment is saved, and the approval count increments.
3. **Given** a reviewer viewing a pending approval, **When** they reject with a required reason, **Then** the rejection is recorded and the document author is notified.
4. **Given** a reviewer with multiple pending approvals, **When** they sort by submission date, **Then** requests appear in chronological order.

---

### User Story 4 - Configure Workflow Triggers (Priority: P2)

A workspace admin configures automatic triggers that fire when documents enter specific states. Triggers can send notifications, assign reviewers automatically based on rules, or enforce policies (e.g., require metadata before publishing).

**Why this priority**: Triggers reduce manual work and enforce consistency. They enhance the workflow but are not required for basic lifecycle operation.

**Independent Test**: Admin creates a trigger "On enter Review → notify all members with Reviewer role", a document enters Review, and notifications are sent automatically.

**Acceptance Scenarios**:

1. **Given** an admin configuring triggers, **When** they create a "notify on state change" trigger for the "Review" state, **Then** the trigger is saved and active.
2. **Given** a trigger configured to notify reviewers on "Review" entry, **When** a document transitions to "Review", **Then** all configured recipients receive a notification.
3. **Given** a trigger configured to require metadata before "Published", **When** a document without required metadata attempts to transition to "Published", **Then** the transition is blocked with a clear error message.
4. **Given** an admin, **When** they disable a trigger, **Then** it no longer fires on state transitions but remains saved for re-enabling.

---

### User Story 5 - View Workflow History and Audit Trail (Priority: P3)

Users can view the complete workflow history of a document — all state transitions, who performed them, when, and any associated comments or approval decisions.

**Why this priority**: Audit trail provides compliance value and transparency, but is not required for the workflow to function.

**Independent Test**: User opens workflow history for a document that has been through Draft → Review → Approved → Published, and sees all four transitions with timestamps and actors.

**Acceptance Scenarios**:

1. **Given** a document with workflow history, **When** a user views the workflow timeline, **Then** they see all transitions in chronological order with actor, timestamp, and any comments.
2. **Given** a document that was rejected and resubmitted, **When** viewing history, **Then** both the rejection (with reason) and subsequent resubmission are visible.
3. **Given** a document with approval records, **When** viewing history, **Then** each reviewer's decision (approve/reject), timestamp, and comment are shown.

---

### Edge Cases

- What happens when a designated reviewer is removed from the workspace while an approval is pending? The approval request is reassigned or the submitter is notified to select a new reviewer.
- What happens when all reviewers reject but one approves? The document is rejected since any single rejection returns the document to Draft.
- What happens when a document is deleted while an approval is pending? The approval request is automatically cancelled.
- What happens when a workspace admin changes the workflow configuration while documents are mid-workflow? In-progress documents continue with their current state; new transitions follow updated rules.
- What happens if a trigger action fails (e.g., notification service unavailable)? The state transition still proceeds; the failed trigger is logged and can be retried.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support five document lifecycle states: Draft, Review, Approved, Published, Archived.
- **FR-002**: System MUST enforce valid state transitions (Draft→Review, Review→Approved, Approved→Published, Published→Archived, Archived→Draft).
- **FR-003**: System MUST assign "Draft" state to newly uploaded documents by default.
- **FR-004**: System MUST allow workspace admins to configure which transitions require approvals.
- **FR-005**: System MUST support designating one or more reviewers per approval request from workspace members.
- **FR-006**: System MUST require all designated reviewers to approve before auto-transitioning to the next state.
- **FR-007**: System MUST return document to "Draft" state when any reviewer rejects, with the rejection reason preserved.
- **FR-008**: System MUST notify designated reviewers when they receive an approval request.
- **FR-009**: System MUST notify document authors when their approval is approved or rejected.
- **FR-010**: System MUST record all state transitions with actor, timestamp, previous state, new state, and optional comment.
- **FR-011**: System MUST allow reviewers to add comments with their approval or rejection decision.
- **FR-012**: System MUST support cancellation of pending approval requests by the document submitter.
- **FR-013**: System MUST support workspace-level trigger configuration for state transitions.
- **FR-014**: System MUST support notification triggers that fire when a document enters a specific state.
- **FR-015**: System MUST support prerequisite triggers that block transitions unless conditions are met (e.g., required metadata present).
- **FR-016**: System MUST provide a complete workflow history view per document.
- **FR-017**: System MUST prevent unauthorized state transitions (only document owner or workspace admin can initiate transitions).
- **FR-018**: System MUST support bulk state transitions for multiple documents at once.

### Key Entities

- **WorkflowState**: Represents the current lifecycle position of a document (Draft, Review, Approved, Published, Archived).
- **WorkflowTransition**: A recorded event of a document moving from one state to another, including actor and timestamp.
- **ApprovalRequest**: A request for one or more reviewers to approve a document's transition, with status tracking per reviewer.
- **ApprovalDecision**: An individual reviewer's decision (approve/reject) with comment and timestamp.
- **WorkflowTrigger**: A configured automation that fires on specific state transitions (notification, validation, assignment).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can transition a document through the complete lifecycle (Draft → Published) in under 5 clicks per transition.
- **SC-002**: Reviewers can view and act on pending approvals within 3 clicks from the dashboard.
- **SC-003**: System supports 50+ concurrent pending approval requests per workspace without performance degradation.
- **SC-004**: Complete workflow history for a document loads in under 2 seconds regardless of history length.
- **SC-005**: Notification delivery to reviewers occurs within 30 seconds of an approval request submission.
- **SC-006**: 95% of users can complete a document submission for approval without guidance on first attempt.
- **SC-007**: Trigger-based automations execute within 5 seconds of a state transition event.

## Assumptions

- The existing notification infrastructure (from the collaboration/dashboard features) will be reused for workflow notifications.
- Workflow states are fixed (Draft, Review, Approved, Published, Archived); custom state definition is out of scope for v1.
- A document can only be in one workflow state at a time (no parallel workflow branches).
- The "any rejection returns to Draft" policy is the default; configurable rejection policies are out of scope for v1.
- Approval requests use a simple "all must approve" model; quorum-based or weighted approval is out of scope for v1.
- Triggers are workspace-scoped; organization-level trigger templates are out of scope for v1.
- The Review→Approved transition is the only one that requires approvals by default; admins can configure additional transitions to require approval.
