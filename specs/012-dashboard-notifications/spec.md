# Feature Specification: Dashboard & Notifications

**Feature Branch**: `012-dashboard-notifications`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build dashboard showing recent files, shared items, approvals, storage usage, activity logs, and alerts."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Recent Files and Activity Overview (Priority: P1)

A user logs in and sees their personalized dashboard with recently accessed/modified files, recent activity across their workspaces, and key statistics at a glance. This gives users immediate context about what they were working on and what changed.

**Why this priority**: The dashboard is the user's landing page and first interaction point. Recent files and activity provide the most immediate value by reducing navigation time and surfacing relevant content.

**Independent Test**: User logs in, sees their 10 most recently accessed files, can click any file to navigate to it, and sees a chronological activity feed showing recent actions across workspaces.

**Acceptance Scenarios**:

1. **Given** a user with recent file activity, **When** they view the dashboard, **Then** they see up to 10 recently accessed/modified files with name, type, workspace, and timestamp
2. **Given** a user with multiple workspaces, **When** they view the dashboard, **Then** they see an activity feed showing the most recent actions (uploads, edits, shares, approvals) across all their workspaces
3. **Given** a user clicks a recent file, **When** the navigation occurs, **Then** they are taken directly to that file in its workspace context
4. **Given** a user with no recent activity, **When** they view the dashboard, **Then** they see a helpful empty state with suggestions to get started

---

### User Story 2 - View Storage Usage and Workspace Statistics (Priority: P2)

A user views their storage consumption across workspaces, including total used space, quota limits, file counts, and a breakdown by workspace or file type. This helps users manage their resources proactively.

**Why this priority**: Storage awareness prevents quota surprises and helps users self-manage their resource consumption without admin intervention.

**Independent Test**: User opens dashboard, sees a storage usage indicator showing used vs. total quota, and can view a breakdown of storage per workspace.

**Acceptance Scenarios**:

1. **Given** a user with files across workspaces, **When** they view the dashboard, **Then** they see total storage used, quota limit, and percentage consumed displayed visually
2. **Given** a user nearing their storage quota (>80%), **When** they view the dashboard, **Then** the storage indicator shows a warning state
3. **Given** a user with multiple workspaces, **When** they expand the storage details, **Then** they see per-workspace storage breakdown with file counts

---

### User Story 3 - View Shared Items and Collaboration Summary (Priority: P2)

A user views files shared with them and files they have shared with others, along with active collaboration indicators. This surfaces content requiring attention without requiring manual navigation through workspaces.

**Why this priority**: Shared content represents active collaboration that often requires timely attention. Surfacing it on the dashboard reduces missed handoffs.

**Independent Test**: User opens dashboard, sees a "Shared with me" section showing files others have shared, and a "Shared by me" section showing files they've shared out.

**Acceptance Scenarios**:

1. **Given** a user with files shared to them, **When** they view the dashboard, **Then** they see up to 5 recently shared items with sharer name, file name, and share date
2. **Given** a user who has shared files, **When** they view the "Shared by me" section, **Then** they see their active shares with recipient info and expiry dates
3. **Given** a shared item, **When** the user clicks on it, **Then** they navigate to the file or receive access to it directly

---

### User Story 4 - Receive and Manage Notifications (Priority: P1)

A user receives real-time notifications about events relevant to them: approval requests, file shares, mentions, task assignments, and workflow state changes. They can view, mark as read, and manage their notification history.

**Why this priority**: Notifications are the primary mechanism for keeping users informed about time-sensitive actions (approvals, mentions). Without them, users must poll multiple pages.

**Independent Test**: User receives a notification when someone requests their approval, sees an unread badge on the notification icon, clicks to view the notification list, and marks items as read.

**Acceptance Scenarios**:

1. **Given** an approval request is submitted to a user, **When** the user views their notifications, **Then** they see a notification with type, message, actor, and timestamp
2. **Given** a user has unread notifications, **When** they view any page, **Then** a notification badge displays the unread count in the navigation header
3. **Given** a user views a notification, **When** they click it, **Then** they navigate to the related item (approval, file, comment)
4. **Given** a user has many notifications, **When** they open the notification panel, **Then** notifications are paginated and can be marked as read individually or in bulk

---

### User Story 5 - View Pending Approvals Summary (Priority: P3)

A user sees their pending approval requests on the dashboard — both items awaiting their review and items they've submitted awaiting others' decisions. This consolidates approval visibility.

**Why this priority**: Approval visibility on the dashboard is a convenience enhancement. The core approval functionality already exists via the dedicated approvals page; this story surfaces a summary on the dashboard.

**Independent Test**: User views dashboard, sees count of approvals awaiting their action and count of their submissions pending review, with quick links to the full approvals page.

**Acceptance Scenarios**:

1. **Given** a user with pending approvals to review, **When** they view the dashboard, **Then** they see count and up to 3 most recent pending items with file name and submitter
2. **Given** a user with submitted items pending approval, **When** they view the dashboard, **Then** they see the status of their recent submissions
3. **Given** a user clicks a pending approval, **When** the navigation occurs, **Then** they are taken to the approval decision view

---

### User Story 6 - View System Alerts (Priority: P3)

A user sees system alerts and important announcements on the dashboard — storage quota warnings, failed uploads, expiring shared links, and admin announcements.

**Why this priority**: Alerts are important but less frequent than daily activities. They prevent data loss (storage full) and access issues (expiring links) but are not the primary dashboard interaction.

**Independent Test**: When a user's storage exceeds 80% quota, an alert appears on the dashboard. When a shared link is expiring within 24 hours, an alert is shown.

**Acceptance Scenarios**:

1. **Given** a user whose storage exceeds 80% of quota, **When** they view the dashboard, **Then** they see a warning alert about storage usage
2. **Given** a user with shared links expiring within 24 hours, **When** they view the dashboard, **Then** they see an alert about expiring links
3. **Given** a user dismisses an alert, **When** they return to the dashboard, **Then** the dismissed alert is no longer shown

---

### Edge Cases

- What happens when a user belongs to many workspaces (10+)? Activity and stats aggregate across all with reasonable pagination.
- How does the dashboard handle a user with zero activity? Show helpful onboarding suggestions.
- What happens when the notification count is very large (100+)? Display "99+" and paginate the notification list.
- How does the dashboard handle slow-loading data? Each widget loads independently with skeleton placeholders.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display up to 10 recently accessed/modified files for the current user on the dashboard
- **FR-002**: System MUST display a chronological activity feed showing recent actions (uploads, downloads, shares, approvals, comments) across the user's workspaces
- **FR-003**: System MUST display storage usage with current consumption, quota limit, and visual indicator
- **FR-004**: System MUST display per-workspace storage breakdown on demand
- **FR-005**: System MUST display files shared with the user and files shared by the user
- **FR-006**: System MUST maintain a notification system that creates notifications for: approval requests, approval decisions, file shares, mentions, task assignments, and workflow transitions
- **FR-007**: System MUST display an unread notification count badge visible across all pages
- **FR-008**: System MUST allow users to view notifications in a paginated list, mark as read individually or in bulk, and navigate to related items
- **FR-009**: System MUST display pending approval counts (awaiting user's review and user's submitted) on the dashboard
- **FR-010**: System MUST generate alerts for: storage quota warnings (>80%), expiring shared links (<24h), and failed uploads
- **FR-011**: System MUST allow users to dismiss alerts and persist that dismissal
- **FR-012**: Dashboard widgets MUST load independently so that failure of one does not block others

### Key Entities

- **Activity Event**: Represents a user action (upload, download, share, comment, approval, transition) with actor, action type, target, workspace, and timestamp
- **Notification**: Represents a user-facing alert with recipient, type, title, message, read status, target link, and actor reference
- **Alert**: Represents a system-generated warning with type, severity, message, target user, dismissal status, and expiry
- **Dashboard Widget Data**: Aggregated statistics (recent files, storage usage, approval counts) computed per user on demand

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can identify their most recent work within 3 seconds of loading the dashboard
- **SC-002**: Dashboard fully loads all widgets within 2 seconds under normal conditions
- **SC-003**: Unread notification count updates within 5 seconds of a new event occurring
- **SC-004**: Users can navigate from a dashboard item to the related content in a single click
- **SC-005**: Storage usage information is accurate within 5 minutes of file changes
- **SC-006**: 90% of users check the dashboard as their first action after login

## Assumptions

- The existing notification entity and service (from feature 007/011) will be extended rather than replaced
- The existing PendingApprovalsWidget (from feature 011) will be incorporated into the enhanced dashboard
- Activity events will be derived from existing audit/action data rather than requiring a separate event stream
- Storage quota is configured at the organization level and exposed via existing workspace/organization APIs
- Real-time push notifications (WebSocket) are out of scope for v1; polling-based refresh is acceptable
- The notification badge in the header already exists (from feature 007) and will be enhanced
- Alert rules (quota threshold, link expiry) use fixed thresholds (80% storage, 24h link expiry) without user customization in v1
