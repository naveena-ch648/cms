# Feature Specification: Integrations & Sync

**Feature Branch**: `015-integrations-sync`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build integrations with Google Drive, and webhooks. Support import/export and sync."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Google Drive Import (Priority: P1)

As a workspace member, I want to import files from my Google Drive into the CMS so that I can centralize document management without manually downloading and re-uploading.

**Why this priority**: Importing existing content from Google Drive is the most immediate value — users likely have documents there already and need a one-click path to bring them into the CMS.

**Independent Test**: Can be fully tested by connecting a Google Drive account, browsing folders, selecting files, and verifying they appear in the CMS workspace with correct metadata.

**Acceptance Scenarios**:

1. **Given** a user is authenticated and has linked their Google Drive account, **When** they open the Import dialog and select files/folders from Drive, **Then** the selected items are copied into the chosen CMS workspace/folder with original filename, size, and creation date preserved.
2. **Given** a user selects a folder from Google Drive for import, **When** the import completes, **Then** the folder hierarchy is recreated in the CMS.
3. **Given** a large file is being imported from Drive, **When** the import is in progress, **Then** the user sees a progress indicator and can continue working on other tasks.
4. **Given** an import fails for a specific file (e.g., permission denied on Drive), **When** the batch import completes, **Then** the user sees a summary showing which files succeeded and which failed with reasons.

---

### User Story 2 - Webhook Event System (Priority: P1)

As an administrator, I want to configure webhooks that fire on CMS events so that external systems can react to file uploads, deletions, approvals, and other changes in real time.

**Why this priority**: Webhooks enable integration with any third-party system without custom code on the CMS side, making this a high-leverage feature for extensibility.

**Independent Test**: Can be fully tested by registering a webhook URL, triggering a CMS event (e.g., file upload), and verifying the external endpoint receives the event payload.

**Acceptance Scenarios**:

1. **Given** an admin is on the webhook configuration page, **When** they create a new webhook with a URL and select event types, **Then** the webhook is saved and listed as active.
2. **Given** a webhook is registered for "file.uploaded" events, **When** a user uploads a file, **Then** the webhook URL receives a POST request with the event payload within 30 seconds.
3. **Given** a webhook delivery fails (target returns 5xx or times out), **When** the system retries, **Then** it uses exponential backoff (3 attempts) and logs the failure.
4. **Given** an admin views webhook delivery history, **When** they select a specific webhook, **Then** they see recent deliveries with status codes, timestamps, and response times.

---

### User Story 3 - Export to Google Drive (Priority: P2)

As a workspace member, I want to export files from the CMS back to my Google Drive so that I can share content with external collaborators who use Drive.

**Why this priority**: Export completes the bidirectional flow and enables users to push finalized documents out to collaborators.

**Independent Test**: Can be fully tested by selecting CMS files, choosing a Drive destination folder, and verifying the files appear in Google Drive.

**Acceptance Scenarios**:

1. **Given** a user has linked their Google Drive account and selected files in the CMS, **When** they click Export to Drive and choose a destination folder, **Then** the files are uploaded to that Drive folder.
2. **Given** a file being exported already exists at the destination, **When** the export runs, **Then** the user is prompted to skip, replace, or rename the file.
3. **Given** an export completes, **When** the user views the export history, **Then** they see a record of what was exported, when, and to which Drive location.

---

### User Story 4 - Google Drive Sync (Priority: P2)

As a workspace member, I want to set up ongoing synchronization between a CMS folder and a Google Drive folder so that changes in either location are automatically reflected in the other.

**Why this priority**: Sync eliminates repetitive import/export actions for users who work across both platforms regularly.

**Independent Test**: Can be fully tested by linking a CMS folder to a Drive folder, modifying a file in Drive, and verifying the change appears in the CMS (and vice versa).

**Acceptance Scenarios**:

1. **Given** a user sets up a sync link between a CMS folder and a Drive folder, **When** a new file is added to the Drive folder, **Then** it appears in the CMS folder within the configured sync interval.
2. **Given** a sync link is active and a file is modified in the CMS, **When** the next sync cycle runs, **Then** the updated version is pushed to Drive.
3. **Given** a conflict occurs (file modified in both locations since last sync), **When** the sync detects it, **Then** both versions are kept (CMS version renamed with suffix) and the user is notified.
4. **Given** an admin views sync status, **When** they check the sync dashboard, **Then** they see last sync time, items synced, and any errors.

---

### User Story 5 - Webhook Management (Priority: P3)

As an administrator, I want to manage webhook configurations including editing, disabling, deleting, and testing webhooks so that I can maintain integrations over time.

**Why this priority**: Management capabilities are essential for ongoing operations but secondary to initial webhook creation and delivery.

**Independent Test**: Can be fully tested by editing a webhook URL, disabling it, triggering an event, and verifying no delivery occurs.

**Acceptance Scenarios**:

1. **Given** an admin selects an existing webhook, **When** they edit the URL or event types, **Then** subsequent deliveries use the updated configuration.
2. **Given** an admin disables a webhook, **When** a matching event fires, **Then** no delivery is attempted for that webhook.
3. **Given** an admin clicks "Test" on a webhook, **When** the test fires, **Then** a sample payload is sent to the configured URL and the result is displayed.
4. **Given** an admin deletes a webhook, **When** they confirm, **Then** the webhook is removed and no further deliveries are attempted.

---

### Edge Cases

- What happens when Google Drive API rate limits are hit during bulk import? System queues remaining items and retries after the rate limit window.
- How does the system handle Google Drive token expiration during a long-running sync? Refresh tokens are used; if refresh fails, sync is paused and user is notified to re-authenticate.
- What happens when a webhook target is permanently unreachable? After exhausting retries, the delivery is marked as failed; after 10 consecutive failures, the webhook is auto-disabled and the admin is notified.
- How does sync handle files that exceed CMS storage quota? The file is skipped, an error is logged, and the user is notified.
- What happens when a synced file is deleted in one location? The deletion propagates to the other location on next sync cycle, with a configurable "trash instead of delete" option.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to authenticate with Google Drive via OAuth2 and store refresh tokens securely.
- **FR-002**: System MUST provide a file browser interface for selecting files/folders from Google Drive for import.
- **FR-003**: System MUST import selected Google Drive files into a specified CMS workspace/folder preserving filename, MIME type, and original timestamps.
- **FR-004**: System MUST support exporting CMS files to a user-specified Google Drive folder.
- **FR-005**: System MUST support bidirectional folder sync between CMS and Google Drive with configurable sync intervals (minimum 5 minutes).
- **FR-006**: System MUST detect and handle sync conflicts by preserving both versions and notifying the user.
- **FR-007**: System MUST allow administrators to create webhooks specifying a target URL, event types, and an optional secret for signature verification.
- **FR-008**: System MUST deliver webhook payloads as HTTP POST requests with JSON body within 30 seconds of the triggering event.
- **FR-009**: System MUST retry failed webhook deliveries with exponential backoff (3 attempts: 10s, 60s, 300s delays).
- **FR-010**: System MUST log all webhook deliveries with status, response code, and duration for at least 30 days.
- **FR-011**: System MUST allow admins to view delivery history, test webhooks, and disable/delete them.
- **FR-012**: System MUST support the following webhook event types: file.uploaded, file.deleted, file.moved, file.version_created, folder.created, folder.deleted, workflow.status_changed, user.created, user.deactivated.
- **FR-013**: System MUST sign webhook payloads using HMAC-SHA256 with the configured secret, included in an `X-CMS-Signature` header.
- **FR-014**: System MUST auto-disable webhooks after 10 consecutive delivery failures and notify the admin.
- **FR-015**: System MUST track import/export/sync history with timestamps, item counts, and error details.
- **FR-016**: System MUST enforce organization-level storage quotas during import and sync operations.

### Key Entities

- **Integration Connection**: Represents a user's linked external account (e.g., Google Drive OAuth tokens). Attributes: provider, user, access token (encrypted), refresh token (encrypted), scopes, status, connected at.
- **Webhook**: A registered endpoint for event notifications. Attributes: URL, event types, secret, status (active/disabled), created by, failure count.
- **Webhook Delivery**: A record of each delivery attempt. Attributes: webhook, event type, payload, response status, response time, attempt number, timestamp.
- **Sync Link**: A mapping between a CMS folder and an external folder. Attributes: CMS folder, external provider, external folder ID, direction (bidirectional/import-only/export-only), sync interval, last sync time, status.
- **Sync Job**: A record of each sync execution. Attributes: sync link, started at, completed at, items synced, items failed, errors.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can import 100 files from Google Drive in a single operation within 5 minutes.
- **SC-002**: Webhook events are delivered to target URLs within 30 seconds of the triggering action, 99% of the time.
- **SC-003**: Bidirectional sync detects and propagates changes within the configured sync interval (default 15 minutes).
- **SC-004**: 95% of users successfully complete their first Google Drive import without requiring support assistance.
- **SC-005**: Webhook delivery success rate exceeds 98% for reachable endpoints.
- **SC-006**: Sync conflict resolution preserves all data (zero data loss) in conflict scenarios.

## Assumptions

- Users have existing Google Drive accounts with files they want to integrate.
- Google Drive API (v3) is available and the organization has a Google Cloud project configured with OAuth2 credentials.
- Webhook targets are HTTP/HTTPS endpoints controlled by the organization's IT team or third-party services.
- The CMS already has file storage, versioning, and folder hierarchy (Steps 1-4 are complete).
- Sync intervals shorter than 5 minutes are not supported to avoid API rate limiting.
- Only Google Drive is supported in v1; additional providers (OneDrive, Dropbox) are out of scope.
- Webhook payloads are limited to event metadata (not full file content) to keep payloads small.
- The system uses background workers (existing Python worker infrastructure) for async import/export/sync operations.
