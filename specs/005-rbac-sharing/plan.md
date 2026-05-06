# Implementation Plan: RBAC & Sharing System

**Branch**: `005-rbac-sharing` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-rbac-sharing/spec.md`

## Summary

Build a granular permission system with folder-level RBAC (Viewer/Editor/Admin), permission inheritance with override support, navigation filtering middleware, and external sharing via secure links with password/expiry/watermark/download restrictions. Extends the existing `folder_permissions` table and adds a new `shared_links` table with access tracking.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7  
**Storage**: MySQL 8.0 (permissions, shared_links tables), Redis 7 (permission cache with 5min TTL), MinIO (file content)  
**Testing**: JUnit 5, Mockito, React Testing Library  
**Target Platform**: Docker containers (Linux)  
**Project Type**: Web service (multi-tenant CMS)  
**Performance Goals**: Permission resolution <200ms for 10+ level hierarchies, navigation filtering adds <100ms overhead  
**Constraints**: <200ms p95 for permission checks, cached permission invalidation on change, stateless share link validation  
**Scale/Scope**: 10k users, 100k folders, 50k share links per organization

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular services, typed models, clear interfaces (entities, DTOs, repos, services, controllers) |
| II. Testing Standards | ✅ PASS | Unit tests for service logic, integration tests for permission resolution |
| III. UX Consistency | ✅ PASS | Structured API responses, clear error messages, predictable navigation filtering |
| IV. Performance & Scalability | ✅ PASS | Redis caching for permission resolution, indexed queries, <200ms target |
| V. Reliability | ✅ PASS | Cache invalidation on permission change, idempotent permission assignments |
| VI. Security & Compliance | ✅ PASS | RBAC enforcement, bcrypt hashed link passwords, signed URLs, audit logging |
| IX. Developer Experience | ✅ PASS | Consistent project structure, extends existing patterns |

**Gate Result**: PASS — No violations.

## Project Structure

### Documentation (this feature)

```text
specs/005-rbac-sharing/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── permissions-api.md
│   └── sharing-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── entity/
│   │   ├── FolderPermission.java        # Extended: add is_inherited, override fields
│   │   ├── SharedLink.java              # NEW
│   │   └── SharedLinkAccess.java        # NEW
│   ├── repository/
│   │   ├── FolderPermissionRepository.java  # Extended: hierarchy queries
│   │   ├── SharedLinkRepository.java        # NEW
│   │   └── SharedLinkAccessRepository.java  # NEW
│   ├── service/
│   │   ├── FolderPermissionService.java     # Extended: inheritance resolution
│   │   ├── PermissionFilterService.java     # NEW: navigation tree filtering
│   │   └── SharedLinkService.java           # NEW
│   ├── controller/
│   │   ├── PermissionController.java        # NEW: permission CRUD endpoints
│   │   └── SharedLinkController.java        # NEW
│   ├── dto/
│   │   ├── permission/                      # NEW
│   │   └── sharing/                         # NEW
│   └── security/
│       └── PermissionInterceptor.java       # NEW: middleware for request filtering
├── src/main/resources/db/migration/
│   ├── V10__add_permission_inheritance.sql  # NEW: alter folder_permissions
│   └── V11__create_shared_links_tables.sql  # NEW

frontend/
├── src/
│   ├── api/
│   │   ├── permissions.ts               # NEW
│   │   └── sharing.ts                   # NEW
│   ├── components/
│   │   ├── PermissionDialog.tsx          # NEW
│   │   ├── ShareLinkDialog.tsx           # NEW
│   │   └── ShareLinkDashboard.tsx        # NEW
│   └── types/
│       ├── permission.ts                 # NEW
│       └── sharing.ts                    # NEW
```

**Structure Decision**: Extends existing web application pattern (backend/ + frontend/). New entities and services follow established conventions. Middleware implemented as Spring Security interceptor.

## Complexity Tracking

> No constitution violations. No justification required.
