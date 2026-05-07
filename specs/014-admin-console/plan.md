# Implementation Plan: Admin Console

**Branch**: `014-admin-console` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/014-admin-console/spec.md`

## Summary

Build an admin console UI for managing users, roles, groups, storage quotas, organization policies, and system analytics. The backend APIs already exist for most CRUD operations (UserController, RoleController, GroupController, PolicyController, StorageQuota entity). This feature is **primarily frontend** — creating admin pages that consume existing APIs — plus a new admin analytics endpoint for org-wide metrics and trends, and a StorageQuota management API.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6  
**Storage**: MySQL 8.0 (port 3307, root/root), Redis 7 (port 6379)  
**Testing**: JUnit 5 + Mockito (backend), manual verification (frontend)  
**Target Platform**: Docker-deployed web application (nginx + Spring Boot)  
**Project Type**: Web application (full-stack)  
**Performance Goals**: Admin pages load < 2s with 10,000 users; API response < 200ms for standard queries  
**Constraints**: Org-scoped data isolation; admin-only access (manage-users/manage-roles permissions)  
**Scale/Scope**: ~8 new frontend pages, 2 new API endpoints, 1 new API client, 1 Flyway migration

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality — Modular, typed, clear interfaces | ✅ PASS | Admin pages follow existing component patterns; TypeScript strict typing |
| II. Testing — Unit + integration tests | ⚠️ PARTIAL | Backend new endpoints will have service-level tests; frontend manual verification (consistent with prior features) |
| III. UX Consistency — Design system, feedback states | ✅ PASS | Reuses existing table, form, dialog patterns from WorkspaceListPage, AuditPage |
| IV. Performance — <200ms API, caching | ✅ PASS | Analytics endpoint uses Redis caching; existing APIs already performant |
| V. Reliability — Retry, graceful errors | ✅ PASS | Frontend handles loading/error states; backend uses existing error handling |
| VI. Security — RBAC, audit logging | ✅ PASS | Permission-gated routes; all admin actions logged via existing audit infrastructure |
| VII. Data & AI Governance | N/A | No AI components in this feature |
| VIII. Observability — Logging | ✅ PASS | Uses existing logging infrastructure |
| IX. Developer Experience — Docs, structure | ✅ PASS | Follows established project conventions |
| X. Continuous Improvement | N/A | Standard feature delivery |

**Gate Result**: PASS — No violations.

## Project Structure

### Documentation (this feature)

```text
specs/014-admin-console/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── admin-api.md     # New admin analytics + storage quota APIs
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   └── AdminController.java          # NEW: Admin analytics endpoints
│   ├── dto/
│   │   ├── AdminAnalyticsResponse.java   # NEW: Analytics response DTO
│   │   ├── StorageQuotaRequest.java      # NEW: Quota update request
│   │   └── StorageQuotaResponse.java     # NEW: Quota response DTO
│   ├── service/
│   │   ├── AdminAnalyticsService.java    # NEW: Analytics aggregation
│   │   └── StorageQuotaService.java      # MODIFY: Add update methods
│   └── repository/
│       └── StorageQuotaRepository.java   # EXISTS: May need new queries
├── src/main/resources/db/migration/
│   └── V21__admin_analytics_views.sql    # NEW: Analytics helper views/indices
└── src/test/java/com/cms/
    └── service/
        └── AdminAnalyticsServiceTest.java # NEW

frontend/
├── src/
│   ├── api/
│   │   └── admin.ts                      # NEW: Admin analytics API client
│   ├── components/admin/
│   │   ├── AdminLayout.tsx               # NEW: Admin shell with sub-nav
│   │   ├── UserManagement.tsx            # NEW: User list + CRUD
│   │   ├── RoleManagement.tsx            # NEW: Role list + CRUD + permission matrix
│   │   ├── GroupManagement.tsx           # NEW: Group list + member management
│   │   ├── StoragePolicies.tsx           # NEW: Quota config + usage display
│   │   └── AnalyticsDashboard.tsx        # NEW: Org-wide metrics + charts
│   ├── pages/
│   │   └── AdminPage.tsx                 # NEW: Admin console entry page
│   └── App.tsx                           # MODIFY: Add /admin routes
```

**Structure Decision**: Web application with existing backend + frontend structure. Admin console adds new pages under `frontend/src/components/admin/` with a shared `AdminLayout` wrapper, following the pattern established by the workspace and audit pages.

## Complexity Tracking

No constitution violations — no entries needed.
