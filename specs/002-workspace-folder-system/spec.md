# Feature Specification: Workspace Folder System

**Feature Branch**: `002-workspace-folder-system`  
**Created**: 2026-05-05  
**Status**: Draft  
**Input**: User description: "Build hierarchical workspace system. Each workspace contains folders and subfolders. Support folder tree, breadcrumbs, favorites, recent items, drag-drop reorganization, and inheritance-based permissions."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Folder CRUD & Hierarchy Navigation (Priority: P1) 🎯 MVP

A workspace member opens a workspace and sees a hierarchical folder tree in a sidebar. They can create folders and subfolders, rename them, and delete them. Clicking a folder shows its contents (child folders) in the main content area. A breadcrumb trail at the top shows the current path from the workspace root to the active folder, and each breadcrumb segment is clickable for quick navigation back up the tree.

**Why this priority**: Folders are the foundational organizing structure of any content management system. Without folders, there is no place to put files or documents — every subsequent feature depends on this hierarchy existing and being navigable.

**Independent Test**: A user creates a workspace, then creates a folder "Projects" at the root. Inside "Projects" they create "Q1" and "Q2" subfolders. Inside "Q1" they create "Reports". The tree sidebar shows the full hierarchy. Clicking "Reports" updates the breadcrumb to "Projects / Q1 / Reports" and each segment is clickable. Renaming "Q2" to "Q2-Final" updates the tree. Deleting an empty folder removes it from the tree.

**Acceptance Scenarios**:

1. **Given** a workspace with no folders, **When** the user creates a folder named "Documents", **Then** the folder appears in the tree sidebar and in the main content area at the root level.
2. **Given** a folder "Documents" exists, **When** the user creates a subfolder "Legal" inside it, **Then** "Legal" appears nested under "Documents" in the tree and clicking "Legal" shows breadcrumb "Documents / Legal".
3. **Given** a folder "Legal" exists, **When** the user renames it to "Legal Documents", **Then** the tree and breadcrumb reflect the new name immediately.
4. **Given** an empty folder "Archive", **When** the user deletes it, **Then** it is removed from the tree and no longer accessible.
5. **Given** a folder "Reports" with child folders, **When** the user attempts to delete it, **Then** the system warns that the folder and all its contents will be deleted, and requires confirmation.
6. **Given** a deep folder path "A / B / C / D", **When** the user clicks the "B" breadcrumb segment, **Then** the view navigates to folder "B" showing its direct children.

---

### User Story 2 - Drag-Drop Folder Reorganization (Priority: P2)

A workspace member reorganizes the folder structure by dragging folders to new locations in the tree. They can move a folder (with all its children) under a different parent or to the workspace root. The system prevents invalid moves such as dropping a folder into its own descendant. After a move, the tree updates to reflect the new hierarchy and all breadcrumb paths update accordingly.

**Why this priority**: Reorganization is essential for evolving content structures. Without drag-drop, users would need to delete and recreate entire subtrees to restructure, which is impractical.

**Independent Test**: A user has folders "Projects / Alpha" and "Archive". They drag "Alpha" from under "Projects" to under "Archive". The tree now shows "Archive / Alpha" and the breadcrumb updates. Attempting to drag "Projects" into "Projects / Alpha" is rejected.

**Acceptance Scenarios**:

1. **Given** a folder "Alpha" under "Projects", **When** the user drags "Alpha" onto "Archive", **Then** "Alpha" (and all its children) moves under "Archive" and the tree updates.
2. **Given** a folder "Docs" at root, **When** the user drags a subfolder "Legal" to the root level, **Then** "Legal" becomes a root-level folder.
3. **Given** a folder "A" with child "B", **When** the user attempts to drag "A" into "B", **Then** the system rejects the move with an error message "Cannot move a folder into its own subfolder."
4. **Given** a folder "Reports" is being dragged, **When** the user hovers over valid drop targets, **Then** those targets highlight to indicate valid placement.

---

### User Story 3 - Favorites & Recent Items (Priority: P3)

A user marks frequently used folders as favorites and sees a "Favorites" section at the top of the sidebar for quick access. The system also tracks recently visited folders and displays them in a "Recent" section. Users can add or remove favorites with a single click. Recent items update automatically as the user navigates.

**Why this priority**: Favorites and recents are productivity features that improve daily workflow efficiency but are not required for core folder management to function.

**Independent Test**: A user navigates to "Projects / Q1 / Reports" and clicks the star icon to favorite it. "Reports" now appears in the Favorites section at the top of the sidebar. Later the user navigates through several other folders. The Recent section shows the last 10 visited folders in reverse chronological order. Clicking the star again on "Reports" removes it from favorites.

**Acceptance Scenarios**:

1. **Given** a folder "Reports", **When** the user clicks the favorite (star) icon on it, **Then** "Reports" appears in the Favorites section of the sidebar.
2. **Given** "Reports" is favorited, **When** the user clicks the star icon again, **Then** "Reports" is removed from the Favorites section.
3. **Given** a user navigates to folder "Projects", **When** the user later views the Recent section, **Then** "Projects" appears in the list with a timestamp.
4. **Given** more than 10 folders have been visited, **When** the user views the Recent section, **Then** only the 10 most recently visited folders are shown.
5. **Given** a user has favorites, **When** they click a folder in the Favorites section, **Then** the view navigates directly to that folder with correct breadcrumbs.

---

### User Story 4 - Folder Permission Inheritance (Priority: P2)

When a workspace member is assigned a role on a folder, that permission automatically propagates to all subfolders (inheritance). An administrator can override inherited permissions on any subfolder by assigning an explicit role that takes precedence. Users without at least view permission on a folder cannot see it in the tree. The effective permission on any folder is determined by: explicit assignment > inherited from nearest ancestor > workspace-level role.

**Why this priority**: Permission inheritance is a core security requirement that prevents unauthorized access. It ties directly into the RBAC system built in Step 1 and must be in place before files are added in Step 3.

**Independent Test**: An admin assigns user "Alice" the "Viewer" role on folder "Projects". Alice can see "Projects" and all subfolders. The admin then assigns Alice "Editor" on "Projects / Confidential" explicitly. Alice has Editor access on "Confidential" but only Viewer on other subfolders. Removing Alice's role on "Projects" removes her access to all subfolders except "Confidential" where she has an explicit assignment.

**Acceptance Scenarios**:

1. **Given** user Alice has "Viewer" role on folder "Projects", **When** a subfolder "Q1" exists under "Projects", **Then** Alice inherits "Viewer" access on "Q1" without any explicit assignment.
2. **Given** Alice inherits "Viewer" on "Q1", **When** an admin assigns Alice "Editor" explicitly on "Q1", **Then** Alice's effective role on "Q1" is "Editor" (explicit overrides inherited).
3. **Given** Alice has inherited access to "Q1" via "Projects", **When** Alice's role on "Projects" is removed, **Then** Alice loses access to "Q1" as well (unless she has an explicit assignment on "Q1").
4. **Given** user Bob has no role on folder "Confidential" and no inherited access, **When** Bob views the folder tree, **Then** "Confidential" does not appear in Bob's tree view.
5. **Given** a folder with both inherited and explicit permissions, **When** viewing the permission panel, **Then** the system clearly indicates which permissions are inherited vs explicit.

---

### Edge Cases

- What happens when a user moves a folder to a location where a folder with the same name already exists? The system appends a numeric suffix (e.g., "Reports (2)") to avoid name collisions.
- What happens when a deeply nested folder (10+ levels) is displayed? The breadcrumb truncates intermediate segments with "..." and shows the first segment, an ellipsis, and the last 3 segments, each still clickable.
- What happens when a folder the user is currently viewing is deleted by another user? The user is redirected to the nearest accessible parent folder with a notification.
- What happens when drag-drop is attempted on a mobile or touch device? The system supports touch-based long-press to initiate drag with visual feedback.
- What happens when a user tries to create a folder with an invalid name (e.g., containing "/" or exceeding 255 characters)? The system validates and rejects with a descriptive error message.
- What happens when a favorited folder is deleted? The favorite entry is automatically removed from the user's favorites list.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support creating folders within a workspace at the root level or nested under any existing folder.
- **FR-002**: System MUST enforce unique folder names within the same parent (no two siblings with identical names).
- **FR-003**: System MUST support renaming folders, updating all references (breadcrumbs, favorites, recents) immediately.
- **FR-004**: System MUST support soft-deleting folders (with an option to permanently delete), cascading to all descendant folders.
- **FR-005**: System MUST display a navigable folder tree in the sidebar that reflects the full hierarchy of accessible folders.
- **FR-006**: System MUST display breadcrumb navigation showing the full path from workspace root to the current folder.
- **FR-007**: System MUST support drag-and-drop to move folders (with all descendants) to a new parent or to root level.
- **FR-008**: System MUST prevent circular moves (a folder cannot be moved into its own descendant).
- **FR-009**: System MUST support marking folders as favorites, persisted per-user, with a dedicated Favorites section in the sidebar.
- **FR-010**: System MUST track recently visited folders per-user (last 10) and display them in a Recent section.
- **FR-011**: System MUST support folder-level permission assignments (user-to-folder role mappings).
- **FR-012**: System MUST implement permission inheritance: a child folder inherits its parent's permissions unless explicitly overridden.
- **FR-013**: System MUST resolve effective permissions using the precedence: explicit folder assignment > nearest ancestor inheritance > workspace-level role.
- **FR-014**: System MUST filter the folder tree to show only folders where the user has at least view permission.
- **FR-015**: System MUST support pagination or lazy-loading for folders with many children (100+ items).
- **FR-016**: System MUST validate folder names: non-empty, max 255 characters, no path separator characters.
- **FR-017**: System MUST record folder operations (create, rename, move, delete, permission change) in the audit log.
- **FR-018**: System MUST support retrieving the full ancestor path for any folder (for breadcrumb rendering).

### Key Entities

- **Folder**: Represents a node in the hierarchical tree. Has a name, belongs to a workspace, references a parent folder (null for root-level folders), and has a sort order for display sequencing. Tracks creation and modification timestamps.
- **Folder Permission**: Links a user (or group) to a folder with a specific role. Distinguishes between explicit assignments and inherited permissions. Supports the same role model used at the workspace level.
- **Folder Favorite**: Links a user to a folder they have marked as a favorite. Scoped per user and per workspace.
- **Folder Recent**: Tracks a user's recently accessed folders with timestamps. Scoped per user, limited to the most recent 10 entries per workspace.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create a 5-level deep folder hierarchy and navigate to any level within 2 seconds.
- **SC-002**: Drag-and-drop folder move completes (tree updates visually) within 1 second for a subtree of up to 50 folders.
- **SC-003**: Breadcrumb navigation accurately reflects the current folder path at all times, and clicking any segment navigates correctly.
- **SC-004**: The folder tree loads and renders within 2 seconds for a workspace with up to 500 folders.
- **SC-005**: Favoriting/unfavoriting a folder updates the Favorites section within 500 milliseconds.
- **SC-006**: Permission changes propagate to all affected subfolders within 3 seconds for a subtree of up to 100 folders.
- **SC-007**: Users without view permission on a folder never see that folder in the tree, breadcrumbs, search results, or direct URL access.
- **SC-008**: 95% of users can successfully create, navigate, and reorganize folders without assistance on their first session.

## Assumptions

- Workspaces already exist from Step 1 (multi-tenant foundation) and have functioning RBAC with role inheritance (Viewer, Editor, Admin).
- The folder system builds on top of the existing workspace entity — each folder belongs to exactly one workspace.
- Permission roles for folders reuse the same Role entity from Step 1 (no separate folder-specific role definitions).
- File management (uploading files into folders) is out of scope for this feature and will be addressed in Step 3.
- The maximum practical folder depth is 20 levels; deeper nesting is allowed but not specifically optimized.
- Folder names are case-insensitive for uniqueness checks (e.g., "Reports" and "reports" cannot coexist under the same parent).
- The existing audit logging infrastructure from Step 1 will be reused for folder operation tracking.
- Redis caching from Step 1 will be used for caching folder tree structures and permission resolution.
