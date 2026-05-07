# Research: Audit Logging & Compliance

**Feature**: 013-audit-compliance  
**Date**: 2026-05-06

## Research Tasks

### 1. Extending Existing AuditEvent vs. New Entity

**Decision**: Extend the existing `AuditEvent` entity with additional fields (user-agent, outcome, category enum).

**Rationale**: An `AuditEvent` entity already exists with `audit_events` table and basic fields (organization, user, event_type, resource_type, resource_id, details JSON, ip_address, created_at). Adding columns is simpler than creating a parallel entity. The existing `AuditService.log()` methods can be enhanced to accept additional parameters.

**Alternatives Considered**:
- New separate `ComprehensiveAuditLog` entity: Rejected — would duplicate structure and require migrating existing data.
- Keep AuditEvent as-is and store extra fields in JSON `details`: Rejected — user-agent and outcome are common query fields and should be indexed columns.

### 2. OpenSearch Indexing Strategy for Audit Events

**Decision**: Create a dedicated `audit_events` index in OpenSearch with async indexing via the existing `@Async` pattern (same as ActivityEventService). Use a Redis-buffered queue to handle temporary OpenSearch outages.

**Rationale**: The existing OpenSearch setup (feature 008) provides full-text search with BoolQuery support, highlighting, and pagination. A dedicated index keeps audit data isolated and allows independent scaling of retention/sharding.

**Alternatives Considered**:
- Reuse existing search index: Rejected — audit events have different schema and retention needs.
- Elasticsearch: Rejected — OpenSearch already deployed and configured.
- MySQL full-text search: Rejected — insufficient for the query performance requirements at scale (10K events/min).

### 3. Automatic Event Capture Approach

**Decision**: Use Spring AOP with a custom `@Audited` annotation on controller methods. The aspect intercepts annotated methods, extracts context (actor, IP, user-agent from SecurityContext/HttpServletRequest), and publishes an audit event asynchronously.

**Rationale**: AOP minimizes manual instrumentation — developers annotate a controller method and the audit trail is automatic. The existing `AuditService.log()` is already called from some services but inconsistently. AOP ensures comprehensive coverage as required by FR-001.

**Alternatives Considered**:
- Servlet filter: Rejected — too coarse-grained, captures all HTTP requests not just meaningful actions.
- Spring ApplicationEvent: Rejected — requires manual event publishing in every service method (current approach, incomplete coverage).
- Database triggers: Rejected — cannot capture application-level context (actor, IP, user-agent).

### 4. Compliance Report Generation

**Decision**: Generate reports asynchronously. Admin requests a report → a `ComplianceReport` entity is created with status PENDING → a `@Async` method queries OpenSearch for the date range and category, aggregates data, generates CSV, stores it as a file reference, and updates status to COMPLETED. Admin downloads via a signed URL.

**Rationale**: Reports covering 30+ days with potentially millions of events cannot be generated synchronously. Async generation with status tracking matches the existing pattern (preview generation, embedding jobs).

**Alternatives Considered**:
- Synchronous CSV stream: Rejected — timeout risk for large date ranges, poor UX.
- Store report files in MinIO: Considered but overkill — store CSV directly in a local/temp directory with cleanup after download. Can evolve to MinIO if needed.
- PDF generation: Out of scope per spec assumptions — CSV sufficient for v1.

### 5. Alert Threshold Detection

**Decision**: Use Redis sorted sets with sliding window counters. When an audit event matches an alert rule's criteria, increment the counter in Redis. A scheduled check (every 30s) evaluates whether any counter exceeds its threshold. If so, create an `AuditAlertInstance` and send a notification via the existing `NotificationService`.

**Rationale**: Redis sorted sets provide O(log N) sliding window operations, can handle 10K events/min without impacting MySQL, and the existing Redis infrastructure is already deployed.

**Alternatives Considered**:
- In-memory counters: Rejected — not shared across application instances (horizontal scaling).
- MySQL-based counting: Rejected — too slow for real-time threshold detection at 10K events/min.
- OpenSearch aggregation queries: Considered — could work but adds latency and couples alerting to search index availability.

### 6. Immutability & Tenant Isolation

**Decision**: No UPDATE or DELETE endpoints/methods for audit events. JPA repository extends only with `save()` and query methods — no `deleteBy*` methods. Controller enforces GET-only access. Tenant isolation via `organization_id` filter in all queries (same pattern as existing services).

**Rationale**: The spec requires that no user, even admins, can modify or delete entries. By simply not providing the capability at any layer, immutability is guaranteed. Organization-scoped queries are the established pattern throughout the codebase.

**Alternatives Considered**:
- Database-level row security policies: Overkill for MySQL — application-level enforcement is sufficient.
- Separate database per tenant: Rejected — existing system uses shared database with organization_id isolation.

### 7. Event Buffering During Outages

**Decision**: When OpenSearch indexing fails, push the event JSON to a Redis list (`audit:buffer:{orgId}`). A scheduled job retries indexing buffered events every 60 seconds. MySQL write always succeeds first (primary storage), OpenSearch is secondary (search optimization).

**Rationale**: MySQL is the source of truth. OpenSearch is an optimization for search. If OpenSearch is down, events are still persisted in MySQL and buffered in Redis for later indexing. This satisfies FR-011.

**Alternatives Considered**:
- Kafka: Rejected — adds infrastructure complexity for a feature that can use existing Redis.
- Write-ahead log file: Rejected — harder to manage in containerized deployments.
- Skip indexing silently: Rejected — would create gaps in searchable audit trail.
