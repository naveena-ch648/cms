# Quickstart: Keyword Search & Filters

**Feature**: 008-keyword-search-filters  
**Date**: 2026-05-06

---

## Prerequisites

- Docker & Docker Compose running
- Existing CMS platform (Steps 1-7) operational
- At least a few files uploaded to a workspace

---

## 1. Start Infrastructure

```bash
cd docker
docker compose up -d
```

This starts MySQL, Redis, MinIO, OpenSearch, backend, worker, and frontend.

**Verify OpenSearch is healthy**:
```bash
curl http://localhost:9200/_cluster/health
# Should return status: "green" or "yellow"
```

---

## 2. Verify Index Creation

On backend startup, the `cms_files` index is created automatically if it doesn't exist.

```bash
curl http://localhost:9200/cms_files
# Should return index mapping
```

---

## 3. Trigger File Indexing

Files uploaded after this feature is deployed are automatically indexed. For existing files, trigger a re-index:

```bash
curl -X POST http://localhost:8080/api/v1/admin/search/reindex \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"workspaceId": "<your-workspace-uuid>"}'
```

Wait ~60 seconds for indexing to complete, then verify:
```bash
curl http://localhost:9200/cms_files/_count
# Should show count matching your file count
```

---

## 4. Test Search

### Keyword Search
```bash
curl "http://localhost:8080/api/v1/search?q=report&workspaceId=<ws-uuid>" \
  -H "Authorization: Bearer <jwt>"
```

### Search with Filters
```bash
curl "http://localhost:8080/api/v1/search?q=budget&workspaceId=<ws-uuid>&fileType=pdf&sortBy=dateModified&sortOrder=desc" \
  -H "Authorization: Bearer <jwt>"
```

### Autocomplete
```bash
curl "http://localhost:8080/api/v1/search/autocomplete?q=bud&workspaceId=<ws-uuid>" \
  -H "Authorization: Bearer <jwt>"
```

---

## 5. Frontend

Navigate to: `http://localhost:3000/workspaces/<id>/search`

Or click the search icon in the workspace header.

**Verify**:
1. Type a keyword → results appear with highlighted snippets
2. Apply a file type filter → results narrow
3. Change sort order → results reorder
4. Type in search bar → autocomplete suggestions appear
5. Clear all → resets to initial state

---

## 6. End-to-End Flow

1. Upload a new file to workspace
2. Wait ~30-60 seconds (async indexing)
3. Search for text from the file name or content
4. File appears in results with relevant snippet
5. Apply filters (type, date, owner)
6. Change sort order
7. Select a result → navigates to file location

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No search results | Check `curl localhost:9200/cms_files/_count` — if 0, trigger re-index |
| OpenSearch not starting | Check docker logs: `docker logs cms-opensearch`. Increase Docker memory to 4GB+ |
| Indexing not happening | Check worker logs: `docker logs cms-worker`. Verify Redis connection |
| Slow search | Check OpenSearch heap: increase `-Xmx` in docker-compose if >80% used |
| 503 on search | OpenSearch container may be starting up. Wait 30s and retry |

---

## Configuration

Key environment variables (in docker-compose.yml):

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENSEARCH_HOST` | `opensearch` | OpenSearch hostname |
| `OPENSEARCH_PORT` | `9200` | OpenSearch port |
| `OPENSEARCH_INDEX` | `cms_files` | Index name |
