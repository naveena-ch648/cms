# Feature Specification: RBAC & Sharing System

**Feature Branch**: `005-rbac-sharing`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build RBAC + sharing system. Support file/folder permissions, inheritance override, external sharing via secure links (password, expiry, watermark, download restrictions)."

## User Scenarios & Testing

### User Story 1 - File & Folder Permission Management (Priority: P1)

A workspace admin assigns permissions on files and folders to individual users or groups. Permissions are granted at the folder level and inherited by all files and subfolders within. When a user navigates folders, they only see content they have access to. The system supports three permission levels: Viewer (read-only), Editor (read/write), and Admin (full control including permission management).

**Why this priority**: Without granular permissions, all workspace members have equal access, which is unacceptable for multi-tenant enterprise content management. This is the foundation upon which all other sharing features depend.

**Independent Test**: Admin assigns "Editor" permission to User B on "Project X" folder. User B can upload/edit files in that folder but cannot manage permissions. User C (with no permission) cannot see the folder at all.

**Acceptance Scenarios**:

1. **Given** a workspace admin viewing a folder, **When** they assign "Viewer" role to a user on that folder, **Then** the user can view and download files in that folder but cannot upload, edit, or delete.
2. **Given** a folder with "Editor" permission for a user, **When** that user navigates to a subfolder, **Then** they inherit the same "Editor" permission without explicit assignment.
3. **Given** a user with no explicit or inherited permission on a folder, **When** they attempt to access the folder or its contents, **Then** the system denies access and the folder is hidden from navigation.
4. **Given** a group with "Viewer" permission on a folder, **When** a new user is added to the group, **Then** they immediately gain "Viewer" access to the folder without additional permission assignment.
5. **Given** a folder hierarchy (A → B → C), **When** the admin assigns "Editor" on folder A to a user, **Then** the user has "Editor" access on A, B, and C via inheritance.

---

### User Story 2 - Permission Inheritance Override (Priority: P1)

A workspace admin overrides inherited permissions at any level in the folder hierarchy. For example, a user may have "Editor" on a parent folder but be restricted to "Viewer" on a sensitive subfolder. Overrides apply only at the specified level and do not cascade further unless explicitly set.

**Why this priority**: Inheritance alone is insufficient for real-world use. Teams need to restrict access to sensitive subfolders (e.g., HR documents, legal contracts) while maintaining broad access to parent directories.

**Independent Test**: User has "Editor" on "Department" folder (inherited to all subfolders). Admin sets "Viewer" override on "Confidential" subfolder. User can edit files in "Department" but only view files in "Confidential".

**Acceptance Scenarios**:

1. **Given** a user inherits "Editor" on a subfolder from a parent, **When** the admin sets an explicit "Viewer" permission on that subfolder, **Then** the user's effective permission on the subfolder becomes "Viewer".
2. **Given** an override set on folder B (child of A), **When** the user navigates to folder C (child of B), **Then** the override from B is inherited by C unless another override exists on C.
3. **Given** a user with an override on a subfolder, **When** the admin removes the override, **Then** the user's permission reverts to the inherited permission from the parent.
4. **Given** a file inside a folder with mixed permissions, **When** the system evaluates access, **Then** the most specific (closest ancestor with explicit permission) takes precedence.

---

### User Story 3 - External Sharing via Secure Links (Priority: P2)

A user with appropriate permissions creates a shareable link for a file or folder that can be accessed by anyone with the link, including external users who do not have a system account. The link creator configures security options: password protection, expiration date, and download restrictions.

**Why this priority**: External sharing is essential for collaboration with clients, partners, and vendors who should not have full system accounts. This enables controlled content distribution beyond the organization boundary.

**Independent Test**: User creates a share link for "Proposal.pdf" with a password and 7-day expiry. An external person accesses the link, enters the password, and views the document. After 7 days, the link no longer works.

**Acceptance Scenarios**:

1. **Given** a user with "Editor" or "Admin" permission on a file, **When** they create a share link, **Then** the system generates a unique URL that grants access to the file without authentication.
2. **Given** a share link with a password set, **When** an external user accesses the link, **Then** they must enter the correct password before viewing the content.
3. **Given** a share link with an expiration date, **When** someone accesses the link after the expiration, **Then** the system displays a "link expired" message and denies access.
4. **Given** a share link with download disabled, **When** an external user accesses the link, **Then** they can preview the file but the download button is hidden/disabled.
5. **Given** a user who created a share link, **When** they revoke the link, **Then** it immediately stops working for anyone who tries to access it.

---

### User Story 4 - Watermark on Shared Content (Priority: P3)

When sharing content externally with the watermark option enabled, the system overlays a watermark (containing the viewer's email or the share link ID) on previewed documents and downloaded files. This discourages unauthorized redistribution.

**Why this priority**: Watermarking adds a layer of accountability for sensitive documents shared externally. It's important for compliance but not essential for the core sharing workflow.

**Independent Test**: User shares a PDF with watermark enabled. External viewer previews the PDF and sees a diagonal watermark with the link ID. If download is allowed, the downloaded file also contains the watermark.

**Acceptance Scenarios**:

1. **Given** a share link with watermark enabled, **When** an external user previews the document, **Then** a semi-transparent watermark overlay is visible showing the share link identifier or viewer context.
2. **Given** a share link with watermark and download enabled, **When** the external user downloads the file, **Then** the downloaded file has the watermark embedded.
3. **Given** a share link without watermark enabled, **When** the external user previews or downloads the file, **Then** no watermark is applied.

---

### User Story 5 - Share Link Management Dashboard (Priority: P2)

Users can view all share links they have created, see access statistics (view count, last accessed), and manage link settings (update password, extend expiry, revoke). Admins can view and manage all share links across the workspace.

**Why this priority**: Without management capability, users lose track of what they've shared externally, creating security blind spots. This provides visibility and control over external access.

**Independent Test**: User views their share links dashboard. They see 3 active links with view counts. They revoke one link and extend another's expiry by 7 days.

**Acceptance Scenarios**:

1. **Given** a user who has created share links, **When** they open the share links dashboard, **Then** they see a list of all their links with status (active/expired/revoked), view count, and creation date.
2. **Given** an active share link, **When** the user updates the password or extends the expiry, **Then** the changes take effect immediately.
3. **Given** a workspace admin, **When** they view the workspace share links dashboard, **Then** they see all share links created by any user in the workspace.
4. **Given** an expired share link, **When** the user extends its expiry to a future date, **Then** the link becomes active again.

---

### Edge Cases

- What happens when a user is removed from a group that granted them folder access? Access is revoked immediately; cached permissions are invalidated.
- What happens when a folder with active share links is moved? Share links remain valid and follow the file/folder to its new location.
- What happens when a shared file is permanently deleted? All associated share links become invalid and display a "content not available" message.
- What happens when the link creator's account is deactivated? Share links they created remain active (owned by the workspace) but can be managed by admins.
- What happens when a password-protected link's password is changed? Existing sessions are invalidated; new access requires the new password.

## Requirements

### Functional Requirements

- **FR-001**: System MUST support permission levels: Viewer (read/download), Editor (read/write/upload/delete), Admin (full control + permission management).
- **FR-002**: System MUST enforce permission inheritance from parent folders to subfolders and contained files.
- **FR-003**: System MUST allow explicit permission overrides at any folder level that take precedence over inherited permissions.
- **FR-004**: System MUST resolve effective permissions by finding the closest ancestor with an explicit permission for the user (direct or via group).
- **FR-005**: System MUST filter navigation trees and file listings to only show content the requesting user has access to.
- **FR-006**: System MUST allow creating share links for files and folders with configurable password, expiry date, and download restriction.
- **FR-007**: System MUST validate share link access (check expiry, password, revocation status) before granting access.
- **FR-008**: System MUST support revoking share links immediately.
- **FR-009**: System MUST track share link access statistics (view count, last accessed timestamp).
- **FR-010**: System MUST support watermark overlay on shared content when the watermark option is enabled.
- **FR-011**: System MUST invalidate permission caches when permissions change (assign, revoke, group membership change).
- **FR-012**: System MUST only allow users with "Editor" or "Admin" permission to create share links for their accessible content.

### Key Entities

- **Permission**: Grants a user or group a specific role on a file or folder. Supports inheritance and override.
- **SharedLink**: A secure, revocable URL providing external access to a file or folder with configurable restrictions (password, expiry, watermark, download toggle).
- **ShareLinkAccess**: Tracks each access event on a shared link (timestamp, IP, user-agent) for audit and statistics.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can assign and revoke permissions on folders in under 3 seconds.
- **SC-002**: Permission inheritance correctly resolves across folder hierarchies of 10+ levels within 200ms.
- **SC-003**: Navigation tree filtering excludes unauthorized content with no visible delay compared to unfiltered loading.
- **SC-004**: Share links can be created, accessed, and revoked with end-to-end response times under 2 seconds.
- **SC-005**: Watermarked previews are generated within 5 seconds of first access.
- **SC-006**: 100% of share link accesses are logged with timestamp and viewer context.

## Assumptions

- The existing folder permission system (FolderPermission entity, FolderPermissionService) will be extended rather than replaced.
- File-level permissions inherit from the parent folder by default; explicit file-level permission override is deferred to a future iteration.
- Watermark generation for downloaded files uses the existing Python worker infrastructure (from 003-file-upload-storage).
- Share link URLs use cryptographically random tokens (not sequential IDs) to prevent enumeration.
- External viewers accessing share links do not need to create an account.
- The system uses bcrypt for share link password hashing (same pattern as user authentication).
