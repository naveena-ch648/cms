# Feature Specification: Metadata & Tagging System

**Feature Branch**: `010-metadata-tagging`  
**Created**: 2026-05-06  
**Status**: Draft  
**Input**: User description: "Build metadata system with custom fields (text, number, date, dropdown). Enable tagging and metadata-based filtering."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Define Custom Metadata Fields (Priority: P1)

A workspace administrator creates custom metadata fields to categorize and describe documents in a way that fits their team's workflow. They can define field types (text, number, date, dropdown) with validation rules and apply them across the workspace.

**Why this priority**: Without field definitions, no metadata can be assigned to files. This is the foundation for all other metadata functionality.

**Independent Test**: Admin navigates to workspace settings, creates a custom field (e.g., "Project Name" as text, "Due Date" as date, "Department" as dropdown with options), and sees it available when viewing file details.

**Acceptance Scenarios**:

1. **Given** a workspace admin is on the workspace settings page, **When** they create a new metadata field with name "Contract Value" and type "number", **Then** the field is saved and appears in the available fields list.
2. **Given** an admin creates a dropdown field "Status", **When** they define options "Active", "Expired", "Pending", **Then** those options are available when assigning metadata to files.
3. **Given** a metadata field exists, **When** the admin edits its name or options, **Then** existing values assigned to files are preserved and the change is reflected globally.
4. **Given** a non-admin user accesses workspace settings, **When** they attempt to create or modify metadata fields, **Then** the system denies access.

---

### User Story 2 - Assign Metadata Values to Files (Priority: P1)

Users assign metadata values to individual files using the defined custom fields. They can set text, pick from dropdowns, enter numbers, or select dates — directly from the file detail panel.

**Why this priority**: Assigning metadata is the core interaction that gives value to the field definitions. Without this, fields have no purpose.

**Independent Test**: User opens a file's detail panel, sees available custom fields, fills in values (text, number, date, dropdown), saves, and sees them persisted on reload.

**Acceptance Scenarios**:

1. **Given** a file has custom fields defined in its workspace, **When** a user opens the file detail panel, **Then** they see all custom fields with input controls matching their types.
2. **Given** a user enters "2026-12-31" for a date field, **When** they save, **Then** the value is stored and displayed correctly on subsequent views.
3. **Given** a dropdown field has options ["HR", "Finance", "Engineering"], **When** a user selects "Finance", **Then** only that valid option is stored.
4. **Given** a number field has been defined, **When** a user enters non-numeric text, **Then** the system shows a validation error and does not save.

---

### User Story 3 - Tag Files with Free-Form Tags (Priority: P2)

Users can add free-form tags to files for flexible categorization beyond structured metadata. Tags support autocomplete from previously used tags in the workspace.

**Why this priority**: Tags provide lightweight, unstructured categorization that complements the rigid metadata fields. They enable organic taxonomy growth.

**Independent Test**: User opens a file, types a tag name, sees autocomplete suggestions from existing workspace tags, adds the tag, and it appears on the file.

**Acceptance Scenarios**:

1. **Given** a user is viewing a file, **When** they type "confidential" in the tag input, **Then** the tag is added to the file and visible immediately.
2. **Given** the workspace has existing tags ["urgent", "draft", "final"], **When** a user starts typing "ur", **Then** "urgent" appears as an autocomplete suggestion.
3. **Given** a file has tags, **When** a user removes a tag, **Then** it is removed from the file but remains available as an autocomplete suggestion for other files.
4. **Given** a user adds a new tag that doesn't exist yet, **When** they save, **Then** the tag is created and becomes available for future autocomplete.

---

### User Story 4 - Filter Files by Metadata and Tags (Priority: P2)

Users can filter the file list by metadata values and tags. Filters can be combined (e.g., "Department = Finance AND tag = urgent") to quickly find relevant documents.

**Why this priority**: Filtering turns stored metadata into actionable navigation. Without it, metadata is just stored data with no retrieval benefit.

**Independent Test**: User applies a metadata filter (e.g., "Status = Active") on the file list, and only matching files are shown. Combining with a tag filter further narrows results.

**Acceptance Scenarios**:

1. **Given** files have metadata "Department" set to various values, **When** a user filters by "Department = HR", **Then** only files with that value are displayed.
2. **Given** a user applies a tag filter "confidential", **When** viewing the file list, **Then** only files tagged "confidential" appear.
3. **Given** a user combines filters "Status = Active" AND tag "urgent", **When** results load, **Then** only files matching both criteria are shown.
4. **Given** no files match the applied filter, **When** the filter is active, **Then** the system shows an empty state with a message indicating no results.
5. **Given** a user has applied filters, **When** they clear all filters, **Then** the full file list is restored.

---

### User Story 5 - Bulk Metadata Assignment (Priority: P3)

Users can select multiple files and assign or update metadata values in bulk, saving time when categorizing large numbers of documents.

**Why this priority**: Bulk operations significantly improve efficiency for power users managing large document sets but are not required for basic functionality.

**Independent Test**: User selects 5 files, opens bulk edit, sets "Department = Legal" for all, and all 5 files reflect the updated value.

**Acceptance Scenarios**:

1. **Given** a user selects multiple files, **When** they choose "Edit Metadata" from bulk actions, **Then** a dialog shows available fields with batch editing controls.
2. **Given** a user sets "Priority" to "High" in bulk edit for 10 files, **When** they confirm, **Then** all 10 files have their "Priority" field updated.
3. **Given** a user bulk-adds a tag "reviewed", **When** they confirm, **Then** the tag is added to all selected files (without removing existing tags).

---

### Edge Cases

- What happens when a metadata field is deleted that has values assigned to files? Values are soft-deleted and hidden from UI but preserved in database for audit.
- What happens when a dropdown option is removed but files use that value? Existing files retain the value (shown as "legacy") but new assignments cannot select it.
- How does the system handle concurrent metadata edits to the same file? Last-write-wins with optimistic locking; user sees a conflict warning if the value changed since they opened the panel.
- What happens when filtering on a field that has null values? Files with null values for that field are excluded from results unless explicitly filtering for "empty".
- Maximum number of custom fields per workspace? 50 fields per workspace.
- Maximum number of tags per file? 20 tags per file.
- Maximum tag length? 50 characters.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow workspace admins to create custom metadata fields with types: text, number, date, dropdown.
- **FR-002**: System MUST allow workspace admins to define dropdown options for dropdown-type fields.
- **FR-003**: System MUST allow workspace admins to edit and delete custom metadata fields.
- **FR-004**: System MUST allow users to assign metadata values to files based on defined fields.
- **FR-005**: System MUST validate metadata values according to field type (numeric validation for number fields, date format for date fields, option membership for dropdowns).
- **FR-006**: System MUST allow users to add, remove, and view free-form tags on files.
- **FR-007**: System MUST provide tag autocomplete based on existing tags within the workspace.
- **FR-008**: System MUST support filtering the file list by metadata field values.
- **FR-009**: System MUST support filtering the file list by tags.
- **FR-010**: System MUST support combining multiple metadata and tag filters (AND logic).
- **FR-011**: System MUST support bulk metadata assignment for multiple selected files.
- **FR-012**: System MUST support bulk tag assignment for multiple selected files.
- **FR-013**: System MUST enforce a limit of 50 custom fields per workspace.
- **FR-014**: System MUST enforce a limit of 20 tags per file and 50 characters per tag.
- **FR-015**: System MUST restrict metadata field management to workspace admins only.
- **FR-016**: System MUST index metadata and tags in the search engine for searchability.
- **FR-017**: System MUST preserve existing metadata values when a field definition is modified.
- **FR-018**: System MUST soft-delete metadata values when a field is deleted.

### Key Entities

- **MetadataField**: Defines a custom field for a workspace — includes name, type (text/number/date/dropdown), options (for dropdowns), display order, required flag, workspace association.
- **MetadataValue**: Stores an assigned value for a specific file and field combination — includes the actual value, file reference, field reference.
- **Tag**: A free-form label associated with a file — includes name, file reference, workspace-level uniqueness for autocomplete.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can define a new custom metadata field and assign values to a file within 30 seconds.
- **SC-002**: File list filtering by metadata or tags returns results within 1 second for workspaces with up to 100,000 files.
- **SC-003**: Tag autocomplete suggestions appear within 300ms of user typing.
- **SC-004**: Bulk metadata assignment for up to 100 files completes within 5 seconds.
- **SC-005**: 90% of users successfully find files using metadata filters on their first attempt.
- **SC-006**: System supports at least 50 custom fields per workspace without performance degradation.

## Assumptions

- Existing file management system (upload, versioning, permissions) is operational.
- OpenSearch is already deployed and integrated for keyword search (Step 8).
- Metadata field definitions are workspace-scoped (not organization-wide).
- Tags are workspace-scoped for autocomplete but stored per-file.
- The existing RBAC system distinguishes workspace admins from regular users.
- Only workspace admins can manage field definitions; all workspace members can assign values and tags.
- Metadata indexing in OpenSearch reuses the existing search infrastructure.
