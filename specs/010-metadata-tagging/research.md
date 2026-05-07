# Research: Metadata & Tagging System

**Feature**: 010-metadata-tagging  
**Date**: 2026-05-06

---

## 1. Custom Metadata Field Storage Pattern

**Decision**: EAV (Entity-Attribute-Value) pattern with typed validation at the application layer.

**Rationale**: 
- EAV allows unlimited custom fields per workspace without schema changes
- Field type validation is enforced in the service layer, not at the DB column level
- Aligns with established CMS patterns (WordPress custom fields, SharePoint metadata)
- MySQL column-per-field approach would require DDL on every field creation — unacceptable for multi-tenant SaaS

**Alternatives considered**:
- JSON column for all metadata values: Rejected — harder to index, no per-field type validation at DB level, poor query performance for filtering
- Column-per-field (dynamic DDL): Rejected — risky for multi-tenant, requires migrations per tenant action
- Separate typed tables (text_values, number_values, date_values): Rejected — overly complex joins, marginal benefit over EAV with application-layer validation

**Implementation notes**:
- `metadata_fields` stores field definitions (name, type, workspace_id, options JSON for dropdowns)
- `metadata_values` stores actual values (field_id, file_id, text_value, number_value, date_value)
- Use appropriate column for type: `text_value VARCHAR(1000)`, `number_value DECIMAL(20,6)`, `date_value DATE`
- Validation in MetadataValueService ensures type-correct assignment

---

## 2. Tag Autocomplete Performance

**Decision**: Redis sorted set for workspace-scoped tag autocomplete with prefix matching.

**Rationale**:
- Redis ZRANGEBYLEX provides O(log(N)+M) prefix matching — ideal for autocomplete
- Tags are workspace-scoped, so key is `tags:autocomplete:{workspaceId}`
- Redis handles thousands of concurrent autocomplete requests with sub-ms latency
- Cache invalidated on tag creation/deletion within workspace

**Alternatives considered**:
- MySQL LIKE query: Rejected — too slow for real-time autocomplete at scale (100K+ tags)
- OpenSearch suggest: Rejected — heavier infrastructure for a simple prefix match use case
- In-memory application cache: Rejected — not shared across backend instances

**Implementation notes**:
- On tag creation: `ZADD tags:autocomplete:{wsId} 0 {tagName}`
- On autocomplete request: `ZRANGEBYLEX tags:autocomplete:{wsId} [{prefix} [{prefix}\xff LIMIT 0 10`
- TTL: none (persistent cache); invalidated on tag add/remove
- Warm cache on first request per workspace if empty

---

## 3. Metadata Filtering via OpenSearch

**Decision**: Index metadata fields and tags into existing OpenSearch document index as nested/flat fields.

**Rationale**:
- OpenSearch already indexes files for keyword search (Step 8)
- Adding metadata as structured fields enables faceted filtering
- Combining metadata filters with full-text search is a single query
- No additional infrastructure needed

**Alternatives considered**:
- MySQL-only filtering with JOINs across EAV: Rejected — EAV joins for multiple filters are O(n²) complex and slow at scale
- Separate metadata-only OpenSearch index: Rejected — unnecessary fragmentation; enriching existing index is simpler
- Elasticsearch (separate from OpenSearch): Rejected — duplicate infrastructure

**Implementation notes**:
- Extend existing file document in OpenSearch with:
  - `metadata` object: key-value pairs of field_name → value
  - `tags` array: string array of tag names
- On metadata value change: update the specific file's document in OpenSearch
- On tag change: update the tags array in the file's OpenSearch document
- Filter queries use `term` (exact) for dropdowns/tags, `range` for dates/numbers, `match` for text

---

## 4. Dropdown Options Management

**Decision**: Store dropdown options as a JSON array in the `metadata_fields` table.

**Rationale**:
- Simpler schema — no separate `field_options` table needed
- Options list is typically small (< 100 items per field)
- JSON supports ordered lists naturally
- Easy to add/remove/reorder options

**Alternatives considered**:
- Separate `metadata_field_options` table: Rejected — over-normalized for small option sets; adds extra JOINs
- Comma-separated string: Rejected — no proper escaping, hard to maintain

**Implementation notes**:
- Column: `options JSON` in `metadata_fields` table
- Format: `["Option A", "Option B", "Option C"]`
- Validation: MetadataValueService checks submitted value is in the options array
- Soft-removal: removed options are tracked in a separate `deprecated_options JSON` column or simply removed from options (existing values retained as-is)

---

## 5. Bulk Operations Strategy

**Decision**: Batch UPDATE in a single transaction with optimistic locking.

**Rationale**:
- Bulk metadata assignment is a bounded operation (max ~100 files at a time in UI)
- Single transaction ensures atomicity — all succeed or all fail
- Optimistic locking prevents lost updates from concurrent edits
- Avoids heavy queue-based async processing for a straightforward batch write

**Alternatives considered**:
- Async worker-based processing: Rejected — overkill for <100 file batch updates; adds latency for user
- Individual API calls per file: Rejected — N+1 API calls, poor UX with individual failure handling

**Implementation notes**:
- Endpoint accepts list of file IDs + metadata/tag values
- Service iterates and applies within `@Transactional`
- On conflict: return partial success response with failed file IDs
- OpenSearch bulk update after DB transaction commits

---

## 6. Soft Delete for Metadata Fields

**Decision**: Add `deleted_at` timestamp column for soft-delete; hide from UI but preserve data.

**Rationale**:
- Constitution requires data governance and audit trail
- Existing metadata values must be preserved for historical queries
- Recoverable if admin accidentally deletes a field
- Minimal schema impact

**Alternatives considered**:
- Hard delete (CASCADE): Rejected — violates data governance, destroys audit trail
- Archive to separate table: Rejected — adds migration complexity for a simple toggle

**Implementation notes**:
- `deleted_at TIMESTAMP NULL` on `metadata_fields`
- Repository queries filter `WHERE deleted_at IS NULL` for active fields
- Existing values with deleted field are hidden from UI but queryable via admin/API
- OpenSearch index retains the values (not removed on field deletion)
