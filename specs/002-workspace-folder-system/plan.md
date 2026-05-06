# Implementation Plan: Workspace Folder System

**Branch**: `002-workspace-folder-system` | **Date**: 2026-05-05 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-workspace-folder-system/spec.md`

## Summary

Build a hierarchical folder system within workspaces using adjacency-list pattern (parent_id self-reference) in MySQL. Backend provides folder CRUD, move, copy, and permission-inheritance APIs via Spring Boot. Frontend renders an interactive folder tree with drag-drop, breadcrumbs, favorites, and recent items using React. Folder tree structure is cached in Redis for performance.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6
**Storage**: MySQL 8.0 (port 3307) with Flyway migrations, Redis 7 (port 6379) for caching
**Testing**: Maven Surefire (JUnit 5), Vitest (frontend)
**Target Platform**: Docker Compose (backend :8080, frontend :3000, MySQL :3307, Redis :6379)
**Project Type**: Web application (full-stack SPA + REST API)
**Performance Goals**: Folder tree loads <2s for 500 folders; drag-drop move <1s; API p95 <200ms
**Constraints**: Multi-tenant isolation via organization_id; folder depth practical max 20; lazy-load children for 100+ items
**Scale/Scope**: Extends existing 11 entities with 4 new tables; ~15 new backend files, ~10 new frontend files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality — Modular, typed, clear interfaces | ✅ PASS | Follows existing pattern: entity/repo/service/controller layers with DTOs |
| II. Testing Standards — Unit + integration tests, 80% coverage | ✅ PASS | Will include unit tests for service logic, controller tests |
| III. UX Consistency — Predictable navigation, structured responses | ✅ PASS | Uses existing ApiResponse envelope; breadcrumbs provide predictable navigation |
| IV. Performance — Caching, optimized queries, <200ms API | ✅ PASS | Redis caching for folder trees; indexed queries; lazy-loading for large folders |
| V. Reliability — Retry, graceful failure | ✅ PASS | Circular move detection; cascade delete safety; optimistic concurrency on moves |
| VI. Security — RBAC, audit logging | ✅ PASS | Folder-level permissions with inheritance; all operations audit-logged |
| IX. Developer Experience — Consistent structure, local dev | ✅ PASS | Same Docker Compose setup; Flyway migrations; follows existing patterns exactly |

**Gate Result**: ✅ ALL PASS — No violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/002-workspace-folder-system/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── entity/
│   │   ├── Folder.java
│   │   ├── FolderPermission.java
│   │   ├── FolderFavorite.java
│   │   └── FolderRecent.java
│   ├── repository/
│   │   ├── FolderRepository.java
│   │   ├── FolderPermissionRepository.java
│   │   ├── FolderFavoriteRepository.java
│   │   └── FolderRecentRepository.java
│   ├── service/
│   │   ├── FolderService.java
│   │   └── FolderPermissionService.java
│   ├── controller/
│   │   └── FolderController.java
│   └── dto/folder/
│       ├── CreateFolderRequest.java
│       ├── UpdateFolderRequest.java
│       ├── MoveFolderRequest.java
│       ├── FolderResponse.java
│       ├── FolderTreeResponse.java
│       └── FolderPermissionRequest.java
├── src/main/resources/db/migration/
│   └── V4__create_folder_tables.sql
└── src/test/java/com/cms/

frontend/
├── src/
│   ├── api/
│   │   └── folders.ts
│   ├── components/
│   │   ├── FolderTree.tsx
│   │   ├── FolderTreeNode.tsx
│   │   ├── Breadcrumbs.tsx
│   │   ├── FolderSidebar.tsx
│   │   └── FolderContextMenu.tsx
│   ├── pages/
│   │   └── WorkspacePage.tsx
│   └── types/
│       └── folder.ts
```

**Structure Decision**: Extends existing web application structure (backend/ + frontend/). New files follow the established entity→repository→service→controller pattern on backend, and api→types→components→pages pattern on frontend.

## Complexity Tracking

> No constitution violations — table not needed.
