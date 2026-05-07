# Quickstart: Audit Logging & Compliance

**Feature**: 013-audit-compliance  
**Prerequisites**: Features 001-012 implemented, Docker environment running

## Quick Setup

```bash
# 1. Start infrastructure (MySQL, Redis, OpenSearch already running from previous features)
cd docker && docker-compose up -d

# 2. Backend - run migrations and start
cd backend
mvn spring-boot:run

# 3. Frontend - start dev server
cd frontend
npm run dev
```

## Key Integration Points

### Existing Services Used

| Service | Usage |
|---------|-------|
| `AuditService` | Extended with async processing, OpenSearch indexing, buffering |
| `AuditEvent` entity | Extended with category, outcome, user_agent, actor_name, workspace_id |
| `AuditEventRepository` | Enhanced with custom query methods |
| `OpenSearchClient` | Reused for audit index CRUD operations |
| `NotificationService` | Used to deliver alert notifications to admins |
| `ActivityEventService` | Pattern reference for async event processing |

### New Components

| Component | Purpose |
|-----------|---------|
| `@Audited` annotation | Marks controller methods for automatic audit capture |
| `AuditAspect` | AOP aspect that intercepts @Audited methods |
| `AuditSearchService` | Builds OpenSearch queries for audit log search |
| `ComplianceReportService` | Generates CSV reports asynchronously |
| `AuditAlertService` | Redis-based threshold detection and alert creation |
| `AuditController` | REST API for all audit operations |

## API Quick Reference

```
GET    /api/audit/events                    # Search/filter events
GET    /api/audit/events/{id}               # Event detail
GET    /api/audit/stats                     # Aggregated statistics
POST   /api/audit/reports                   # Request compliance report
GET    /api/audit/reports                   # List reports
GET    /api/audit/reports/{uuid}/download   # Download CSV
GET    /api/audit/alerts/rules              # List alert rules
POST   /api/audit/alerts/rules              # Create alert rule
PUT    /api/audit/alerts/rules/{uuid}       # Update rule
DELETE /api/audit/alerts/rules/{uuid}       # Delete rule
GET    /api/audit/alerts                    # List triggered alerts
POST   /api/audit/alerts/{uuid}/acknowledge # Acknowledge alert
GET    /api/audit/alerts/{uuid}/events      # Events for alert
```

## Database Migration

- **V20__audit_compliance.sql**: Adds columns to `audit_events`, creates `compliance_reports`, `audit_alert_rules`, `audit_alert_instances`, `audit_alert_events` tables.

## OpenSearch Index

- **Index name**: `audit_events`
- **Purpose**: Full-text search over audit log with keyword, date range, category, and actor filtering.

## Testing

```bash
# Backend unit + integration tests
cd backend && mvn test

# Frontend tests
cd frontend && npm test
```

## Architecture Notes

- **Event Capture**: AOP aspect (`@Audited`) intercepts controller methods → extracts context → async publish to AuditService.
- **Dual Storage**: MySQL (source of truth) + OpenSearch (search optimization). MySQL write is synchronous; OpenSearch indexing is async.
- **Buffering**: If OpenSearch is unavailable, events are buffered in Redis list and retried on a 60s schedule.
- **Alerting**: Redis sorted sets track event counts per user/type within sliding windows. A scheduled job (30s interval) evaluates thresholds.
- **Immutability**: No UPDATE/DELETE API endpoints or repository methods for audit events.
