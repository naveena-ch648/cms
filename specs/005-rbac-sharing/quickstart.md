# Quickstart: RBAC & Sharing System

**Feature**: 005-rbac-sharing

## Prerequisites

- Docker Compose running (MySQL, Redis, MinIO)
- Backend built and running on port 8080
- Frontend dev server on port 5173
- Admin user seeded (from V3 migration)

## Key Flows

### 1. Assign Permission on Folder

```bash
# Assign Viewer role to a user on a folder
curl -X POST http://localhost:8080/api/v1/folders/{folderUuid}/permissions \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"userUuid": "{userUuid}", "roleUuid": "{viewerRoleUuid}", "isOverride": false}'
```

### 2. Check Effective Permission

```bash
# Get current user's resolved permission on a folder
curl http://localhost:8080/api/v1/folders/{folderUuid}/effective-permission \
  -H "Authorization: Bearer {token}"
```

### 3. Create Share Link

```bash
# Create a password-protected share link expiring in 7 days
curl -X POST http://localhost:8080/api/v1/share-links \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "FILE",
    "fileUuid": "{fileUuid}",
    "password": "secret123",
    "expiresAt": "2026-05-13T00:00:00Z",
    "allowDownload": true,
    "watermarkEnabled": false
  }'
```

### 4. Access Share Link (Public)

```bash
# Check if link requires password
curl http://localhost:8080/api/share/{token}

# Verify password
curl -X POST http://localhost:8080/api/share/{token}/verify \
  -H "Content-Type: application/json" \
  -d '{"password": "secret123"}'
```

## Database Migrations

After implementing, two new migrations run automatically:
- `V10__add_permission_inheritance.sql` — adds `is_override` column to `folder_permissions`
- `V11__create_shared_links_tables.sql` — creates `shared_links` and `shared_link_accesses`

## Testing Permission Inheritance

1. Create folder hierarchy: A → B → C
2. Assign "Editor" to user on folder A
3. Verify user inherits "Editor" on B and C (GET effective-permission)
4. Set "Viewer" override on folder B
5. Verify user now has "Viewer" on B and C, still "Editor" on A
6. Remove override on B
7. Verify user reverts to "Editor" on B and C

## Redis Cache Verification

```bash
# Check cached permission
docker exec cms-redis redis-cli GET "folder_perm:{userId}:{folderId}"

# Verify invalidation after permission change
docker exec cms-redis redis-cli KEYS "folder_perm:*"
```
