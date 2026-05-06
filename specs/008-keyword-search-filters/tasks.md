# Tasks: Keyword Search & Filters

**Input**: Design documents from `/specs/008-keyword-search-filters/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested in the feature specification. Test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add OpenSearch to the platform infrastructure and configure client dependencies

- [x] T001 Add OpenSearch 2.11 service to docker/docker-compose.yml with single-node config, security disabled, port 9200, healthcheck, and opensearch-data volume
- [x] T002 [P] Add opensearch-java 2.10.0, opensearch-rest-client 2.10.0, and httpclient5 dependencies to backend/pom.xml
- [x] T003 [P] Add opensearch-py dependency to worker/requirements.txt
- [x] T004 [P] Add OpenSearch connection properties (host, port, index name) to backend/src/main/resources/application.yml
- [x] T005 Create OpenSearchConfig bean with OpenSearchClient initialization in backend/src/main/java/com/cms/config/OpenSearchConfig.java
- [x] T006 Create index initialization logic that creates cms_files index with mapping on app startup in backend/src/main/java/com/cms/service/SearchIndexService.java

---

## Phase 2: Foundational (Indexing Pipeline)

**Purpose**: Async indexing pipeline that must be operational before search can return results

**⚠️ CRITICAL**: No search queries can return results until indexing is working

- [x] T007 Create FileIndexEventPublisher that publishes index/delete events to Redis search:index queue in backend/src/main/java/com/cms/event/FileIndexEventPublisher.java
- [x] T008 Hook FileIndexEventPublisher into existing file upload flow (after successful upload) in backend/src/main/java/com/cms/service/FileService.java
- [x] T009 Hook FileIndexEventPublisher into file delete flow in backend/src/main/java/com/cms/service/FileService.java
- [x] T010 Create search_indexer.py processor that consumes search:index queue, reads file metadata from MySQL, builds document, and indexes into OpenSearch in worker/processors/search_indexer.py
- [x] T011 Add file type categorization utility (MIME to category mapping) in worker/processors/search_indexer.py
- [x] T012 Add folder path resolution (build full path from folder hierarchy) in worker/processors/search_indexer.py
- [x] T013 Add search:index queue listener to worker main loop in worker/worker.py
- [x] T014 Add dead-letter queue handling (move to search:index:dlq after 3 retries) in worker/processors/search_indexer.py
- [x] T015 Add OpenSearch connection config to worker/config.py

**Checkpoint**: Files uploaded after this phase are automatically indexed into OpenSearch and searchable.

---

## Phase 3: User Story 1 - Keyword Search (Priority: P1) 🎯 MVP

**Goal**: Users can search files by keyword and see relevance-ranked results with highlighted snippets

**Independent Test**: Type a keyword in the search endpoint → receive matching files with highlights, pagination, and total count

- [x] T016 [P] [US1] Create SearchRequest DTO with query, workspaceId, page, size fields in backend/src/main/java/com/cms/dto/search/SearchRequest.java
- [x] T017 [P] [US1] Create SearchResponse DTO with results list, pagination, query echo in backend/src/main/java/com/cms/dto/search/SearchResponse.java
- [x] T018 [P] [US1] Create SearchResultDto with fileUuid, fileName, fileType, mimeType, fileSize, ownerUuid, ownerName, folderPath, folderUuid, createdAt, updatedAt, highlights, score in backend/src/main/java/com/cms/dto/search/SearchResultDto.java
- [x] T019 [US1] Create SearchService with keyword search method using multi_match query across fileName, content, ownerName, folderPath with boost weights in backend/src/main/java/com/cms/service/SearchService.java
- [x] T020 [US1] Add workspace-scoped filtering (mandatory workspaceId term filter) to SearchService query builder in backend/src/main/java/com/cms/service/SearchService.java
- [x] T021 [US1] Add highlighting configuration (content field, 150 char fragments, mark tags) to SearchService in backend/src/main/java/com/cms/service/SearchService.java
- [x] T022 [US1] Create SearchController with GET /api/v1/search endpoint that validates workspaceId, calls SearchService, returns SearchResponse in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T023 [P] [US1] Create TypeScript search types (SearchResult, SearchResponse, SearchFilters, SortOption) in frontend/src/types/search.ts
- [x] T024 [P] [US1] Create search API client with search() method in frontend/src/api/search.ts
- [x] T025 [US1] Create SearchResultItem component displaying file name, path, owner, date, type icon, and highlighted snippet in frontend/src/components/search/SearchResultItem.tsx
- [x] T026 [US1] Create SearchResults component with result list, total count, pagination controls, empty state, and loading state in frontend/src/components/search/SearchResults.tsx
- [x] T027 [US1] Create initial SearchPage with search input, submit handler, and SearchResults in frontend/src/pages/SearchPage.tsx
- [x] T028 [US1] Add /workspaces/:workspaceId/search route to React Router in frontend/src/App.tsx

**Checkpoint**: End-to-end keyword search works — user types query, backend queries OpenSearch, results with highlights returned and displayed.

---

## Phase 4: User Story 2 - Filters (Priority: P2)

**Goal**: Users refine search results by file type, owner, and date range

**Independent Test**: Apply type/owner/date filters → only matching files shown; remove filter → full results return

- [x] T029 [US2] Extend SearchRequest with fileType list, ownerUuid, dateFrom, dateTo, dateField filter parameters in backend/src/main/java/com/cms/dto/search/SearchRequest.java
- [x] T030 [US2] Add filter clause construction to SearchService: bool query filter for fileType (terms), ownerUuid (term), date range (range on dateField) in backend/src/main/java/com/cms/service/SearchService.java
- [x] T031 [US2] Update SearchController to pass filter params from request to SearchService in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T032 [US2] Create SearchFilters component with file type checkboxes, owner dropdown, date range pickers, clear-all button in frontend/src/components/search/SearchFilters.tsx
- [x] T033 [US2] Integrate SearchFilters into SearchPage: pass filter state to API, update results on filter change in frontend/src/pages/SearchPage.tsx

**Checkpoint**: Filters work independently and combined with keyword search. Users can browse files by type/owner/date without keyword.

---

## Phase 5: User Story 3 - Sort Results (Priority: P3)

**Goal**: Users change result ordering by relevance, name, date, size, or owner

**Independent Test**: Perform search → change sort to "Date Modified (Newest)" → results reorder correctly

- [x] T034 [US3] Extend SearchRequest with sortBy and sortOrder parameters in backend/src/main/java/com/cms/dto/search/SearchRequest.java
- [x] T035 [US3] Add sort logic to SearchService: map sortBy values to OpenSearch field names (relevance→_score, name→fileName.keyword, dateModified→updatedAt, dateCreated→createdAt, fileSize→fileSize, owner→ownerName.keyword) in backend/src/main/java/com/cms/service/SearchService.java
- [x] T036 [US3] Create SearchSortSelect component with sort options dropdown in frontend/src/components/search/SearchSortSelect.tsx
- [x] T037 [US3] Integrate SearchSortSelect into SearchPage: pass sort state to API, maintain filters on sort change in frontend/src/pages/SearchPage.tsx

**Checkpoint**: Sort changes re-order results without losing keyword or filter state.

---

## Phase 6: User Story 4 - Autocomplete (Priority: P4)

**Goal**: Users see file/folder name suggestions and recent searches as they type

**Independent Test**: Type 2+ chars → file suggestions + recent searches appear within 300ms; select suggestion → navigates or searches

- [x] T038 [P] [US4] Create AutocompleteResponse DTO with files list and recentSearches list in backend/src/main/java/com/cms/dto/search/AutocompleteResponse.java
- [x] T039 [US4] Add autocomplete method to SearchService using match_phrase_prefix on fileName, limited to 5 results, workspace-scoped in backend/src/main/java/com/cms/service/SearchService.java
- [x] T040 [US4] Add recent search methods to SearchService: save (ZADD to Redis sorted set), get (ZREVRANGE), clear (DEL) in backend/src/main/java/com/cms/service/SearchService.java
- [x] T041 [US4] Add GET /api/v1/search/autocomplete endpoint to SearchController in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T042 [US4] Add POST /api/v1/search/recent endpoint (save search term) to SearchController in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T043 [US4] Add DELETE /api/v1/search/recent endpoint (clear history) to SearchController in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T044 [US4] Save search term to recent searches on successful full search execution in SearchController in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T045 [P] [US4] Add autocomplete() and saveRecentSearch() and clearRecentSearches() to frontend search API client in frontend/src/api/search.ts
- [x] T046 [US4] Create SearchBar component with input, debounced autocomplete (300ms), suggestion dropdown (files + recent searches), keyboard navigation in frontend/src/components/search/SearchBar.tsx
- [x] T047 [US4] Replace plain search input with SearchBar component in SearchPage in frontend/src/pages/SearchPage.tsx

**Checkpoint**: Autocomplete suggestions appear as user types; selecting file navigates to it; selecting term triggers search; recent searches persist across sessions.

---

## Phase 7: Frontend Integration & Navigation

**Purpose**: Wire search into existing workspace navigation

- [x] T048 Add search icon/button to workspace header in frontend/src/pages/WorkspacePage.tsx that navigates to /workspaces/:workspaceId/search
- [x] T049 Add search icon/button to DashboardPage header in frontend/src/pages/DashboardPage.tsx (navigates to last workspace search or shows workspace picker)

**Checkpoint**: Users can reach search from any workspace page via header icon.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Input validation, error handling, admin tools, graceful degradation

- [x] T050 [P] Add input validation to SearchController: query max 500 chars, page/size bounds, valid sortBy/sortOrder values in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T051 [P] Add graceful error handling when OpenSearch is unavailable (catch connection errors, return 503 with message) in backend/src/main/java/com/cms/service/SearchService.java
- [x] T052 [P] Create POST /api/v1/admin/search/reindex endpoint that queues all files in a workspace for re-indexing in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T053 [P] Create GET /api/v1/search/health endpoint returning index status, document count, size in backend/src/main/java/com/cms/controller/SearchController.java
- [x] T054 Add OpenSearch environment variables (OPENSEARCH_HOST, OPENSEARCH_PORT) to backend and worker services in docker/docker-compose.yml

**Checkpoint**: System handles edge cases gracefully, admin can re-index, health is monitorable.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately
- **Phase 2 (Indexing Pipeline)**: Depends on Phase 1 (OpenSearch running + client configured)
- **Phase 3 (US1 Keyword Search)**: Depends on Phase 2 (data must be indexed to return results)
- **Phase 4 (US2 Filters)**: Depends on Phase 3 (extends search service and UI)
- **Phase 5 (US3 Sort)**: Depends on Phase 3 (extends search service and UI). Can parallel with Phase 4
- **Phase 6 (US4 Autocomplete)**: Depends on Phase 3 (uses SearchService + SearchPage exists). Can parallel with Phase 4/5
- **Phase 7 (Navigation)**: Depends on Phase 3 (SearchPage must exist)
- **Phase 8 (Polish)**: Can start after Phase 2 (some tasks independent)

### Parallel Execution Opportunities

```text
Phase 1: T001 → [T002 | T003 | T004] → T005 → T006
Phase 2: T007 → [T008 | T009] → T010 → [T011 | T012 | T014 | T015] → T013
Phase 3: [T016 | T017 | T018 | T023 | T024] → T019 → T020 → T021 → T022 → T025 → T026 → T027 → T028
Phase 4: T029 → T030 → T031 → T032 → T033  (parallel with Phase 5, 6)
Phase 5: T034 → T035 → T036 → T037          (parallel with Phase 4, 6)
Phase 6: [T038 | T045] → T039 → T040 → [T041 | T042 | T043] → T044 → T046 → T047
Phase 7: [T048 | T049]
Phase 8: [T050 | T051 | T052 | T053 | T054] (all independent)
```

---

## Implementation Strategy

### MVP Scope (User Story 1 only)

Phases 1 + 2 + 3 deliver a functional keyword search with highlighted results. This is a complete, deployable increment that provides immediate user value.

### Incremental Delivery

1. **MVP**: Phases 1-3 (keyword search works end-to-end)
2. **+Filters**: Phase 4 (users can refine results)
3. **+Sort**: Phase 5 (users can reorder results)
4. **+Autocomplete**: Phase 6 (enhanced search UX)
5. **+Navigation**: Phase 7 (accessible from workspace)
6. **+Polish**: Phase 8 (production-ready)

### Key Technical Notes

- OpenSearch index is created programmatically on Spring Boot startup (no manual setup)
- Indexing is fully async — file upload response time unaffected
- Worker retries up to 3 times before dead-letter queue
- File type categorization maps MIME types to user-friendly categories (pdf, image, document, spreadsheet, presentation, video, audio, archive, other)
- Workspace UUID is a mandatory filter on every search query (multi-tenant isolation)
- Recent searches stored in Redis sorted set, capped at 20 per user
