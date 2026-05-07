# Research: Admin Console

**Date**: 2026-05-06  
**Feature**: 014-admin-console

## Research Task 1: Existing Backend API Coverage

**Decision**: Leverage existing APIs (UserController, RoleController, GroupController, PolicyController) for CRUD operations. Add only AdminController for analytics and StorageQuota update endpoints.

**Rationale**: The backend already has comprehensive APIs for user, role, group, and policy management. Duplicating these in an admin controller would violate DRY. The admin console frontend will call existing APIs directly; only analytics aggregation and storage quota management are missing.

**Alternatives Considered**:
- Create a unified AdminController wrapping all CRUD ops → Rejected: adds unnecessary abstraction layer over existing, working endpoints.
- Add admin analytics to DashboardController → Rejected: DashboardController serves user-level dashboard; admin analytics are org-scoped with different permission requirements.

## Research Task 2: Storage Quota Management

**Decision**: Add PUT endpoint to a new AdminController for updating storage quota settings (maxStorageBytes, maxFileSizeBytes, allowedExtensions, blockedExtensions, trashRetentionDays). GET already exists at `/files/quota`.

**Rationale**: StorageQuotaService has read + validation methods but no update methods. The entity has all fields. Only need: service method to update quota fields + controller endpoint.

**Alternatives Considered**:
- Add to FileController → Rejected: storage policy management is admin concern, not file management.
- Add to OrganizationController → Acceptable but AdminController groups all new admin endpoints.

## Research Task 3: Admin Analytics API Design

**Decision**: Create `GET /api/v1/admin/analytics` returning org-wide metrics: total users (by status), total files, storage usage breakdown, upload trends (daily counts for last 30 days), active users (last 30 days), role distribution. Cache results in Redis with 5-minute TTL.

**Rationale**: DashboardService provides user-level summary (recent files, personal activity). Admin analytics need org-wide aggregation queries that are more expensive — caching prevents repeated DB hits.

**Alternatives Considered**:
- Extend existing dashboard/summary → Rejected: different permission scope (any user vs admin-only), different data granularity.
- Real-time queries without caching → Rejected: aggregation across users/files tables with large datasets would be slow.

## Research Task 4: Password Reset vs Change

**Decision**: Keep existing `PUT /users/{userId}/password` endpoint. Add logic: if the caller is not the target user (admin resetting), skip old password validation. If the caller IS the target user, require currentPassword field.

**Rationale**: Currently currentPassword is optional and never validated. This is a security gap. For admin console, admins need to reset passwords without knowing the old one, but users changing their own should verify identity.

**Alternatives Considered**:
- Separate endpoint POST /users/{userId}/reset-password → Rejected: adds unnecessary endpoint; the existing endpoint can handle both cases with caller context.

## Research Task 5: Self-Deactivation Protection

**Decision**: Add check in UserService.deactivate() to prevent admins from deactivating their own account. The last-admin check already exists (checkLastAdmin method).

**Rationale**: The spec requires preventing self-deactivation (Edge Case 1). Currently only the last-admin check exists. An admin who deactivates themselves cannot undo it.

**Alternatives Considered**:
- Allow self-deactivation with confirmation → Rejected: spec explicitly prohibits it, and there's no recovery mechanism.

## Research Task 6: Frontend Architecture

**Decision**: Create AdminPage as a route-level page at `/admin/*` with an AdminLayout component providing sub-navigation. Individual admin sections (UserManagement, RoleManagement, etc.) are child components rendered within AdminLayout based on active tab/route.

**Rationale**: Follows the established pattern from AuditPage (tab-based navigation) and WorkspacePage (nested content areas). Using React Router nested routes under `/admin` provides clean URL structure.

**Alternatives Considered**:
- Single page with tabs only (no sub-routes) → Rejected: admin sections are substantial enough to warrant individual URLs for bookmarking and navigation.
- Separate pages per section (AdminUsersPage, AdminRolesPage, etc.) → Acceptable but AdminLayout wrapper with nested routes is cleaner.

## Research Task 7: Admin Access Control (Frontend)

**Decision**: Gate admin routes using `authUser.organizationRole === 'Admin'` check. The ProtectedRoute component already handles authentication; add an admin-specific check. Backend endpoints already require `manage-users`, `manage-roles`, etc. permissions.

**Rationale**: AuthContext provides `organizationRole` as a string. Admin role has all manage-* permissions. No new permissions needed since `manage-users`, `manage-roles`, `manage-groups`, `manage-policies` already exist and are assigned to Admin role.

**Alternatives Considered**:
- Add permissions array to AuthContext → Deferred: current role-name check is sufficient for admin vs non-admin; fine-grained permission checks can be added later.

## Research Task 8: Inline Styling Approach

**Decision**: Use inline React styles (CSSProperties objects) consistent with all existing pages. No CSS framework.

**Rationale**: The entire frontend uses inline styles with a consistent Tailwind-inspired color palette. Introducing a CSS framework would create inconsistency and require refactoring existing pages.

**Alternatives Considered**:
- Introduce Tailwind CSS → Rejected: would require touching every existing component.
- CSS modules → Rejected: inconsistent with codebase convention.
