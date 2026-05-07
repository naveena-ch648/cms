# Implementation Plan: Audit Logging & Compliance

**Branch**: `013-audit-compliance` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-audit-compliance/spec.md`

## Summary

Comprehensive audit logging system that captures all user and system actions into a tamper-evident, searchable log. Extends the existing AuditEvent entity and AuditService with OpenSearch indexing for full-text search, compliance report generation (CSV export), configurable alert rules for suspicious patterns, and an admin UI for browsing/filtering/exporting audit data. Builds on existing OpenSearch infrastructure (feature 008), NotificationService (feature 012), and the basic AuditEvent/AuditService already in place.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, OpenSearch Java Client 2.x, React 18, React Router 6.28, Axios 1.7.7, Vite 6
**Storage**: MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V20), Redis 7 (port 6379) for event buffering and alert rate tracking, OpenSearch 2.x for audit event indexing
**Testing**: JUnit 5, Vitest
**Target Platform**: Web (desktop + responsive)
**Project Type**: Web application (multi-tenant CMS)
**Performance Goals**: Event capture < 5s latency, search response < 500ms, 10,000 events/minute throughput, report generation < 60s for 30-day range
**Constraints**: Append-only (no update/delete), async event capture (non-blocking), tenant-isolated visibility, < 200ms for standard API calls
**Scale/Scope**: Multi-tenant, organization-scoped audit data, 365-day retention in searchable index

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Code Quality | ✅ PASS | Modular service/controller structure, strict typing, clear interfaces |
| II. Testing Standards | ✅ PASS | Unit + integration tests, mock OpenSearch for tests |
| III. UX Consistency | ✅ PASS | Consistent admin UI patterns, loading/error states, predictable navigation |
| IV. Performance & Scalability | ✅ PASS | Async event capture, OpenSearch for search (< 500ms), Redis buffering |
| V. Reliability | ✅ PASS | Event queue with retry on storage failure, graceful degradation |
| VI. Security | ✅ PASS | Immutable audit entries, RBAC-restricted access, organization isolation |
| VII. Data Governance | ✅ PASS | Append-only data model, 365-day retention, full traceability |
| VIII. Observability | ✅ PASS | Audit system IS observability — centralized logging of all actions |
| IX. Developer Experience | ✅ PASS | Standard project patterns, AOP-based event capture minimizes manual instrumentation |
| X. Continuous Improvement | ✅ PASS | Configurable alert rules allow iterative security refinement |

**Gate Result: PASS** — No violations found.

## Project Structure

### Documentation (this feature)

```text
specs/013-audit-compliance/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── audit-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── aspect/
│   │   └── AuditAspect.java              # AOP interceptor for automatic event capture
│   ├── annotation/
│   │   └── Audited.java                   # Method-level annotation for audit capture
│   ├── controller/
│   │   └── AuditController.java           # REST endpoints for audit log browsing/export
│   ├── dto/audit/
│   │   ├── AuditEventDto.java
│   │   ├── AuditEventDetailDto.java
│   │   ├── AuditSearchRequest.java
│   │   ├── AuditSearchResponse.java
│   │   ├── ComplianceReportRequest.java
│   │   ├── ComplianceReportDto.java
│   │   ├── AuditAlertRuleDto.java
│   │   └── AuditStatsDto.java
│   ├── entity/
│   │   ├── AuditEvent.java                # Enhanced (add user-agent, outcome, category)
│   │   ├── ComplianceReport.java          # NEW
│   │   ├── AuditAlertRule.java            # NEW
│   │   └── AuditAlertInstance.java        # NEW
│   ├── repository/
│   │   ├── AuditEventRepository.java      # Enhanced
│   │   ├── ComplianceReportRepository.java
│   │   ├── AuditAlertRuleRepository.java
│   │   └── AuditAlertInstanceRepository.java
│   └── service/
│       ├── AuditService.java              # Enhanced (async, OpenSearch indexing, buffering)
│       ├── AuditSearchService.java        # NEW - OpenSearch query builder for audit
│       ├── ComplianceReportService.java   # NEW - report generation & CSV export
│       └── AuditAlertService.java         # NEW - threshold detection & alert firing
├── src/main/resources/db/migration/
│   └── V20__audit_compliance.sql
└── src/test/

frontend/
├── src/
│   ├── api/
│   │   └── audit.ts
│   ├── components/audit/
│   │   ├── AuditLogTable.tsx
│   │   ├── AuditEventDetail.tsx
│   │   ├── AuditFilters.tsx
│   │   ├── AuditSearchBar.tsx
│   │   ├── ComplianceReportDialog.tsx
│   │   └── AlertRulesPanel.tsx
│   ├── pages/
│   │   └── AuditPage.tsx
│   └── types/
│       └── audit.ts
```

**Structure Decision**: Standard web application pattern (backend + frontend). Extends existing project structure with new audit-specific packages. Uses AOP aspect for cross-cutting event capture to minimize manual instrumentation.

## Complexity Tracking

No violations to justify — standard patterns used throughout. AOP aspect adds minimal complexity and is justified by the constitutional requirement to capture ALL actions without requiring manual instrumentation in every service.
