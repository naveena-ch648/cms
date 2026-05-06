# Research: RBAC & Sharing System

**Feature**: 005-rbac-sharing  
**Date**: 2026-05-06

## 1. Permission Inheritance Resolution Strategy

**Decision**: Closest-ancestor-wins with explicit override markers

**Rationale**: The folder hierarchy (A → B → C) requires resolving effective permissions by walking up the tree from the target folder to the root. The first explicit permission found for the user (direct or via group) determines access. This avoids storing denormalized permissions on every node while keeping resolution predictable.

**Algorithm**:
1. Query all `folder_permissions` for the user (direct + via groups) in the ancestry path
2. Order by depth (deepest first)
3. Return the first match as the effective permission
4. If no match found → deny access

**Alternatives considered**:
- Materialized permission view (denormalized): Rejected — expensive to maintain on permission changes, complex cascading updates
- ACL bitmask per folder: Rejected — doesn't scale well for group-based permissions, hard to audit
- Store computed permissions in Redis: Adopted as cache layer but not source of truth

## 2. Permission Cache Strategy

**Decision**: Cache effective permission per (user_id, folder_id) in Redis with 5min TTL

**Rationale**: Existing `FolderPermissionService` already uses Redis with prefix `folder_perm:` and 5min TTL. Extending this pattern to cache resolved (inherited) permissions avoids repeated hierarchy traversals.

**Cache key format**: `folder_perm:{userId}:{folderId}`  
**Cache value**: Role name (e.g., "Viewer", "Editor", "Admin") or "NONE"  
**Invalidation**: On permission change → invalidate all cache entries for the affected folder AND all descendant folders for the affected user/group members.

**Alternatives considered**:
- No caching: Rejected — hierarchy traversal on every request would exceed 200ms target at 10+ levels
- Longer TTL (30min): Rejected — permission changes need timely effect
- Event-based invalidation only: Considered but Redis TTL provides safety net

## 3. Navigation Tree Filtering (Middleware)

**Decision**: Spring Security interceptor that filters folder/file listings post-query

**Rationale**: Rather than modifying every repository query to include permission joins (fragile, complex), apply a filter layer that removes unauthorized items from query results before returning to client. This is a cross-cutting concern suited for interceptor/AOP pattern.

**Implementation approach**:
- `PermissionFilterService` takes a list of folders/files + userId → returns filtered list
- Uses batch permission resolution (single query for all folders in user's ancestry paths)
- Applied in controller layer or via response body advice

**Alternatives considered**:
- Database-level row security (MySQL views): Rejected — MySQL doesn't have native RLS, views would be complex and hard to maintain
- Query-time JOIN with permissions: Rejected — couples every query to permission logic, makes pagination difficult
- Client-side filtering: Rejected — security violation, exposes unauthorized data

## 4. Shared Links Architecture

**Decision**: Token-based links with stateless validation + database state check

**Rationale**: Each share link gets a cryptographically random token (UUID v4 or SecureRandom 32-byte hex). Accessing a link validates: token exists → not revoked → not expired → password matches (if set) → download allowed check.

**Token format**: 32-character hex string (SecureRandom)  
**URL format**: `/share/{token}` (public, no auth required)  
**Password storage**: bcrypt hash (matches existing user auth pattern)

**Alternatives considered**:
- JWT-based links (self-contained): Rejected — can't revoke without a blacklist, expiry changes require new link
- Signed URLs with HMAC: Rejected — need database state for revocation/stats anyway
- Short URLs with redirect: Unnecessary complexity

## 5. Signed URL Generation for File Access

**Decision**: Use MinIO pre-signed URLs for actual file download/preview from share links

**Rationale**: Share link token validates access rights. Once validated, generate a MinIO pre-signed GET URL (short-lived, 15min) for the actual file content. This keeps file serving off the application server.

**Flow**:
1. External user accesses `/share/{token}` 
2. Backend validates token, password, expiry
3. If download allowed → generate MinIO pre-signed URL (15min expiry)
4. Return file metadata + pre-signed URL to frontend/viewer

**Alternatives considered**:
- Proxy through backend: Rejected — high memory/bandwidth cost for large files
- Static public MinIO URLs: Rejected — no access control possible
- CloudFront signed cookies: Over-engineered for current deployment (Docker local)

## 6. Watermark Implementation

**Decision**: Server-side overlay via Python worker for downloaded files; CSS overlay for preview

**Rationale**: Preview watermarking is cosmetic (CSS overlay in frontend). Download watermarking requires actual file modification — delegate to existing Python worker infrastructure (Pillow for images, reportlab/PyPDF for PDFs).

**Alternatives considered**:
- Client-side only: Rejected for downloads — user gets unwatermarked file
- Real-time processing on every download: Acceptable for MVP, can add caching later
- Pre-generate watermarked versions: Rejected — storage cost, need different watermarks per link

## 7. Permission Levels Mapping to Existing Roles

**Decision**: Map to existing system roles (Admin, Editor, Viewer) in `roles` table

**Rationale**: The existing `roles` table with `role_permissions` mapping already defines what each role can do. Folder permissions reference role_id. No new permission levels needed — the three existing levels already match the spec requirements:
- Viewer: read/download (FILE_DOWNLOAD, view-folders)
- Editor: read/write/upload/delete (FILE_UPLOAD, FILE_DOWNLOAD, FILE_MANAGE, manage-folders)
- Admin: full control including permission management

**Alternatives considered**:
- Create new permission levels separate from roles: Rejected — duplicates existing role system
- Bitmask permissions: Rejected — harder to audit, less readable
