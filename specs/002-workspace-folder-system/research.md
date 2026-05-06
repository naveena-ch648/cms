# Research: Workspace Folder System

**Feature**: 002-workspace-folder-system  
**Date**: 2026-05-05  
**Status**: Complete

## R-001: Hierarchical Folder Storage Pattern (MySQL)

**Decision**: Adjacency List with `parent_id` self-referencing foreign key

**Rationale**:
- Simplest pattern to implement and reason about with JPA/Hibernate
- Natural fit for the existing codebase pattern (roles already use `parent_role_id` adjacency list)
- Insert/move/delete operations are O(1) for the target row
- Breadcrumb (ancestor path) retrieval uses MySQL 8.0 recursive CTE (`WITH RECURSIVE`) for efficient ancestor traversal
- The practical depth limit (20 levels) makes recursive queries performant
- JPA `@ManyToOne` self-reference is well-supported

**Alternatives Considered**:
- **Nested Set Model**: Excellent read performance for subtree queries, but move operations require recalculating `lft`/`rgt` for entire subtrees — too expensive for drag-drop reorganization (FR-007)
- **Materialized Path** (e.g., `/1/5/12/`): Simple ancestor queries via `LIKE`, but path updates on move cascade to all descendants — O(n) string updates
- **Closure Table**: Best query flexibility, but requires a separate table with O(n²) rows for deep trees and complex insert/move logic

## R-002: Circular Move Prevention

**Decision**: Walk ancestors from target parent up to root; reject if source folder appears in ancestor chain

**Rationale**:
- Before moving folder A under folder B, query all ancestors of B using recursive CTE
- If A appears in B's ancestor chain, the move would create a cycle → reject with HTTP 400
- This check is O(depth) which is capped at 20 in practice
- Simpler and more reliable than graph-cycle detection algorithms

**Alternatives Considered**:
- **Depth counter with limit**: Doesn't prevent cycles, only limits depth
- **Full subtree materialization**: Over-engineered for the depth constraint

## R-003: Permission Inheritance Resolution

**Decision**: Walk-up resolution at query time with Redis caching

**Rationale**:
- For a given (user, folder), walk up the ancestor chain until an explicit `folder_permissions` row is found
- If none found, fall back to the user's workspace-level role (`user_workspace_roles`)
- Cache the resolved effective permission in Redis with key `folder_perm:{userId}:{folderId}` and TTL of 5 minutes
- Invalidate cache entries when permissions change (explicit assignment or ancestor change)
- This matches the existing `PermissionService` pattern that uses Redis for workspace permission caching

**Alternatives Considered**:
- **Materialized permission rows**: Pre-compute and store inherited permissions for every user×folder combination — too many rows for large trees, complex cascade on permission change
- **Database trigger-based propagation**: MySQL triggers for inheritance propagation — hard to debug, poor testability

## R-004: Folder Tree API Design

**Decision**: Single endpoint returns flattened list with `parentId` references; frontend builds the tree client-side

**Rationale**:
- `GET /api/v1/workspaces/{workspaceId}/folders` returns all accessible folders as a flat array with `id`, `parentId`, `name`, `sortOrder`
- Frontend builds tree structure from flat list in O(n) time using a Map lookup
- For workspaces with 500+ folders, supports `lazy=true` parameter to load only root-level folders initially, then `GET /api/v1/workspaces/{workspaceId}/folders/{folderId}/children` for expansion
- This avoids deeply nested JSON responses that are harder to paginate

**Alternatives Considered**:
- **Nested JSON tree response**: Server builds the full tree — harder to paginate, larger payload, server does work the client can do efficiently
- **GraphQL**: Overkill for this project's REST-based architecture

## R-005: Drag-Drop Frontend Implementation

**Decision**: Use HTML5 Drag and Drop API with custom React components

**Rationale**:
- Native browser API — no additional library dependency
- React state manages drag source, drop target, and visual indicators
- The `onDragStart`, `onDragOver`, `onDrop` events on tree nodes handle the UX
- Drop validation (circular check) done client-side with local tree data for instant feedback, then confirmed server-side on the move API call
- Touch support via `touch-action: none` CSS and `touchstart`/`touchmove`/`touchend` event handlers for mobile

**Alternatives Considered**:
- **react-dnd**: Popular library but adds ~40KB bundle; not needed for tree-only drag-drop
- **dnd-kit**: Modern alternative but still an extra dependency for a single use case

## R-006: Favorites & Recents Storage

**Decision**: Dedicated MySQL tables (`folder_favorites`, `folder_recents`) with per-user scoping

**Rationale**:
- `folder_favorites`: Composite key `(user_id, folder_id)` — simple toggle operations
- `folder_recents`: `(user_id, folder_id)` with `accessed_at` timestamp, capped at 10 per user per workspace via application-level enforcement on insert
- Queried per-workspace for sidebar display
- No Redis caching needed — these are low-volume, user-specific queries that hit indexed lookups

**Alternatives Considered**:
- **Redis-only storage**: Faster but not durable; lost on Redis restart
- **Local storage (frontend)**: Not portable across devices/browsers
- **Single JSON column on users table**: Poor queryability and concurrency

## R-007: Folder Name Uniqueness & Validation

**Decision**: Case-insensitive unique constraint per parent + application-level validation

**Rationale**:
- MySQL unique index on `(workspace_id, parent_id, LOWER(name))` enforces sibling uniqueness
- Since `parent_id` can be NULL (root folders), use a sentinel value approach: store `parent_id = 0` for root folders with a virtual "root" concept, OR use a nullable composite index (MySQL handles NULL in unique indexes — two NULLs are considered distinct)
- Better approach: Use `COALESCE(parent_id, 0)` in a generated column for the unique index, or enforce uniqueness in the application layer with a query before insert
- Application validates: non-empty, max 255 chars, no `/`, `\`, or null characters

**Alternatives Considered**:
- **Case-sensitive uniqueness**: Confusing for users who don't expect "Reports" and "reports" to coexist

## R-008: Redis Caching Strategy for Folder Trees

**Decision**: Cache full folder tree per workspace, invalidate on any folder mutation

**Rationale**:
- Cache key: `folder_tree:{workspaceId}` containing the JSON-serialized flat folder list
- TTL: 10 minutes
- Invalidation: Any folder CRUD or move operation for a workspace invalidates its cached tree
- Permission filtering happens after cache retrieval (filter by user's accessible folder IDs)
- This balances read performance (most requests are tree reads) with write simplicity (full invalidation avoids partial-update complexity)

**Alternatives Considered**:
- **Per-folder caching**: Fine-grained but complex invalidation on move operations that affect subtrees
- **No caching**: Acceptable for small workspaces but won't meet <2s target for 500-folder workspaces
