# Feature Specification: Collaboration System

**Feature Branch**: `007-collaboration-system`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build collaboration features: comments, mentions, tasks, activity timeline, and file discussions."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - File Comments & Discussions (Priority: P1) 🎯 MVP

Users can open a file and participate in threaded discussions. They can post comments, reply to existing comments, and view the full conversation history associated with a file. This provides the core communication layer for team collaboration around documents.

**Why this priority**: Comments are the fundamental building block of collaboration. Without a way to discuss files, none of the other features (mentions, tasks) have context. This is the minimum viable collaboration experience.

**Independent Test**: Open any file → post a comment → another user sees the comment → reply to it → verify threading works and timestamps are correct.

**Acceptance Scenarios**:

1. **Given** a user has access to a file, **When** they open the file detail view, **Then** they see all existing comments sorted chronologically with author names, timestamps, and reply threads.
2. **Given** a user is viewing a file, **When** they post a new comment, **Then** the comment appears immediately in the discussion thread with their name and current timestamp.
3. **Given** a comment exists on a file, **When** a user replies to it, **Then** the reply appears nested under the parent comment maintaining threading hierarchy (max 2 levels).
4. **Given** a user authored a comment, **When** they delete it, **Then** the comment is removed (replies are also removed if it's a parent comment).
5. **Given** a file has comments, **When** a user with file access opens the file, **Then** they see the comment count badge before opening the discussion panel.

---

### User Story 2 - @Mentions & Notifications (Priority: P2)

Users can @mention other team members in comments to draw their attention. Mentioned users receive notifications and can navigate directly to the relevant comment/file.

**Why this priority**: Mentions transform passive comments into active collaboration by alerting specific people. This is the bridge between commenting and task assignment.

**Independent Test**: Post a comment mentioning @username → the mentioned user sees a notification → clicking the notification navigates to the file and highlights the comment.

**Acceptance Scenarios**:

1. **Given** a user is typing a comment, **When** they type "@" followed by characters, **Then** an autocomplete dropdown appears showing matching workspace members.
2. **Given** a user posts a comment with @username, **When** the comment is saved, **Then** the mentioned user receives an in-app notification with file name and comment preview.
3. **Given** a user has an unread mention notification, **When** they click it, **Then** they are navigated to the file with the comment panel open and the relevant comment highlighted.
4. **Given** a user is mentioned in a comment, **When** they view the comment, **Then** the mention is visually distinct (highlighted/linked to the user profile).

---

### User Story 3 - File Tasks (Priority: P2)

Users can create tasks linked to files, assign them to team members, set due dates, and track completion status. Tasks provide actionable follow-ups from file discussions.

**Why this priority**: Tasks convert discussions into trackable action items. Combined with mentions, this creates a lightweight project management layer directly attached to content.

**Independent Test**: Open a file → create a task with assignee and due date → assignee sees the task in their task list → mark it complete → verify status updates.

**Acceptance Scenarios**:

1. **Given** a user has editor access to a file, **When** they create a task with title, assignee, and optional due date, **Then** the task appears in the file's task list and the assignee's personal task list.
2. **Given** a task is assigned to a user, **When** the assignee views their dashboard, **Then** they see the task with file context, due date, and status.
3. **Given** a task exists on a file, **When** the assignee marks it as complete, **Then** the task status changes to "done" and a completion timestamp is recorded.
4. **Given** a user is viewing a file's tasks, **When** they filter by status (open/done/overdue), **Then** only matching tasks are displayed.
5. **Given** a task has a due date in the past and is not complete, **When** viewed in any task list, **Then** it is visually marked as overdue.

---

### User Story 4 - Activity Timeline (Priority: P3)

Users can view a chronological activity feed showing all actions performed on a file (uploads, version changes, comments, task updates, shares). This provides audit-like transparency at the file level.

**Why this priority**: The timeline is a read-only view that aggregates existing events. It depends on comments, tasks, and file operations being in place to have meaningful content to display.

**Independent Test**: Upload a file → add a comment → create a task → view the activity timeline → verify all events appear in chronological order with actor and timestamp.

**Acceptance Scenarios**:

1. **Given** a file has activity history, **When** a user opens the activity timeline, **Then** they see all events in reverse-chronological order with actor, action type, and timestamp.
2. **Given** a new action occurs on a file (comment, task, version upload, share), **When** a user refreshes the timeline, **Then** the new event appears at the top.
3. **Given** a file has many activity events, **When** viewing the timeline, **Then** events are paginated (loaded on scroll) to maintain performance.
4. **Given** a user views the timeline, **When** they filter by activity type (comments, tasks, versions, shares), **Then** only matching events are shown.

---

### User Story 5 - Discussion Threads on Folders (Priority: P3)

Users can start discussions at the folder level for team-wide communication that isn't tied to a specific file. Folder discussions support the same threading and mention features as file comments.

**Why this priority**: Folder-level discussions complement file-level comments by providing a space for broader team conversations (e.g., project status, guidelines). Lower priority because file comments cover the primary use case.

**Independent Test**: Navigate to a folder → open discussions panel → post a comment → verify it persists and other team members see it.

**Acceptance Scenarios**:

1. **Given** a user has access to a folder, **When** they open the folder's discussion panel, **Then** they see threaded comments specific to that folder.
2. **Given** a user posts a comment in a folder discussion with an @mention, **When** saved, **Then** the mentioned user receives a notification linking to the folder discussion.
3. **Given** a folder has discussion activity, **When** viewing the folder in the sidebar, **Then** a badge shows the count of unread discussion messages.

---

### Edge Cases

- What happens when a mentioned user no longer has access to the file? → Notification is still delivered but navigating to it shows "Access denied" with option to request access.
- What happens when a file is deleted that has open tasks? → Tasks are marked as "cancelled" automatically and assignees are notified.
- What happens when a user deletes a parent comment that has replies? → All child replies are cascade-deleted along with the parent.
- How does the system handle concurrent edits to task status? → Last-write-wins with optimistic locking; conflict results in a refresh prompt.
- What happens when a user mentions themselves? → The mention is rendered visually but no notification is generated.
- What if a file has hundreds of comments? → Comments are paginated (50 per page) with infinite scroll loading.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support creating, reading, and deleting comments on files.
- **FR-002**: System MUST support threaded replies (max 2 levels: comment → reply).
- **FR-003**: System MUST display comments with author name, avatar placeholder, and relative timestamp.
- **FR-004**: System MUST support @mentioning workspace members in comment text with autocomplete.
- **FR-005**: System MUST generate in-app notifications for mentioned users.
- **FR-006**: System MUST render mentions as distinct visual elements (highlighted, clickable).
- **FR-007**: System MUST support creating tasks with title, description (optional), assignee, and due date (optional).
- **FR-008**: System MUST allow task status transitions: open → done, done → open (reopen).
- **FR-009**: System MUST display tasks in both file context and a personal "My Tasks" view.
- **FR-010**: System MUST track and display all file activity events in a chronological timeline.
- **FR-011**: System MUST paginate comments, tasks, and activity events for performance.
- **FR-012**: System MUST support folder-level discussion threads with the same comment/mention features.
- **FR-013**: System MUST restrict comment/task creation to users with at least viewer access to the file.
- **FR-014**: System MUST restrict task assignment to members of the same workspace.
- **FR-015**: System MUST display unread notification count in the UI header.
- **FR-016**: System MUST support marking notifications as read individually or in bulk.
- **FR-017**: System MUST cascade-delete comments and cancel tasks when a file is permanently deleted.

### Key Entities

- **Comment**: Text content attached to a file or folder, with author, timestamp, and optional parent (for threading). Supports @mention markers within content.
- **Mention**: A reference to a user within a comment, linking the comment to the mentioned user and triggering a notification.
- **Task**: An actionable item linked to a file with title, optional description, assignee, optional due date, status (open/done), and creator.
- **Notification**: An in-app alert generated by mentions and task assignments, with read/unread status, target link, and preview text.
- **Activity Event**: A log entry recording an action on a file/folder (who did what, when), displayed in the activity timeline.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can post a comment and see it appear in under 2 seconds.
- **SC-002**: Mentioned users receive notifications within 5 seconds of the comment being posted.
- **SC-003**: Task creation and status updates complete in under 1 second.
- **SC-004**: Activity timeline loads the most recent 50 events in under 2 seconds.
- **SC-005**: 90% of users can successfully create a comment, mention a colleague, and assign a task on first attempt without guidance.
- **SC-006**: System supports at least 500 comments per file without UI degradation.
- **SC-007**: Notification panel displays accurately with up to 200 unread notifications.

## Assumptions

- The existing authentication and RBAC system (feature 001/005) handles access control; this feature respects existing file/folder permissions.
- Comments created in feature 006 (file preview) will be migrated/unified with this collaboration system's comment model.
- In-app notifications are sufficient for v1; email/push notifications are out of scope.
- Real-time updates (WebSocket) are out of scope for v1; polling or manual refresh is acceptable.
- The existing audit_events infrastructure can be extended for the activity timeline rather than building a separate system.
- Task management is lightweight (no Gantt charts, dependencies, or sub-tasks); this is not a full project management tool.
- Mentions autocomplete searches within the current workspace membership only.
