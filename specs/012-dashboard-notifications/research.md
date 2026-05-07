# Research: Dashboard & Notifications

**Feature**: 012-dashboard-notifications  
**Date**: 2026-05-06

## R1: Activity Event Tracking Strategy

**Decision**: Create a lightweight `activity_events` table that records user actions with denormalized display fields for fast dashboard queries.

**Rationale**: The CMS already performs actions (uploads, shares, transitions, comments) across services. Rather than retroactively parsing logs, we create activity events at the service layer when actions occur. Denormalized fields (actor_name, target_name) enable fast reads without joins.

**Alternatives considered**:
- Event sourcing: Over-engineered for a dashboard feed; we don't need replay capability
- Audit log reuse: Audit logs (feature 013) have compliance focus; dashboard activity needs user-friendly titles and filtered events
- CDC from MySQL binlog: Complex infrastructure for a read-heavy feed

## R2: Dashboard API Design — Aggregate vs. Individual Widgets

**Decision**: Provide both a summary endpoint (`GET /dashboard/summary`) for initial load and individual widget endpoints for lazy-loading/refresh.

**Rationale**: The summary endpoint returns counts and minimal data (recent files IDs, unread notification count, approval counts, storage percentage) in a single call for fast initial paint. Individual endpoints (`/dashboard/recent-files`, `/dashboard/activity`, `/dashboard/shared`) provide full paginated data when widgets expand.

**Alternatives considered**:
- Single aggregate endpoint: Too slow if any one data source is slow; violates independent widget loading requirement
- All separate endpoints: Too many parallel requests on initial load; 6+ concurrent requests per page view
- GraphQL: Not worth adding for a single aggregation use case

## R3: Redis Caching Strategy for Dashboard

**Decision**: Cache dashboard summary per user with 2-minute TTL in Redis. Invalidate on relevant mutations (file upload, share, notification).

**Rationale**: Dashboard is the highest-traffic page. Most data is stable for short periods. A 2-minute TTL balances freshness with performance. The existing Redis infrastructure (already used for notification counts) is extended.

**Alternatives considered**:
- No caching: Unacceptable for repeated dashboard loads within session
- Long TTL (10+ min): Too stale for activity feed and notification counts
- Event-driven invalidation only: Complex; time-based expiry is simpler and sufficient

## R4: Alert Generation Strategy

**Decision**: Alerts generated lazily on dashboard load by checking conditions (storage > 80%, links expiring < 24h) rather than via background scheduled jobs.

**Rationale**: Alert conditions are cheap to evaluate (single query each). Generating on-demand avoids a background job that writes alerts for users who may never view their dashboard. Dismissed alerts are persisted to prevent re-display.

**Alternatives considered**:
- Scheduled alert generation job: Generates alerts for all users including inactive ones; wasteful
- Push notifications via WebSocket: Out of scope for v1 per spec assumptions
- Alert as notification type: Conflates two UX patterns; alerts are dismissible banners, notifications are persistent items

## R5: Notification System Enhancement

**Decision**: Extend existing `NotificationService` and `NotificationController` with bulk mark-as-read and notification type filtering. The existing entity and service are sufficient; no new notification tables needed.

**Rationale**: The notification infrastructure (entity, service, Redis-cached unread count, controller with list/read/read-all) already exists from feature 007. We only need to add type filtering and ensure the frontend NotificationPanel presents them well.

**Alternatives considered**:
- Rebuild notifications: Wasteful; existing implementation is solid
- Add notification preferences table: Out of scope for v1; all notification types are enabled by default

## R6: Shared Items Display Strategy

**Decision**: Query SharedLinkRepository for links created by user (shared by me) and query a new permission-based query for files shared to the user (shared with me).

**Rationale**: "Shared by me" uses existing `findByCreatedByIdAndWorkspaceId`. "Shared with me" requires querying file permissions where the user was granted access by someone else — use existing permission infrastructure.

**Alternatives considered**:
- Only show shared links: Misses internal permission-based sharing
- Track shares in separate table: Redundant with permissions table
