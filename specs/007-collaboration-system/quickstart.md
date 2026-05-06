# Quickstart: Collaboration System

**Feature**: 007-collaboration-system  
**Prerequisites**: Features 001–006 deployed (multi-tenant, folders, files, versioning, RBAC, preview)

---

## Setup

```bash
# From project root
cd docker
docker-compose up -d mysql redis minio

# Run backend (applies V007 migration automatically)
cd ../backend
mvn spring-boot:run

# Run frontend
cd ../frontend
npm install
npm run dev
```

---

## Verification Flow

### 1. Post a Comment on a File

```bash
# Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"password"}' | jq -r '.data.accessToken')

# Get a file ID from workspace
FILE_ID=$(curl -s http://localhost:8080/api/v1/workspaces/{workspaceId}/files \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data.content[0].id')

# Post a comment
curl -X POST "http://localhost:8080/api/v1/files/$FILE_ID/comments" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content": "This looks great! @[user-uuid] can you take a look?"}'
```

**Expected**: 201 response with comment object including extracted mentions.

### 2. Create a Task

```bash
curl -X POST "http://localhost:8080/api/v1/files/$FILE_ID/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Review formatting on page 3", "assigneeId": "user-uuid", "dueDate": "2026-05-10"}'
```

**Expected**: 201 response with task object, assignee receives notification.

### 3. Check Notifications

```bash
curl http://localhost:8080/api/v1/notifications/count \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: `{ "success": true, "data": { "unreadCount": N } }`

### 4. View Activity Timeline

```bash
curl "http://localhost:8080/api/v1/files/$FILE_ID/activity" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: 200 response with chronological activity events including comment and task creation.

### 5. Frontend Verification

1. Open http://localhost:5173
2. Login and navigate to a workspace
3. Click a file → Collaboration sidebar should appear
4. Switch between Comments, Tasks, Activity tabs
5. Post a comment with @mention
6. Create a task with assignee
7. Check notification bell in header shows unread count

---

## Key Endpoints Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /files/{id}/comments | List file comments |
| POST | /files/{id}/comments | Create comment |
| DELETE | /files/{id}/comments/{id} | Delete comment |
| GET | /files/{id}/tasks | List file tasks |
| POST | /files/{id}/tasks | Create task |
| PATCH | /files/{id}/tasks/{id} | Update task |
| GET | /users/me/tasks | My assigned tasks |
| GET | /notifications | List notifications |
| GET | /notifications/count | Unread count |
| POST | /notifications/read-all | Mark all read |
| GET | /files/{id}/activity | File activity timeline |
