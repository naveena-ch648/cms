# Quickstart: Metadata & Tagging System

**Feature**: 010-metadata-tagging  
**Date**: 2026-05-06

---

## Prerequisites

- Existing CMS platform running (Steps 1–9 complete)
- Docker Compose with MySQL, Redis, MinIO, OpenSearch already operational
- OpenSearch file index from Step 8 operational

## New Infrastructure

No new infrastructure services needed. This feature extends existing MySQL, Redis, and OpenSearch.

## Database Migration

Run Flyway migration `V17__metadata_tagging_tables.sql` which creates:
- `metadata_fields` table (field definitions per workspace)
- `metadata_values` table (assigned values per file per field)
- `file_tags` table (tags per file)

## Development Setup

### Backend

No new dependencies in `pom.xml` — uses existing Spring Data JPA, Spring Data Redis, and OpenSearch client.

New Java files:
- `MetadataFieldController.java`, `MetadataValueController.java`, `TagController.java`
- `MetadataFieldService.java`, `MetadataValueService.java`, `TagService.java`
- `MetadataField.java`, `MetadataValue.java`, `Tag.java` (entities)
- DTOs in `dto/metadata/` package

### Frontend

No new npm dependencies — uses existing React, Axios, TypeScript setup.

New TypeScript files:
- `src/api/metadata.ts`, `src/api/tags.ts`
- `src/components/metadata/MetadataFieldManager.tsx`
- `src/components/metadata/MetadataEditor.tsx`
- `src/components/metadata/MetadataFilter.tsx`
- `src/components/metadata/TagInput.tsx`
- `src/components/metadata/BulkMetadataDialog.tsx`

## Running

```bash
# Start all services (no new containers needed)
docker compose -f docker/docker-compose.yml up --build

# Verify migration ran
docker exec cms-mysql mysql -u root -proot cms -e "SHOW TABLES LIKE 'metadata%';"
docker exec cms-mysql mysql -u root -proot cms -e "SHOW TABLES LIKE 'file_tags';"
```

## Testing the Feature

1. **Create a metadata field** (as workspace admin):
   ```bash
   curl -X POST http://localhost:8080/api/v1/workspaces/{wsId}/metadata-fields \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"name": "Department", "fieldType": "DROPDOWN", "options": ["HR", "Finance", "Engineering"]}'
   ```

2. **Assign metadata to a file**:
   ```bash
   curl -X PUT http://localhost:8080/api/v1/files/{fileId}/metadata \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"values": [{"fieldId": "<field-uuid>", "value": "Engineering"}]}'
   ```

3. **Add tags to a file**:
   ```bash
   curl -X POST http://localhost:8080/api/v1/files/{fileId}/tags \
     -H "Authorization: Bearer <token>" \
     -H "Content-Type: application/json" \
     -d '{"tags": ["confidential", "urgent"]}'
   ```

4. **Filter files by metadata**:
   ```bash
   curl "http://localhost:8080/api/v1/workspaces/{wsId}/files?meta.Department=Engineering&tag=urgent" \
     -H "Authorization: Bearer <token>"
   ```

5. **Tag autocomplete**:
   ```bash
   curl "http://localhost:8080/api/v1/workspaces/{wsId}/tags/autocomplete?prefix=ur&limit=5" \
     -H "Authorization: Bearer <token>"
   ```

## Key Configuration

| Setting | Default | Description |
|---------|---------|-------------|
| Max fields per workspace | 50 | Configurable via application.yml |
| Max tags per file | 20 | Enforced in TagService |
| Max tag length | 50 chars | Validated on input |
| Tag autocomplete cache | Redis ZSET | Per-workspace, no TTL |
| Metadata fields cache | Redis String | 10-min TTL, invalidated on change |
| OpenSearch index update | Synchronous | Updated on metadata/tag change |

## OpenSearch Index Update

The existing `files` index mapping is extended with:
```json
{
  "metadata": { "type": "object", "dynamic": true },
  "tags": { "type": "keyword" }
}
```

Files are re-indexed when metadata values or tags change. The existing search API is extended with filter parameters.
