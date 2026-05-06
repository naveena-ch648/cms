# Research: Multi-Tenant Foundation

**Feature**: 001-multi-tenant-foundation
**Date**: 2026-05-05
**Purpose**: Resolve technology decisions and best practices for multi-tenant CMS foundation

---

## 1. Multi-Tenant Isolation Strategy

### Decision: Shared Database with Discriminator Column (tenant_id)

**Rationale**: All tenants share a single MySQL database with `organization_id` as a discriminator column on every tenant-scoped table. This is the most cost-effective and operationally simple approach for the initial scale (100+ orgs, 10K+ users). Tenant isolation is enforced at the application layer via middleware that injects the tenant context from the authenticated JWT into every query.

**Alternatives considered**:
- **Database-per-tenant**: Maximum isolation but operationally expensive (schema migrations, connection pools, backups per tenant). Rejected for initial phase—premature for scale target.
- **Schema-per-tenant**: Better isolation than shared DB, but MySQL has limited schema-level isolation tooling. Adds migration complexity. Rejected.

**Implementation approach**:
- `TenantFilter` (Servlet filter) extracts `organizationId` from JWT claims and stores it in a `ThreadLocal` context.
- Spring Data JPA `@Where` annotations or Hibernate filters automatically append `organization_id = :tenantId` to queries.
- All tenant-scoped repositories extend a `TenantAwareRepository` base that enforces the filter.
- Integration tests verify that cross-tenant data access is impossible.

---

## 2. JWT Authentication & Session Management

### Decision: Stateless JWT with Redis-Backed Blocklist

**Rationale**: JWT tokens provide stateless authentication, enabling horizontal scaling without sticky sessions. A Redis blocklist handles explicit sign-out and token revocation. Access tokens are short-lived (15 min); refresh tokens are longer-lived (7 days) and stored in Redis for rotation.

**Alternatives considered**:
- **Opaque tokens with Redis session store**: Simpler revocation but requires Redis lookup on every request. Rejected—adds latency and single point of failure for all auth.
- **JWT-only (no blocklist)**: Simpler but cannot support immediate sign-out/revocation. Rejected—violates security requirements.

**Implementation approach**:
- `JwtProvider` service: generates access token (15 min TTL) + refresh token (7 days TTL).
- Access token claims: `sub` (userId), `org` (organizationId), `roles` (list of role names), `exp`, `iat`, `jti` (unique token ID).
- `JwtAuthenticationFilter` validates token on every request; checks Redis blocklist by `jti`.
- Sign-out adds the token's `jti` to Redis with TTL matching remaining token lifetime.
- Refresh endpoint issues new access token; old refresh token is rotated (one-time use).
- Password hashing: bcrypt with cost factor 12 (industry standard, ~250ms per hash).

---

## 3. RBAC with Role Inheritance

### Decision: Adjacency List Model with Recursive Permission Resolution

**Rationale**: Roles reference a `parent_role_id` forming a tree. Effective permissions are computed by walking the inheritance chain upward, collecting all permissions. This is cached in Redis per role to avoid repeated DB traversal.

**Alternatives considered**:
- **Closure table**: Precomputed ancestor-descendant pairs. Faster reads but more complex writes and migrations. Rejected—role hierarchies are shallow (typically 3-5 levels), so adjacency list with caching is sufficient.
- **Flat permission assignment (no inheritance)**: Simpler but duplicates permissions across roles and makes bulk changes error-prone. Rejected—violates constitution principle of composability.

**Implementation approach**:
- `Role` entity: `id`, `name`, `organizationId`, `parentRoleId` (nullable, self-referencing FK).
- `Permission` entity: `id`, `name`, `description`.
- `role_permissions` join table: many-to-many between Role and Permission (direct grants only).
- `PermissionService.getEffectivePermissions(roleId)`: walks parent chain, unions all permissions, caches result in Redis with key `permissions:role:{roleId}`.
- Cache invalidated when role permissions or hierarchy change (publish Redis event).
- `@PreAuthorize` annotations on controllers use a custom `PermissionEvaluator` that checks effective permissions.

---

## 4. Workspace-Scoped Permissions

### Decision: Dual-Scope Role Assignment (Organization + Workspace)

**Rationale**: Users have an organization-level role (default for all contexts) and optional workspace-level role overrides. The effective role in a workspace is the workspace-specific assignment if present; otherwise, the organization-level role applies.

**Alternatives considered**:
- **Workspace-only roles (no org-level default)**: Requires explicit assignment to every workspace. Rejected—poor UX for new users.
- **Merged permissions (union of org + workspace)**: Complex and unpredictable. Rejected—workspace role should be a clear override.

**Implementation approach**:
- `user_organization_roles` table: `userId`, `organizationId`, `roleId` (org-level default).
- `user_workspace_roles` table: `userId`, `workspaceId`, `roleId` (workspace override).
- `group_workspace_roles` table: `groupId`, `workspaceId`, `roleId`.
- Permission resolution order: direct workspace role > group workspace role > org-level role. Highest privilege wins when multiple sources grant access.
- `PermissionService.getEffectiveRole(userId, workspaceId)`: checks workspace direct assignment, then group assignments, then org-level default.

---

## 5. Group-Based Permission Propagation

### Decision: Indirect Mapping via Group-Workspace-Role Join

**Rationale**: Groups are assigned roles on workspaces. When checking permissions, the system queries both direct user assignments and group memberships. No permission records are duplicated per user—everything is resolved at query time (with caching).

**Implementation approach**:
- `user_groups` table: `userId`, `groupId`.
- When resolving workspace access, query: direct `user_workspace_roles` UNION `group_workspace_roles` where user is member.
- Redis cache key: `permissions:user:{userId}:workspace:{workspaceId}` → cached effective permissions.
- Cache invalidated when: user added/removed from group, group role changed, user workspace role changed.

---

## 6. Organization Policies

### Decision: JSON Policy Column with Typed Defaults

**Rationale**: Policies are stored as a JSON column on the `organizations` table, merged at runtime with system-wide defaults. This allows flexible policy addition without schema changes.

**Alternatives considered**:
- **Separate policies table (key-value)**: More normalized but requires multiple queries for policy checks. Rejected for simplicity.
- **Dedicated columns per policy**: Type-safe but requires migration for every new policy. Rejected—too rigid.

**Implementation approach**:
- `Organization.policies` column: JSON containing overrides only.
- Default policies defined in application config: `{ "passwordMinLength": 8, "sessionTimeoutMinutes": 30, "maxWorkspaces": 50, "requireUppercase": true, "requireNumber": true }`.
- `PolicyService.getEffectivePolicy(organizationId)`: merges org overrides over defaults.
- Policy enforcement points: `UserService` (password validation), `JwtProvider` (session timeout), `WorkspaceService` (workspace limit).

---

## 7. API Response Envelope

### Decision: Standardized JSON Envelope

**Rationale**: Constitution principle III requires structured, predictable responses. All API responses follow a consistent envelope format.

**Implementation approach**:
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": {
    "timestamp": "2026-05-05T12:00:00Z",
    "requestId": "uuid",
    "pagination": { "page": 1, "size": 20, "totalElements": 100, "totalPages": 5 }
  }
}
```
- Error responses:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Invalid email or password",
    "details": []
  },
  "meta": { "timestamp": "...", "requestId": "..." }
}
```
- `GlobalExceptionHandler` with `@ControllerAdvice` ensures all exceptions map to this format.

---

## 8. Database Migration Strategy

### Decision: Flyway with Versioned SQL Scripts

**Rationale**: Flyway provides reliable, versioned schema migrations that integrate with Spring Boot. SQL scripts give full control over MySQL-specific syntax (indexes, constraints, character sets).

**Implementation approach**:
- Migration files in `src/main/resources/db/migration/` named `V{version}__{description}.sql`.
- Initial migration creates all foundation tables with proper indexes.
- `spring.flyway.enabled=true` in application config.
- All tables use `utf8mb4` character set for full Unicode support.
- Audit columns (`created_at`, `updated_at`) on every table, managed by JPA `@PrePersist`/`@PreUpdate`.

---

## 9. Local Development Environment

### Decision: Docker Compose with Hot Reload

**Rationale**: Constitution principle IX requires single-command local setup. Docker Compose orchestrates MySQL, Redis, backend, and frontend.

**Implementation approach**:
- `docker-compose.yml` defines: MySQL (port 3307), Redis (port 6379), backend (port 8080), frontend (port 3000).
- Backend uses Spring Boot DevTools for hot reload.
- Frontend uses Vite dev server with HMR.
- `.env.example` for environment variables.
- `make dev` or `docker compose up` as single entry command.
