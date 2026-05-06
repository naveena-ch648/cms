# Quickstart: Workspace Folder System

**Feature**: 002-workspace-folder-system

## Prerequisites

- Docker Compose running (MySQL 3307, Redis 6379, Backend 8080, Frontend 3000)
- Feature 001 (multi-tenant foundation) deployed and operational
- At least one organization, user, and workspace exist

## Build & Run

```bash
# From repository root
cd backend
mvn clean compile

cd ../frontend
npm install
npm run build

# Start all services
cd ..
docker compose up -d
```

## Verify Feature

### 1. Create a Folder

```bash
# Login to get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@cms.local","password":"admin123"}' | jq -r '.data.accessToken')

# Get workspace UUID
WS_UUID=$(curl -s http://localhost:8080/api/v1/workspaces \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data[0].id')

# Create root folder
curl -X POST "http://localhost:8080/api/v1/workspaces/$WS_UUID/folders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Documents", "parentId": null}'
```

### 2. Create a Subfolder

```bash
# Get the parent folder UUID from the previous response
PARENT_UUID="<uuid-from-step-1>"

curl -X POST "http://localhost:8080/api/v1/workspaces/$WS_UUID/folders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Legal", "parentId": "'$PARENT_UUID'"}'
```

### 3. List Folder Tree

```bash
curl "http://localhost:8080/api/v1/workspaces/$WS_UUID/folders" \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 4. Move a Folder

```bash
FOLDER_UUID="<uuid-of-folder-to-move>"
TARGET_UUID="<uuid-of-new-parent>"

curl -X PUT "http://localhost:8080/api/v1/workspaces/$WS_UUID/folders/$FOLDER_UUID/move" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetParentId": "'$TARGET_UUID'"}'
```

### 5. Frontend Verification

Open `http://localhost:3000` in a browser, login, navigate to a workspace. The folder tree sidebar should appear with:
- Hierarchical tree with expand/collapse
- Breadcrumb bar at top of content area
- Drag-and-drop reordering in tree
- Star icons for favoriting
- Favorites and Recent sections at top of sidebar

## Key Files

| Layer | File | Purpose |
|-------|------|---------|
| Migration | `backend/src/main/resources/db/migration/V4__create_folder_tables.sql` | Creates folders, folder_permissions, folder_favorites, folder_recents tables |
| Entity | `backend/src/main/java/com/cms/entity/Folder.java` | JPA entity with self-referencing parent |
| Service | `backend/src/main/java/com/cms/service/FolderService.java` | CRUD, move, tree building, Redis caching |
| Controller | `backend/src/main/java/com/cms/controller/FolderController.java` | REST endpoints under `/api/v1/workspaces/{id}/folders` |
| Frontend | `frontend/src/components/FolderTree.tsx` | Interactive tree with drag-drop |
| Frontend | `frontend/src/components/Breadcrumbs.tsx` | Path navigation component |
