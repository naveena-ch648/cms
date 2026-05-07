# Implementation Plan: Dashboard & Notifications

**Branch**: `012-dashboard-notifications` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-dashboard-notifications/spec.md`

## Summary

Enhanced dashboard with personalized content (recent files, shared items, activity feed, storage usage, pending approvals, alerts) and a Redis-cached notification system with unread badge, mark-as-read, bulk actions, and paginated notification history. Builds on existing NotificationService, StorageQuotaService, and SharedLinkRepository.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6
**Storage**: MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V19), Redis 7 (port 6379) for notification counts and dashboard caching
**Testing**: JUnit 5, Vitest
**Target Platform**: Web (desktop + responsive)
**Project Type**: Web application (multi-tenant CMS)
**Performance Goals**: Dashboard load < 2s, API response < 200ms for dashboard endpoints
**Constraints**: Independent widget loading, graceful degradation on individual widget failure
**Scale/Scope**: Multi-tenant, per-user dashboard data, Redis-cached aggregates

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular service/controller/component structure, strict typing |
| II. Testing Standards | ✅ PASS | Unit + integration tests planned |
| III. UX Consistency | ✅ PASS | Consistent widgets, loading/error/empty states, predictable navigation |
| IV. Performance & Scalability | ✅ PASS | Redis caching for dashboard data, < 200ms target |
| V. Reliability | ✅ PASS | Independent widget loading, graceful failure handling |
| VI. Security | ✅ PASS | User-scoped data, tenant isolation, RBAC |
| VII. Data Governance | ✅ PASS | Activity data derived from existing audit trail |
| VIII. Observability | ✅ PASS | Existing logging infrastructure used |
| IX. Developer Experience | ✅ PASS | Standard project patterns, clear API contracts |
| X. Continuous Improvement | ✅ PASS | Iterative enhancement of existing dashboard |

**Gate Result: PASS** — No violations found.

## Project Structure

### Documentation (this feature)

```text
specs/012-dashboard-notifications/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── dashboard-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── controller/
│   │   └── DashboardController.java
│   ├── dto/dashboard/
│   │   ├── RecentFileDto.java
│   │   ├── ActivityEventDto.java
│   │   ├── SharedItemDto.java
│   │   ├── DashboardSummaryDto.java
│   │   └── AlertDto.java
│   ├── entity/
│   │   ├── ActivityEvent.java
│   │   └── UserAlert.java
│   ├── repository/
│   │   ├── ActivityEventRepository.java
│   │   └── UserAlertRepository.java
│   └── service/
│       ├── DashboardService.java
│       └── AlertService.java
├── src/main/resources/db/migration/
│   └── V19__dashboard_activity_alerts.sql
└── src/test/

frontend/
├── src/
│   ├── api/
│   │   └── dashboard.ts
│   ├── components/
│   │   ├── dashboard/
│   │   │   ├── RecentFilesWidget.tsx
│   │   │   ├── ActivityFeedWidget.tsx
│   │   │   ├── StorageUsageWidget.tsx
│   │   │   ├── SharedItemsWidget.tsx
│   │   │   ├── AlertsWidget.tsx
│   │   │   └── NotificationPanel.tsx
│   │   └── NotificationBell.tsx
│   ├── pages/
│   │   └── DashboardPage.tsx (enhanced)
│   └── types/
│       └── dashboard.ts
```

## Complexity Tracking

No violations to justify — standard patterns used throughout.
