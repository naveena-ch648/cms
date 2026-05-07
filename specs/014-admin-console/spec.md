# Feature Specification: Admin Console

**Feature Branch**: `014-admin-console`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build admin console for managing users, roles, storage limits, policies, integrations, and system analytics."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — User Management Dashboard (Priority: P1)

An organization administrator opens the Admin Console and navigates to the Users section. They see a paginated, searchable table of all users in their organization showing name, email, role, status, and last login date. The admin can create new users, edit existing users, change a user's role, reset passwords, and activate/deactivate accounts. Bulk actions allow selecting multiple users to change status or role at once.

**Why this priority**: User management is the most fundamental admin capability. Without it, administrators cannot onboard, offboard, or govern user access — making all other admin features secondary.

**Independent Test**: Can be tested by logging in as an admin, navigating to /admin/users, performing CRUD operations on users, and verifying results persist.

**Acceptance Scenarios**:

1. **Given** an admin is on the Users page, **When** they search for a user by name or email, **Then** the table filters to show matching users in real time.
2. **Given** an admin clicks "Create User", **When** they fill in name, email, and role and submit, **Then** a new user account is created and appears in the user list.
3. **Given** an admin selects a user, **When** they change the user's role from Viewer to Editor, **Then** the user's permissions update immediately and an audit event is logged.
4. **Given** an admin selects a user, **When** they deactivate the user's account, **Then** the user can no longer log in and their status shows as "Inactive".
5. **Given** an admin selects multiple users, **When** they apply a bulk role change, **Then** all selected users' roles are updated.

---

### User Story 2 — Role & Permission Management (Priority: P1)

An administrator navigates to the Roles section to view all roles in the organization including system-defined and custom roles. They can create custom roles by selecting from a list of available permissions, edit existing custom roles, and view which users/groups are assigned to each role. System roles (Admin, Editor, Viewer) are read-only but can be inspected. The admin can see a permission matrix showing the full mapping of roles to permissions.

**Why this priority**: Role management directly controls what every user can do across the platform. It is tightly coupled with user management and is required for proper access governance.

**Independent Test**: Can be tested by creating a custom role with specific permissions, assigning it to a user, and verifying the user gains exactly those permissions.

**Acceptance Scenarios**:

1. **Given** an admin is on the Roles page, **When** they view the role list, **Then** they see all system and custom roles with user counts for each.
2. **Given** an admin clicks "Create Role", **When** they name the role and select permissions, **Then** a new custom role is created and can be assigned to users.
3. **Given** an admin edits a custom role, **When** they add or remove permissions, **Then** all users with that role immediately gain or lose those capabilities.
4. **Given** an admin views a system role, **When** they inspect its permissions, **Then** the permissions are displayed but editing is disabled.
5. **Given** an admin views the permission matrix, **When** they review it, **Then** they see a clear grid of all roles vs all permissions with checkmarks.

---

### User Story 3 — Group Management (Priority: P2)

An administrator navigates to the Groups section to manage organizational groups. They can create groups, add or remove members, assign workspace-level roles to groups, and view group membership across workspaces. Groups simplify bulk permission management by allowing role assignment to many users at once.

**Why this priority**: Groups reduce admin overhead for organizations with many users. They are important but build on top of user and role management.

**Independent Test**: Can be tested by creating a group, adding members, assigning a workspace role, and verifying all group members inherit the workspace permissions.

**Acceptance Scenarios**:

1. **Given** an admin is on the Groups page, **When** they create a group and add members, **Then** the group appears in the list with accurate member count.
2. **Given** an admin assigns a workspace role to a group, **When** they save, **Then** all group members can access that workspace with the assigned role.
3. **Given** an admin removes a user from a group, **When** they save, **Then** the user loses permissions inherited from that group.

---

### User Story 4 — Storage Quota & Policy Administration (Priority: P2)

An administrator navigates to the Storage & Policies section to configure organization-wide limits and policies. They can set maximum storage capacity, per-file size limits, allowed/blocked file extensions, and trash retention period. They can view current storage usage with a visual breakdown. They can also configure organization policies such as password requirements and session timeout.

**Why this priority**: Storage and policy management prevents resource abuse and ensures compliance. Important for governance but existing defaults cover most cases initially.

**Independent Test**: Can be tested by changing the max file size policy, attempting to upload a file exceeding the limit, and verifying the upload is rejected.

**Acceptance Scenarios**:

1. **Given** an admin views the Storage section, **When** the page loads, **Then** they see current usage, total capacity, and a usage percentage bar.
2. **Given** an admin sets max file size to 50MB, **When** a user uploads a 60MB file, **Then** the upload is rejected with an appropriate error message.
3. **Given** an admin adds ".exe" to blocked extensions, **When** a user uploads a .exe file, **Then** the upload is rejected.
4. **Given** an admin updates the password policy to require 12+ characters, **When** a user changes their password, **Then** the new requirement is enforced.
5. **Given** an admin changes session timeout to 30 minutes, **When** a user is idle for 30+ minutes, **Then** their session expires.

---

### User Story 5 — System Analytics Dashboard (Priority: P2)

An administrator navigates to the Analytics section to view organization-wide metrics. They see key performance indicators: total users, active users (last 30 days), total files, total storage used, and recent activity trends. Charts show file uploads over time, storage growth trend, and top active users. The admin can filter analytics by date range.

**Why this priority**: Analytics give administrators visibility into platform usage patterns and help with capacity planning. Valuable but not blocking core admin operations.

**Independent Test**: Can be tested by viewing the analytics dashboard and verifying that the displayed metrics accurately reflect the current state of the organization.

**Acceptance Scenarios**:

1. **Given** an admin opens the Analytics section, **When** the page loads, **Then** they see summary cards for total users, files, storage used, and active users.
2. **Given** an admin views the uploads trend chart, **When** they select a 30-day range, **Then** a chart shows daily upload counts for that period.
3. **Given** an admin views storage growth, **When** the chart loads, **Then** it shows cumulative storage usage over time.

---

### User Story 6 — Admin Navigation & Layout (Priority: P1)

An administrator accesses the Admin Console from the main application sidebar. The console has its own navigation with sections for Users, Roles, Groups, Storage & Policies, and Analytics. Only users with admin-level permissions can access the console. Non-admin users do not see the admin navigation link.

**Why this priority**: Without the navigation shell and access control, no admin features can be reached. This is the structural foundation for all other admin stories.

**Independent Test**: Can be tested by logging in as an admin and verifying the Admin Console link is visible and navigable, then logging in as a non-admin and verifying it is hidden.

**Acceptance Scenarios**:

1. **Given** a user with admin permissions, **When** they view the sidebar, **Then** they see an "Admin" navigation item.
2. **Given** a user without admin permissions, **When** they view the sidebar, **Then** no "Admin" navigation item is visible.
3. **Given** an admin clicks "Admin", **When** the Admin Console loads, **Then** they see a sidebar with sections: Users, Roles, Groups, Storage & Policies, Analytics.
4. **Given** a non-admin user navigates directly to /admin, **When** the page loads, **Then** they are redirected or shown an access denied message.

---

### Edge Cases

- What happens when an admin tries to deactivate their own account? The system should prevent self-deactivation.
- What happens when the last admin tries to remove their own admin role? The system should prevent removing the last admin.
- What happens when a role is deleted that is still assigned to users? The system should prevent deletion or require reassignment first.
- How does the system handle concurrent edits to the same user or role by two admins? Last-write-wins with optimistic concurrency.
- What happens when storage quota is reduced below current usage? Existing files are preserved; new uploads are blocked until usage is under the new limit.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide an admin console accessible only to users with `manage-users` or equivalent admin permissions.
- **FR-002**: System MUST allow admins to list, search, create, edit, deactivate, and reactivate user accounts within their organization.
- **FR-003**: System MUST allow admins to reset a user's password and change a user's organization-level role.
- **FR-004**: System MUST allow admins to perform bulk operations (role change, status change) on multiple selected users.
- **FR-005**: System MUST display all roles (system and custom) with their associated permissions and user counts.
- **FR-006**: System MUST allow admins to create custom roles by selecting from available permissions, and to edit or delete custom roles.
- **FR-007**: System MUST prevent deletion of system-defined roles and roles currently assigned to users.
- **FR-008**: System MUST allow admins to create, edit, and delete groups, and to manage group membership.
- **FR-009**: System MUST display current storage usage (used vs total) with a visual indicator.
- **FR-010**: System MUST allow admins to configure storage quotas: max capacity, max file size, allowed/blocked file extensions, and trash retention days.
- **FR-011**: System MUST allow admins to configure organization policies: password requirements and session timeout.
- **FR-012**: System MUST display organization-wide analytics: total users, active users, total files, storage used, and upload/storage trends.
- **FR-013**: System MUST provide a consistent navigation layout with sections for Users, Roles, Groups, Storage & Policies, and Analytics.
- **FR-014**: System MUST hide the admin console navigation from non-admin users and deny access if navigated to directly.
- **FR-015**: System MUST prevent admins from deactivating their own account or removing the last admin role from an organization.
- **FR-016**: System MUST log all admin actions (user changes, role changes, policy changes) as audit events.

### Key Entities

- **User**: Person within an organization. Key attributes: name, email, status (active/inactive/locked), organization-level role, last login timestamp, creation date.
- **Role**: Named set of permissions. Types: system-defined (read-only) or custom (editable). Attributes: name, description, permissions list, user count, parent role.
- **Group**: Named collection of users for bulk permission assignment. Attributes: name, description, member list, workspace role assignments.
- **Storage Quota**: Organization-level storage configuration. Attributes: max storage, used storage, max file size, allowed/blocked extensions, trash retention days.
- **Organization Policy**: Organization-wide behavioral settings. Attributes: password min length, password complexity rules, session timeout duration.
- **Analytics Snapshot**: Aggregated metrics for an organization. Attributes: total users, active users (30d), total files, total storage, upload counts by period.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administrators can complete user onboarding (create user + assign role) in under 1 minute.
- **SC-002**: All admin pages load within 2 seconds with up to 10,000 users in the organization.
- **SC-003**: Role permission changes take effect for affected users within 5 seconds without requiring logout.
- **SC-004**: Storage quota changes are enforced on the very next file upload attempt.
- **SC-005**: Analytics data reflects the current state of the organization within a 5-minute delay.
- **SC-006**: 100% of admin actions are recorded in the audit log with actor, action, and timestamp.
- **SC-007**: Non-admin users have zero visibility into admin-only sections of the interface.

## Assumptions

- The existing user, role, group, and storage quota APIs provide sufficient backend coverage; the admin console primarily needs a frontend UI and minor API additions for analytics aggregation.
- The admin console is scoped to organization-level administration; platform-level (super-admin / multi-org) management is out of scope.
- Charts and analytics visualizations will use simple rendered elements (bars, lines) without requiring a third-party charting library in v1; lightweight libraries may be added if needed.
- All admin actions will use the existing audit logging infrastructure (audit_events table + OpenSearch indexing).
- Password policy enforcement relies on the existing authentication flow; the admin console only configures the policy parameters.
- The admin console will reuse the existing application layout shell (AppLayout with sidebar) and add an admin-specific sub-navigation.
