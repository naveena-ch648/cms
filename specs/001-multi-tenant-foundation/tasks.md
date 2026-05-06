# Tasks: Multi-Tenant Foundation

**Input**: Design documents from `/specs/001-multi-tenant-foundation/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/cms/`
- **Frontend**: `frontend/src/`
- **Migrations**: `backend/src/main/resources/db/migration/`
- **Docker**: `docker/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, Docker environment, and build tooling

- [X] T001 Create root project structure with backend/, frontend/, docker/, docs/ directories
- [X] T002 Initialize Spring Boot 3.x project with pom.xml including dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-data-redis, spring-boot-starter-validation, flyway-core, flyway-mysql, mysql-connector-j, jjwt-api, jjwt-impl, jjwt-jackson, lombok, spring-boot-starter-test in backend/pom.xml
- [X] T003 [P] Initialize React 18 + TypeScript project with Vite, React Router, Axios in frontend/package.json and frontend/vite.config.ts
- [X] T004 [P] Create Docker Compose configuration with MySQL (port 3307), Redis (port 6379), backend (port 8080), frontend (port 3000) in docker/docker-compose.yml
- [X] T005 [P] Create MySQL init script with database and user setup in docker/mysql/init.sql
- [X] T006 [P] Create backend application.yml with MySQL, Redis, JWT, Flyway, and CORS configuration in backend/src/main/resources/application.yml
- [X] T007 Create CmsApplication.java Spring Boot entry point in backend/src/main/java/com/cms/CmsApplication.java

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T008 Create Flyway migration V1__create_foundation_tables.sql with all tables: organizations, users, roles, permissions, role_permissions, groups, user_groups, workspaces, user_organization_roles, user_workspace_roles, group_workspace_roles, audit_events in backend/src/main/resources/db/migration/V1__create_foundation_tables.sql
- [X] T009 Create Flyway migration V2__seed_permissions.sql to insert system permissions (view-workspace, manage-workspace, view-users, manage-users, view-roles, manage-roles, view-groups, manage-groups, manage-policies, view-audit-log) in backend/src/main/resources/db/migration/V2__seed_permissions.sql
- [X] T010 [P] Create Organization JPA entity in backend/src/main/java/com/cms/entity/Organization.java
- [X] T011 [P] Create User JPA entity in backend/src/main/java/com/cms/entity/User.java
- [X] T012 [P] Create Role JPA entity with self-referencing parentRole in backend/src/main/java/com/cms/entity/Role.java
- [X] T013 [P] Create Permission JPA entity in backend/src/main/java/com/cms/entity/Permission.java
- [X] T014 [P] Create Group JPA entity in backend/src/main/java/com/cms/entity/Group.java
- [X] T015 [P] Create Workspace JPA entity in backend/src/main/java/com/cms/entity/Workspace.java
- [X] T016 [P] Create AuditEvent JPA entity in backend/src/main/java/com/cms/entity/AuditEvent.java
- [X] T017 [P] Create UserOrganizationRole JPA entity in backend/src/main/java/com/cms/entity/UserOrganizationRole.java
- [X] T018 [P] Create UserWorkspaceRole JPA entity in backend/src/main/java/com/cms/entity/UserWorkspaceRole.java
- [X] T019 [P] Create GroupWorkspaceRole JPA entity in backend/src/main/java/com/cms/entity/GroupWorkspaceRole.java
- [X] T020 Create API response envelope classes: ApiResponse, ApiError, PagedMeta in backend/src/main/java/com/cms/dto/ApiResponse.java
- [X] T021 [P] Create GlobalExceptionHandler with @ControllerAdvice mapping exceptions to standardized envelope in backend/src/main/java/com/cms/exception/GlobalExceptionHandler.java
- [X] T022 [P] Create custom exception classes: ResourceNotFoundException, DuplicateResourceException, BusinessRuleException, AuthenticationException in backend/src/main/java/com/cms/exception/
- [X] T023 Create TenantContext ThreadLocal holder for organization_id in backend/src/main/java/com/cms/middleware/TenantContext.java
- [X] T024 Create TenantFilter servlet filter that extracts organizationId from JWT and sets TenantContext in backend/src/main/java/com/cms/middleware/TenantFilter.java
- [X] T025 Create base repositories: OrganizationRepository, UserRepository, RoleRepository, PermissionRepository, GroupRepository, WorkspaceRepository, AuditEventRepository in backend/src/main/java/com/cms/repository/
- [X] T026 [P] Create AuditService for logging auth events, permission denials, and CRUD operations in backend/src/main/java/com/cms/service/AuditService.java
- [X] T027 [P] Create RedisConfig for Redis connection and cache manager in backend/src/main/java/com/cms/config/RedisConfig.java
- [X] T028 [P] Create CorsConfig for CORS settings allowing frontend origin in backend/src/main/java/com/cms/config/CorsConfig.java
- [X] T029 [P] Create TypeScript type definitions for API response envelope, User, Organization, Role, Permission, Group, Workspace in frontend/src/types/api.ts and frontend/src/types/models.ts
- [X] T030 [P] Create Axios API client with interceptors for auth token injection, response unwrapping, and error handling in frontend/src/api/client.ts

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Organization Onboarding & User Registration (Priority: P1) 🎯 MVP

**Goal**: Platform admin creates organizations; org admin registers users who can sign in

**Independent Test**: Create an organization, add a user, verify the user can sign in and view org settings

### Implementation for User Story 1

- [X] T031 [US1] Create OrganizationService with create, getById, update, deactivate, and policy merge logic in backend/src/main/java/com/cms/service/OrganizationService.java
- [X] T032 [US1] Create PolicyService with default policy definitions and effective policy merge in backend/src/main/java/com/cms/service/PolicyService.java
- [X] T033 [US1] Create request/response DTOs: CreateOrganizationRequest, UpdateOrganizationRequest, UpdatePoliciesRequest, OrganizationResponse in backend/src/main/java/com/cms/dto/organization/
- [X] T034 [US1] Create OrganizationController with POST/GET/PUT /api/v1/organizations and PUT /api/v1/organizations/{orgId}/policies endpoints in backend/src/main/java/com/cms/controller/OrganizationController.java
- [X] T035 [US1] Create UserService with register, getById, list (paginated), update, changePassword, deactivate, and password validation against org policy in backend/src/main/java/com/cms/service/UserService.java
- [X] T036 [US1] Create request/response DTOs: CreateUserRequest, UpdateUserRequest, ChangePasswordRequest, UserResponse in backend/src/main/java/com/cms/dto/user/
- [X] T037 [US1] Create UserController with POST/GET/PUT/DELETE /api/v1/users endpoints including pagination and filtering in backend/src/main/java/com/cms/controller/UserController.java
- [X] T038 [US1] Create Flyway migration V3__seed_default_org_and_admin.sql to bootstrap a platform admin organization and super admin user in backend/src/main/resources/db/migration/V3__seed_default_org_and_admin.sql
- [X] T039 [US1] Add last-admin protection in UserService: prevent removing/changing role of last admin in an organization

**Checkpoint**: Organization CRUD and user registration working. Can create org + register users via API.

---

## Phase 4: User Story 2 — Authentication & Session Management (Priority: P1)

**Goal**: Users sign in with email/password, receive JWT tokens, access protected resources, sign out

**Independent Test**: Sign in, access protected endpoint, sign out, verify token invalidated

### Implementation for User Story 2

- [X] T040 [US2] Create JwtProvider service for generating access tokens (15 min) and refresh tokens (7 days) with claims (sub, org, roles, jti) in backend/src/main/java/com/cms/security/JwtProvider.java
- [X] T041 [US2] Create JwtAuthenticationFilter that validates Bearer token, checks Redis blocklist, and sets SecurityContext in backend/src/main/java/com/cms/security/JwtAuthenticationFilter.java
- [X] T042 [US2] Create CustomUserDetailsService implementing UserDetailsService loading user by email+org in backend/src/main/java/com/cms/security/CustomUserDetailsService.java
- [X] T043 [US2] Create SecurityConfig with Spring Security filter chain, public/protected endpoint rules, and JWT filter registration in backend/src/main/java/com/cms/config/SecurityConfig.java
- [X] T044 [US2] Create AuthService with login (validate credentials, issue tokens, log event), refresh (rotate tokens), and logout (blocklist token in Redis) in backend/src/main/java/com/cms/service/AuthService.java
- [X] T045 [US2] Create request/response DTOs: LoginRequest, TokenResponse, RefreshRequest in backend/src/main/java/com/cms/dto/auth/
- [X] T046 [US2] Create AuthController with POST /api/v1/auth/login, POST /api/v1/auth/refresh, POST /api/v1/auth/logout, GET /api/v1/auth/me in backend/src/main/java/com/cms/controller/AuthController.java
- [X] T047 [US2] Implement account lockout logic in AuthService: track failed attempts in Redis, lock account after maxFailedLoginAttempts per org policy
- [X] T048 [P] [US2] Create AuthContext React context with login, logout, token storage, and auto-refresh in frontend/src/contexts/AuthContext.tsx
- [X] T049 [P] [US2] Create auth API service with login, logout, refresh, getMe functions in frontend/src/services/authService.ts
- [X] T050 [US2] Create Login page with email/password form, error display, and loading state in frontend/src/pages/Login.tsx
- [X] T051 [US2] Create ProtectedRoute component that redirects unauthenticated users to login in frontend/src/components/ProtectedRoute.tsx
- [X] T052 [US2] Create App router with public routes (login) and protected routes (dashboard) in frontend/src/App.tsx

**Checkpoint**: Full auth flow working — login, token validation, logout, account lockout

---

## Phase 5: User Story 3 — Role-Based Access Control with Inheritance (Priority: P1)

**Goal**: Org admin defines roles with permissions and inheritance; system enforces RBAC on every action

**Independent Test**: Create role hierarchy (Viewer→Editor→Admin), assign roles, verify permission enforcement

### Implementation for User Story 3

- [X] T053 [US3] Create PermissionService with getEffectivePermissions(roleId) using recursive parent chain walk and Redis caching in backend/src/main/java/com/cms/service/PermissionService.java
- [X] T054 [US3] Create RoleService with create, update, delete (with child re-linking), list, and circular inheritance check in backend/src/main/java/com/cms/service/RoleService.java
- [X] T055 [US3] Create request/response DTOs: CreateRoleRequest, UpdateRoleRequest, RoleResponse, PermissionResponse in backend/src/main/java/com/cms/dto/role/
- [X] T056 [US3] Create RoleController with POST/GET/PUT/DELETE /api/v1/roles and GET /api/v1/permissions endpoints in backend/src/main/java/com/cms/controller/RoleController.java
- [X] T057 [US3] Create CustomPermissionEvaluator implementing PermissionEvaluator for @PreAuthorize checks in backend/src/main/java/com/cms/security/CustomPermissionEvaluator.java
- [X] T058 [US3] Add @PreAuthorize annotations to OrganizationController, UserController, and RoleController methods enforcing required permissions
- [X] T059 [US3] Implement cache invalidation in PermissionService: clear Redis cache on role permission/hierarchy changes
- [X] T060 [US3] Create default role seeding logic in OrganizationService: create Viewer, Editor, Admin roles when a new organization is created

**Checkpoint**: RBAC fully enforced — roles with inheritance, permission checks on all endpoints

---

## Phase 6: User Story 4 — Workspace Management & Scoped Membership (Priority: P2)

**Goal**: Org admin creates workspaces; users assigned with workspace-scoped roles; users see only their workspaces

**Independent Test**: Create workspaces, assign users with different roles, verify scoped access

### Implementation for User Story 4

- [X] T061 [US4] Create WorkspaceService with create (enforcing org policy maxWorkspaces), update, delete (soft), list (filtered by user membership), addMember, removeMember, updateMemberRole in backend/src/main/java/com/cms/service/WorkspaceService.java
- [X] T062 [US4] Create request/response DTOs: CreateWorkspaceRequest, UpdateWorkspaceRequest, AddMemberRequest, WorkspaceResponse, WorkspaceMemberResponse in backend/src/main/java/com/cms/dto/workspace/
- [X] T063 [US4] Create WorkspaceController with POST/GET/PUT/DELETE /api/v1/workspaces, POST/PUT/DELETE /api/v1/workspaces/{id}/members endpoints in backend/src/main/java/com/cms/controller/WorkspaceController.java
- [X] T064 [US4] Update PermissionService.getEffectiveRole(userId, workspaceId) to resolve workspace-scoped role: direct > group > org-level in backend/src/main/java/com/cms/service/PermissionService.java
- [X] T065 [P] [US4] Create workspace API service with CRUD and member management in frontend/src/services/workspaceService.ts
- [X] T066 [P] [US4] Create WorkspaceContext for current workspace state in frontend/src/contexts/WorkspaceContext.tsx
- [X] T067 [US4] Create Workspaces list page showing user's accessible workspaces with role badges in frontend/src/pages/Workspaces.tsx
- [X] T068 [US4] Create Workspace detail page with member list and management in frontend/src/pages/WorkspaceDetail.tsx
- [X] T069 [US4] Create usePermission hook for checking current user's permissions in a workspace in frontend/src/hooks/usePermission.ts

**Checkpoint**: Workspaces working — CRUD, membership, scoped permissions, filtered visibility

---

## Phase 7: User Story 5 — Group Management & Bulk Role Assignment (Priority: P3)

**Goal**: Org admin creates groups, assigns users; groups get workspace roles propagating to all members

**Independent Test**: Create group, assign workspace role, add user, verify inherited access

### Implementation for User Story 5

- [X] T070 [US5] Create GroupService with create, update, delete, list, addMembers, removeMember, assignWorkspaceRole, removeWorkspaceRole in backend/src/main/java/com/cms/service/GroupService.java
- [X] T071 [US5] Create request/response DTOs: CreateGroupRequest, UpdateGroupRequest, AddGroupMembersRequest, GroupResponse, GroupWorkspaceRoleResponse in backend/src/main/java/com/cms/dto/group/
- [X] T072 [US5] Create GroupController with POST/GET/PUT/DELETE /api/v1/groups, POST/DELETE /api/v1/groups/{id}/members, PUT/DELETE /api/v1/groups/{id}/workspaces/{wsId}/role in backend/src/main/java/com/cms/controller/GroupController.java
- [X] T073 [US5] Update PermissionService to include group_workspace_roles in effective permission resolution and invalidate cache on group membership changes
- [X] T074 [P] [US5] Create group API service with CRUD, membership, and workspace role management in frontend/src/services/groupService.ts
- [X] T075 [US5] Create Groups list page with member count and workspace role summary in frontend/src/pages/Groups.tsx
- [X] T076 [US5] Create Group detail page with member management and workspace role assignment in frontend/src/pages/GroupDetail.tsx

**Checkpoint**: Groups working — CRUD, membership, workspace role propagation, effective permission union

---

## Phase 8: User Story 6 — Organization-Level Policies (Priority: P3)

**Goal**: Org admin configures policies (password, session, workspace limits); system enforces them

**Independent Test**: Set password min length to 12, attempt user creation with short password, verify rejection

### Implementation for User Story 6

- [X] T077 [US6] Add policy enforcement to UserService.register and changePassword: validate password against org policy rules
- [X] T078 [US6] Add session timeout enforcement in JwtProvider: use org policy sessionTimeoutMinutes for access token TTL
- [X] T079 [US6] Add workspace limit enforcement in WorkspaceService.create: check org policy maxWorkspaces
- [X] T080 [P] [US6] Create organization API service with CRUD and policy management in frontend/src/services/organizationService.ts
- [X] T081 [US6] Create Organization Settings page with policy editor form in frontend/src/pages/OrgSettings.tsx
- [X] T082 [US6] Add policy validation feedback in user registration form in frontend/src/pages/Users.tsx

**Checkpoint**: Policies enforced — password complexity, session timeout, workspace limits

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Admin UI, navigation, and platform-wide improvements

- [X] T083 [P] Create AppLayout with sidebar navigation (Dashboard, Workspaces, Users, Groups, Roles, Settings) and header with user menu in frontend/src/layouts/AppLayout.tsx
- [X] T084 [P] Create Dashboard page with summary cards (workspace count, user count, recent activity) in frontend/src/pages/Dashboard.tsx
- [X] T085 [P] Create Users management page with table, search, filters, pagination, and role assignment in frontend/src/pages/Users.tsx
- [X] T086 [P] Create Roles management page with role list, inheritance visualization, and permission editor in frontend/src/pages/Roles.tsx
- [X] T087 Create shared UI components: DataTable, Modal, FormField, LoadingSpinner, ErrorAlert, Badge, Pagination in frontend/src/components/
- [X] T088 [P] Create user and role API services in frontend/src/services/userService.ts and frontend/src/services/roleService.ts
- [X] T089 Update App.tsx router with all page routes: dashboard, workspaces, users, groups, roles, settings in frontend/src/App.tsx
- [X] T090 Run quickstart.md validation: verify Docker Compose startup, API endpoint tests, frontend connectivity

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational phase completion
- **US2 (Phase 4)**: Depends on US1 (needs users to authenticate)
- **US3 (Phase 5)**: Depends on US2 (needs auth to test permission enforcement)
- **US4 (Phase 6)**: Depends on US3 (needs RBAC for workspace access control)
- **US5 (Phase 7)**: Depends on US4 (needs workspaces for group role assignment)
- **US6 (Phase 8)**: Depends on US1 (needs org + users for policy enforcement)
- **Polish (Phase 9)**: Can start after US2 for navigation; full polish after US5

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — No dependencies on other stories
- **US2 (P1)**: Depends on US1 — Needs registered users to authenticate
- **US3 (P1)**: Depends on US2 — Needs auth context for permission enforcement
- **US4 (P2)**: Depends on US3 — Needs RBAC framework for workspace access
- **US5 (P3)**: Depends on US4 — Needs workspaces for group role assignment
- **US6 (P3)**: Can start after US1 — Policy enforcement independent of RBAC/workspaces

### Within Each User Story

- Models/entities before services
- Services before controllers
- Backend before frontend (API must exist for frontend to consume)
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks T003–T006 marked [P] can run in parallel
- All entity tasks T010–T019 marked [P] can run in parallel
- Foundation DTOs, exception handler, Redis config, CORS (T021, T022, T27, T28) in parallel
- Frontend type definitions and API client (T029, T030) in parallel with backend entities
- Within US2: AuthContext + auth service (T048, T049) in parallel
- Within US4: workspace service + context (T065, T066) in parallel
- Within US5: group service (T074) in parallel with backend
- Within US6: org service (T080) in parallel with backend policy enforcement
- All Polish tasks T083–T088 marked [P] can run in parallel

---

## Parallel Example: Phase 2 Entities

```bash
# Launch all entity creations in parallel:
Task: "Create Organization JPA entity in backend/.../entity/Organization.java"
Task: "Create User JPA entity in backend/.../entity/User.java"
Task: "Create Role JPA entity in backend/.../entity/Role.java"
Task: "Create Permission JPA entity in backend/.../entity/Permission.java"
Task: "Create Group JPA entity in backend/.../entity/Group.java"
Task: "Create Workspace JPA entity in backend/.../entity/Workspace.java"
Task: "Create AuditEvent JPA entity in backend/.../entity/AuditEvent.java"
Task: "Create UserOrganizationRole JPA entity in backend/.../entity/UserOrganizationRole.java"
Task: "Create UserWorkspaceRole JPA entity in backend/.../entity/UserWorkspaceRole.java"
Task: "Create GroupWorkspaceRole JPA entity in backend/.../entity/GroupWorkspaceRole.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 + 3)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US1 — Organization + User Registration
4. Complete Phase 4: US2 — Authentication
5. Complete Phase 5: US3 — RBAC
6. **STOP and VALIDATE**: Full auth + RBAC working end-to-end
7. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Org + User management working (API only)
3. Add US2 → Full auth flow → Deploy/Demo (MVP!)
4. Add US3 → RBAC enforcement → Secure MVP
5. Add US4 → Workspaces with scoped access → Deploy/Demo
6. Add US5 → Groups for bulk management → Deploy/Demo
7. Add US6 → Policy governance → Deploy/Demo
8. Polish → Full admin UI → Deploy/Demo

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 → US2 → US3 (sequential, P1 stories)
   - Developer B: Frontend type defs + API client → Polish UI components
3. After US3 complete:
   - Developer A: US4 → US5
   - Developer B: US6 + Polish pages
4. Stories integrate naturally via shared entities and services

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
