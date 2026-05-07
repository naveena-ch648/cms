# Quickstart: Admin Console

**Feature**: 014-admin-console  
**Date**: 2026-05-06

## Prerequisites

- Docker and Docker Compose installed
- Existing CMS platform running (features 001–013 implemented)
- Admin user account (admin@cms-platform.com / Admin123!)

## Running Locally

```bash
# Start all services
cd docker
docker compose up -d

# Backend runs on port 8080
# Frontend runs on port 80 (nginx)
# MySQL on port 3307
# Redis on port 6379
```

## Verifying the Feature

### 1. Access Admin Console

1. Login as admin: `admin@cms-platform.com` / `Admin123!`
2. Click "Admin" in the sidebar navigation
3. You should see the Admin Console with sections: Users, Roles, Groups, Storage & Policies, Analytics

### 2. User Management

1. Navigate to Admin → Users
2. Verify the user list shows all org users with name, email, role, status, last login
3. Create a test user: Click "Create User", fill in details, assign Viewer role
4. Search for the created user by name or email
5. Change the user's role to Editor
6. Deactivate the user and verify their status shows "Inactive"

### 3. Role Management

1. Navigate to Admin → Roles
2. Verify system roles (Admin, Editor, Viewer) are listed and marked read-only
3. Create a custom role: Click "Create Role", select permissions
4. View the permission matrix to verify role-permission mapping

### 4. Group Management

1. Navigate to Admin → Groups
2. Create a group, add members
3. Verify member count updates

### 5. Storage & Policies

1. Navigate to Admin → Storage & Policies
2. View current storage usage (used vs total with visual bar)
3. Update max file size, blocked extensions, or trash retention
4. Verify policy updates persist

### 6. Analytics

1. Navigate to Admin → Analytics
2. Verify summary cards: total users, active users, files, storage
3. View upload trend chart for the last 30 days

### 7. Access Control

1. Logout and login as a non-admin user (Viewer or Editor role)
2. Verify the "Admin" navigation link is NOT visible
3. Navigate directly to `/admin` — verify access denied or redirect

## API Testing

```bash
# Login as admin
TOKEN=$(curl -s -X POST http://localhost/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cms-platform.com","password":"Admin123!"}' \
  | jq -r '.accessToken')

# Get admin analytics
curl -s http://localhost/api/v1/admin/analytics \
  -H "Authorization: Bearer $TOKEN" | jq .

# Get storage quota details
curl -s http://localhost/api/v1/admin/storage-quota \
  -H "Authorization: Bearer $TOKEN" | jq .

# Update storage quota
curl -s -X PUT http://localhost/api/v1/admin/storage-quota \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"maxFileSizeBytes": 52428800, "blockedExtensions": [".exe", ".bat"]}' | jq .

# Bulk user action
curl -s -X POST http://localhost/api/v1/admin/users/bulk-action \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userIds": ["<user-uuid>"], "action": "CHANGE_ROLE", "roleId": "<role-uuid>"}' | jq .
```

## Key Files

| Component | Path |
|-----------|------|
| Admin Controller | `backend/src/main/java/com/cms/controller/AdminController.java` |
| Admin Analytics Service | `backend/src/main/java/com/cms/service/AdminAnalyticsService.java` |
| Storage Quota Service | `backend/src/main/java/com/cms/service/StorageQuotaService.java` |
| Migration V21 | `backend/src/main/resources/db/migration/V21__admin_analytics_indices.sql` |
| Admin Page | `frontend/src/pages/AdminPage.tsx` |
| Admin Layout | `frontend/src/components/admin/AdminLayout.tsx` |
| User Management | `frontend/src/components/admin/UserManagement.tsx` |
| Role Management | `frontend/src/components/admin/RoleManagement.tsx` |
| Group Management | `frontend/src/components/admin/GroupManagement.tsx` |
| Storage Policies | `frontend/src/components/admin/StoragePolicies.tsx` |
| Analytics Dashboard | `frontend/src/components/admin/AnalyticsDashboard.tsx` |
| Admin API Client | `frontend/src/api/admin.ts` |
