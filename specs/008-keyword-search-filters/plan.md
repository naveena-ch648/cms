# Implementation Plan: Keyword Search & Filters

**Branch**: `008-keyword-search-filters` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-keyword-search-filters/spec.md`

## Summary

Build a full-text search system for the CMS platform using OpenSearch as the search engine. The system indexes file metadata and extracted text content, provides keyword search with relevance ranking, supports filtering by type/owner/date, sorting, and autocomplete suggestions. A Python indexing worker listens for file events and maintains the search index. The Spring Boot backend exposes search APIs, and the React frontend provides a search bar with autocomplete, filters panel, and paginated results.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (indexing worker), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Boot 3.3.5, OpenSearch Java Client 2.x, React 18, Axios 1.7.7, Vite 6; Python: opensearch-py, redis-py, boto3, pymysql  
**Storage**: OpenSearch 2.x (search index), MySQL 8.0 (search history/recent searches), Redis 7 (autocomplete cache, indexing queue)  
**Testing**: Maven Surefire (JUnit 5), pytest, Vitest  
**Target Platform**: Docker (Linux containers), local development on Windows  
**Project Type**: Web service (API + UI)  
**Performance Goals**: <500ms search response (p95), <300ms autocomplete, <60s index latency after upload  
**Constraints**: Multi-tenant isolation (workspace-scoped), permission-filtered results, max 500 char query  
**Scale/Scope**: 100K+ files per workspace, 20 concurrent search users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evidence |
|-----------|--------|----------|
| I. Code Quality | ✅ PASS | Modular services (SearchService, IndexingWorker), strict typing, clear interfaces |
| II. Testing Standards | ✅ PASS | Unit tests for search service, integration tests with OpenSearch testcontainer |
| III. UX Consistency | ✅ PASS | Loading/empty/error states, consistent API response envelope, accessible search UI |
| IV. Performance & Scalability | ✅ PASS | OpenSearch horizontal scaling, async indexing, <500ms target, Redis caching |
| V. Reliability & Fault Tolerance | ✅ PASS | Retry on index failures, dead-letter for failed indexing, idempotent re-index |
| VI. Security & Compliance | ✅ PASS | Permission-filtered results, workspace-scoped queries, JWT auth on all endpoints |
| VII. Data & AI Governance | ✅ PASS | Document lineage tracked (file→index), reprocessing supported via bulk re-index |
| VIII. Observability | ✅ PASS | Search latency metrics logged, error rates on index failures |
| IX. Developer Experience | ✅ PASS | Single docker-compose up, clear API docs, consistent project structure |
| X. Continuous Improvement | ✅ PASS | Search quality metrics enable iteration |

**Gate Result**: ALL PASS — proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/008-keyword-search-filters/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── search-api.md
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── config/
│   │   └── OpenSearchConfig.java          # OpenSearch client bean
│   ├── controller/
│   │   └── SearchController.java          # Search + autocomplete endpoints
│   ├── dto/
│   │   └── search/
│   │       ├── SearchRequest.java         # Query + filters + sort + pagination
│   │       ├── SearchResponse.java        # Results with highlights and facets
│   │       └── AutocompleteResponse.java  # Suggestion list
│   ├── service/
│   │   ├── SearchService.java             # Builds OpenSearch queries
│   │   └── SearchIndexService.java        # Index/delete operations + event publishing
│   └── event/
│       └── FileIndexEventPublisher.java   # Publishes index events to Redis queue
├── src/main/resources/
│   └── application.yml                    # + OpenSearch connection config

docker/
├── docker-compose.yml                     # + opensearch + opensearch-dashboards services
└── opensearch/
    └── (index created programmatically on app startup)

worker/
├── processors/
│   └── search_indexer.py                  # Consumes search:index queue, writes to OpenSearch
├── worker.py                              # + search:index queue listener
├── requirements.txt                       # + opensearch-py

frontend/
├── src/
│   ├── api/
│   │   └── search.ts                      # Search + autocomplete API client
│   ├── components/
│   │   └── search/
│   │       ├── SearchBar.tsx              # Input with autocomplete dropdown
│   │       ├── SearchFilters.tsx          # Filter panel (type, owner, date)
│   │       ├── SearchResults.tsx          # Result list with highlights
│   │       ├── SearchResultItem.tsx       # Single result card
│   │       └── SearchSortSelect.tsx       # Sort dropdown
│   ├── pages/
│   │   └── SearchPage.tsx                 # Full search page layout
│   └── types/
│       └── search.ts                      # Search-related TypeScript types
```

## Architecture Decisions

### AD-001: OpenSearch as Search Engine
- **Decision**: Use OpenSearch 2.x (AWS-compatible fork of Elasticsearch)
- **Rationale**: Full-text search, relevance ranking, highlighting, aggregations, horizontally scalable, Docker-compatible, open-source
- **Alternatives Rejected**: MySQL FULLTEXT (limited ranking/highlighting/scalability), Meilisearch (simpler but less mature for complex filtered queries and aggregations)

### AD-002: Asynchronous Indexing via Redis Queue
- **Decision**: Backend publishes indexing events to Redis queue (`search:index`); Python worker consumes and writes to OpenSearch
- **Rationale**: Decouples upload path from search indexing, uses existing Redis+Worker infrastructure, supports retry/DLQ patterns, non-blocking to user operations
- **Alternatives Rejected**: Synchronous indexing in Spring Boot (blocks upload, couples systems), database triggers (complex, not portable)

### AD-003: Permission Filtering via Workspace Scope
- **Decision**: All search queries scoped by workspace UUID (stored in index). Permission enforced at workspace membership level — if a user has workspace access, they can search all files in it.
- **Rationale**: Simplifies query construction, aligns with existing RBAC model, avoids expensive per-document ACL lookups in search engine
- **Alternatives Rejected**: Per-file permission IDs in index with terms lookup (complex, degrades search performance at scale)

### AD-004: Autocomplete via OpenSearch Prefix + Redis Recent Searches
- **Decision**: Autocomplete combines OpenSearch `match_phrase_prefix` for file/folder names with Redis sorted set for per-user recent searches
- **Rationale**: Achieves <300ms response time; recent searches provide personalized experience; capped at 20 items per user
- **Alternatives Rejected**: Separate completion suggester index (overkill for file count), client-side only (can't search full workspace)

### AD-005: OpenSearch Java Client (not RestHighLevelClient)
- **Decision**: Use official `opensearch-java` client 2.x with Jackson-based serialization
- **Rationale**: RestHighLevelClient is deprecated; the new client is the official path forward, supports all OpenSearch 2.x features
- **Alternatives Rejected**: Spring Data Elasticsearch (not fully compatible with OpenSearch 2.x), raw REST calls (error-prone)

## Phases

### Phase 1: Infrastructure Setup
- Add OpenSearch 2.x service to docker-compose.yml
- Add `opensearch-java` client dependency to backend pom.xml
- Create OpenSearchConfig bean with client initialization
- Create index mapping programmatically on application startup (index: `cms_files`)
- Add `opensearch-py` to worker requirements.txt

### Phase 2: Indexing Pipeline
- Create FileIndexEventPublisher: publishes events to Redis `search:index` queue on file create/update/delete
- Hook into existing file upload/update/delete flows
- Create `search_indexer.py` worker processor: consumes queue, reads file metadata from MySQL, fetches extracted text, writes to OpenSearch
- Handle re-index (update) and delete-from-index operations
- Dead-letter queue for failed indexing attempts

### Phase 3: Search API (US1 - Keyword Search)
- Create SearchService: builds OpenSearch multi_match query across file name, content, metadata
- Create SearchController: GET /api/v1/search?q={query}&workspaceId={id}&page={n}&size={n}
- Return results with highlighted snippets, pagination, total count
- Workspace-scoped via mandatory workspaceId filter in query
- Create SearchRequest/SearchResponse DTOs

### Phase 4: Filters & Sorting (US2, US3)
- Extend SearchRequest with filter params: fileType[], ownerUuid, dateFrom, dateTo, sortBy, sortOrder
- Extend SearchService with bool query filter clauses
- Support sort by: _score (relevance), name.keyword, updatedAt, createdAt, fileSize, ownerName.keyword
- Frontend: SearchFilters component, SearchSortSelect component

### Phase 5: Autocomplete (US4)
- Create GET /api/v1/search/autocomplete?q={prefix}&workspaceId={id}
- OpenSearch match_phrase_prefix on fileName and folderPath fields
- Redis sorted set per user for recent searches (key: `search:recent:{userId}`, max 20)
- Save search terms on full search execution
- Frontend: SearchBar with debounced autocomplete dropdown

### Phase 6: Frontend Search Page
- Create SearchPage with SearchBar, SearchFilters, SearchResults, pagination
- Route: /workspaces/:workspaceId/search
- Add search icon to workspace header navigating to SearchPage
- Loading states, empty state ("No results found"), error handling

### Phase 7: Polish & Cross-Cutting
- Input validation: query max 500 chars, sanitize filter values
- Bulk re-index admin endpoint (POST /api/v1/admin/search/reindex)
- Index health check integrated into app health endpoint
- Graceful degradation when OpenSearch is unavailable (return error, don't crash)
- Log search latency metrics
