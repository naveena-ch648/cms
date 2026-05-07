# Quickstart: Dashboard & Notifications

**Feature**: 012-dashboard-notifications  
**Prerequisites**: Docker Compose running (MySQL, Redis, MinIO), backend compiled, frontend dev server

## Setup

```bash
# Start infrastructure
cd docker && docker-compose up -d

# Backend
cd backend && mvn spring-boot:run

# Frontend
cd frontend && npm run dev
```

## Verification Steps

### 1. Dashboard Summary Load

```bash
# Login and get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cms.com","password":"admin123"}' | jq -r '.data.accessToken')

# Fetch dashboard summary
curl -s http://localhost:8080/api/v1/dashboard/summary \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: JSON with recentFilesCount, unreadNotifications, pendingApprovals, storageUsedBytes, storageMaxBytes, storagePercentage, activeAlertsCount.

### 2. Recent Files Widget

```bash
curl -s "http://localhost:8080/api/v1/dashboard/recent-files?limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: Array of up to 5 file objects with name, workspaceName, lastAccessedAt.

### 3. Activity Feed

```bash
curl -s "http://localhost:8080/api/v1/dashboard/activity?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: Paginated activity events with actionType, targetName, actorName, createdAt.

### 4. Shared Items

```bash
# Shared with me
curl -s "http://localhost:8080/api/v1/dashboard/shared?direction=with_me&limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Shared by me
curl -s "http://localhost:8080/api/v1/dashboard/shared?direction=by_me&limit=5" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: Arrays of shared items with fileName, sharedBy/sharedWith, sharedAt.

### 5. Alerts

```bash
# Get active alerts
curl -s "http://localhost:8080/api/v1/dashboard/alerts" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Dismiss an alert
curl -s -X POST "http://localhost:8080/api/v1/dashboard/alerts/{alertId}/dismiss" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: 200 with alerts array; 204 on dismiss.

### 6. Notifications (existing + enhanced)

```bash
# Get unread count
curl -s "http://localhost:8080/api/v1/notifications/count" \
  -H "Authorization: Bearer $TOKEN" | jq .

# List notifications
curl -s "http://localhost:8080/api/v1/notifications?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: Unread count and paginated notification list.

### 7. Frontend Dashboard

1. Navigate to `http://localhost:5173`
2. Login with admin@cms.com / admin123
3. **Verify**: Dashboard shows:
   - Stats cards (workspaces, storage)
   - Recent files widget with clickable items
   - Activity feed with chronological events
   - Shared items section (with me / by me tabs)
   - Pending approvals widget
   - Alerts banner (if conditions met)
4. **Verify**: Notification bell in header shows unread count
5. Click bell → notification panel opens with list
6. Click notification → navigates to target

### 8. Storage Alert Trigger

1. Upload files until storage exceeds 80% of quota
2. Refresh dashboard
3. **Verify**: Storage warning alert appears

## Common Issues

| Issue | Solution |
|-------|----------|
| Dashboard returns empty | Ensure user has workspace membership and file activity |
| Storage shows 0% | Run V19 migration; check storage_quotas has org entry |
| No notifications | Perform an action that triggers one (share a file, submit approval) |
| Redis cache stale | Wait 2 minutes or restart Redis |
