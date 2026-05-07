# Quickstart: Integrations & Sync

**Feature**: 015-integrations-sync

## Prerequisites

- Docker environment running (all existing services)
- Google Cloud Console project with OAuth2 credentials (for Drive integration)

## Environment Variables

Add to `backend/src/main/resources/application.yml` or Docker environment:

```yaml
# Google Drive OAuth2
google:
  drive:
    client-id: ${GOOGLE_DRIVE_CLIENT_ID}
    client-secret: ${GOOGLE_DRIVE_CLIENT_SECRET}
    redirect-uri: http://localhost:8080/api/v1/integrations/google-drive/callback

# Token encryption
integration:
  encryption-key: ${INTEGRATION_ENCRYPTION_KEY}  # 32-byte base64 key for AES-256-GCM
```

### Generate Encryption Key

```bash
openssl rand -base64 32
```

### Google Cloud Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create OAuth2 credentials (Web Application type)
3. Add authorized redirect URI: `http://localhost:8080/api/v1/integrations/google-drive/callback`
4. Enable Google Drive API
5. Set `GOOGLE_DRIVE_CLIENT_ID` and `GOOGLE_DRIVE_CLIENT_SECRET`

## Docker Compose Addition

```yaml
# In docker/docker-compose.yml, add to backend environment:
environment:
  - GOOGLE_DRIVE_CLIENT_ID=${GOOGLE_DRIVE_CLIENT_ID:-not-set}
  - GOOGLE_DRIVE_CLIENT_SECRET=${GOOGLE_DRIVE_CLIENT_SECRET:-not-set}
  - INTEGRATION_ENCRYPTION_KEY=${INTEGRATION_ENCRYPTION_KEY:-dGVzdC1rZXktZm9yLWRldi1vbmx5LTMyYnl0ZXM=}
```

## Database Migration

Migration `V22__integrations_sync.sql` creates:
- `integration_connections`
- `webhooks`
- `webhook_deliveries`
- `sync_links`
- `sync_jobs`

Applied automatically by Flyway on startup.

## Testing

### Test Webhook Registration

```bash
# Register a webhook (use https://webhook.site for testing)
curl -X POST http://localhost:8080/api/v1/webhooks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Webhook",
    "url": "https://webhook.site/your-uuid",
    "secret": "my-test-secret-16chars",
    "eventTypes": ["file.uploaded", "file.deleted"]
  }'
```

### Test Webhook Delivery

```bash
# Send test event
curl -X POST http://localhost:8080/api/v1/webhooks/{webhookId}/test \
  -H "Authorization: Bearer $TOKEN"
```

### Test Google Drive Connection

```bash
# Initiate OAuth flow
curl http://localhost:8080/api/v1/integrations/google-drive/connect \
  -H "Authorization: Bearer $TOKEN"
# Returns authorization URL — open in browser
```

### Test Sync Link

```bash
# Create sync link (requires active Google Drive connection)
curl -X POST http://localhost:8080/api/v1/integrations/sync-links \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "connectionId": "connection-uuid",
    "folderId": "cms-folder-uuid",
    "externalFolderId": "google-drive-folder-id",
    "externalFolderName": "Synced Folder",
    "direction": "BIDIRECTIONAL",
    "syncIntervalMinutes": 15
  }'
```

## Worker Setup

New worker processes for webhooks and sync:

```python
# worker/webhook_worker.py - processes webhook delivery queue
# worker/sync_worker.py - processes sync jobs from Redis queue
```

Run alongside existing worker:
```bash
docker compose up worker  # Updated to run all worker processes
```

## Development Flow

1. Start all services: `docker compose up -d`
2. Backend starts with Flyway migrations (V22 applied)
3. Register test webhook via API
4. Upload a file → webhook delivery triggered
5. Connect Google Drive → browse/import files
6. Create sync link → scheduled sync runs automatically
