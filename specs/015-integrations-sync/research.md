# Research: Integrations & Sync

**Feature**: 015-integrations-sync  
**Date**: 2026-05-06

## Research Topics

### 1. Google Drive API v3 Integration with Spring Boot

**Decision**: Use `google-api-services-drive` Java client library for backend Drive operations; `google-auth-library` for OAuth2 flow.

**Rationale**:
- Official Google Java client provides type-safe access to Drive API v3
- Handles pagination, resumable uploads, and file metadata natively
- Well-documented with Spring Boot examples
- Supports service account and OAuth2 user credential flows

**Alternatives considered**:
- Raw HTTP calls to Drive REST API — more control but more boilerplate, no retry/pagination helpers
- Python client in workers only — would fragment the codebase; backend needs Drive access for the OAuth flow and file browsing

**Key patterns**:
- OAuth2 flow: Backend initiates authorization URL → user grants access → callback receives auth code → exchange for access+refresh tokens
- Store encrypted refresh tokens in DB; access tokens are short-lived and regenerated from refresh token
- Use `Drive.Files.list()` with `fields` parameter to minimize response size
- Use `Drive.Files.get()` with `alt=media` for downloading file content
- Respect rate limits: 12,000 requests/minute per project; implement per-org token bucket

### 2. Webhook Delivery System Design

**Decision**: Event-driven architecture with Redis queue for async delivery; backend produces events, Python worker consumes and delivers.

**Rationale**:
- Decouples event production from delivery — upload handler doesn't block on webhook delivery
- Redis queue already used by existing worker infrastructure
- Python worker can handle HTTP delivery with fine-grained timeout/retry control
- Matches existing pattern (file processing workers)

**Alternatives considered**:
- In-process async (Spring @Async) — simpler but no persistence on crash; no retry visibility
- Dedicated webhook microservice — over-engineering for current scale
- Kafka/RabbitMQ — heavier infrastructure; Redis LPUSH/BRPOP sufficient for webhook volume

**Key patterns**:
- Event production: After CMS action completes, backend queries active webhooks matching the event type and pushes delivery jobs to Redis queue `webhook:deliveries`
- Delivery format: JSON payload with event type, timestamp, resource data, idempotency key
- Signature: HMAC-SHA256 of raw JSON body using webhook secret; included in `X-CMS-Signature` header
- Retry policy: 3 attempts with delays 10s, 60s, 300s (exponential backoff)
- Circuit breaker: After 10 consecutive failures, auto-disable webhook and notify admin
- Delivery log: Store response status, duration, attempt number in `webhook_deliveries` table

### 3. Bidirectional Sync Conflict Resolution

**Decision**: Last-writer-wins with conflict preservation — conflicting files are kept (CMS version renamed) and user is notified.

**Rationale**:
- Zero data loss is a hard requirement (SC-006)
- True CRDT-based merge is inappropriate for binary files
- Users need to manually resolve which version to keep
- Notification ensures awareness of conflicts

**Alternatives considered**:
- Last-writer-wins with overwrite — violates zero data loss requirement
- Block sync on conflict until resolved — blocks all other sync items, poor UX
- Three-way merge — only applicable to text files, not general binary content

**Key patterns**:
- Track `lastModifiedTime` and content hash for each synced file in both locations
- On sync cycle: compare local hash vs. remote hash vs. last-known hash
  - If only one changed → propagate change
  - If both changed (conflict) → keep both, rename CMS version with `.conflict-{timestamp}` suffix
- Store sync state per file in `sync_file_state` (or as metadata on sync_link)
- Sync interval: configurable per sync_link, minimum 5 minutes, default 15 minutes
- Sync direction: bidirectional (default), import-only, export-only

### 4. OAuth2 Token Security

**Decision**: Encrypt OAuth refresh tokens using AES-256-GCM with a server-side encryption key; store cipher text in the DB.

**Rationale**:
- Refresh tokens are long-lived credentials that grant file access — must be protected at rest
- AES-256-GCM provides authenticated encryption (confidentiality + integrity)
- Server-side key can be rotated via environment variable without data migration (re-encrypt on access)
- Matches constitution requirement VI (data encrypted at rest)

**Alternatives considered**:
- Store tokens in Redis only (volatile) — lost on restart; not persistent
- Use HSM/Vault — ideal for production but adds infrastructure complexity for MVP
- Asymmetric encryption — unnecessary for server-to-server; symmetric is simpler and faster

**Key patterns**:
- Encryption key stored in environment variable (`INTEGRATION_ENCRYPTION_KEY`)
- `IntegrationTokenEncryptor` utility class: `encrypt(plaintext) → base64(iv + ciphertext + tag)`, `decrypt(encoded) → plaintext`
- On OAuth callback: encrypt refresh token before persisting to `integration_connections.refresh_token_encrypted`
- On use: decrypt refresh token, use Google credential builder to get fresh access token
- Never log or return tokens in API responses

### 5. Scheduled Sync Job Architecture

**Decision**: Use a Spring `@Scheduled` cron job that checks for sync links due for execution and enqueues sync jobs to Redis for the Python worker.

**Rationale**:
- Spring scheduler is lightweight and already available in the stack
- Actual sync work is CPU/IO intensive (Drive API calls, file transfers) — offload to Python worker
- Redis queue provides durability and visibility into pending/in-progress jobs
- Python worker already handles file operations with MinIO/S3

**Alternatives considered**:
- Python-native scheduler (APScheduler) — would require separate always-on process; Spring already runs 24/7
- Quartz scheduler — heavy for simple interval checks; overkill
- Cron container — adds infrastructure; Spring scheduler is zero-config

**Key patterns**:
- `SyncSchedulerService` with `@Scheduled(fixedRate = 60000)` checks every minute
- Queries `sync_links WHERE status='ACTIVE' AND next_sync_at <= NOW()`
- For each due link: push job to Redis `sync:jobs` queue, update `next_sync_at`
- Python `sync_worker.py` processes jobs: authenticate with stored tokens, compare file states, transfer deltas
- Job result written to `sync_jobs` table with success/failure counts

## Unresolved Items

None — all research questions resolved.
