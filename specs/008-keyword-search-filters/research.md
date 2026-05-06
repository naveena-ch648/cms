# Research: Keyword Search & Filters

**Feature**: 008-keyword-search-filters  
**Date**: 2026-05-06  
**Purpose**: Resolve technical unknowns and validate architecture decisions

---

## R-001: OpenSearch Java Client Integration with Spring Boot 3.x

**Question**: How to integrate OpenSearch 2.x Java client with Spring Boot 3.3.5?

**Decision**: Use `org.opensearch.client:opensearch-java:2.10.0` with `org.opensearch.client:opensearch-rest-client:2.10.0` and Apache HttpClient 5 transport.

**Rationale**: 
- The `opensearch-java` client is the officially maintained client for OpenSearch 2.x
- Uses Jackson for serialization (already in Spring Boot classpath)
- Supports all OpenSearch query types needed (multi_match, bool, prefix, highlight, sort)
- Configuration via `OpenSearchClient` bean with `ApacheHttpClient5TransportBuilder`

**Configuration Pattern**:
```java
@Bean
public OpenSearchClient openSearchClient() {
    HttpHost host = new HttpHost("http", opensearchHost, opensearchPort);
    ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder.builder(host).build();
    return new OpenSearchClient(transport);
}
```

**Maven Dependencies**:
```xml
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-java</artifactId>
    <version>2.10.0</version>
</dependency>
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-rest-client</artifactId>
    <version>2.10.0</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

**Alternatives Considered**: 
- Spring Data Elasticsearch: Not fully compatible with OpenSearch 2.x, divergent API
- Raw HTTP via RestTemplate: Error-prone, no type safety, manual JSON parsing

---

## R-002: OpenSearch Index Mapping Design

**Question**: What is the optimal index mapping for file search with highlighting and filters?

**Decision**: Single index `cms_files` with the following mapping:

```json
{
  "mappings": {
    "properties": {
      "fileUuid": { "type": "keyword" },
      "fileName": { "type": "text", "analyzer": "standard", "fields": { "keyword": { "type": "keyword" } } },
      "content": { "type": "text", "analyzer": "standard" },
      "fileType": { "type": "keyword" },
      "mimeType": { "type": "keyword" },
      "ownerUuid": { "type": "keyword" },
      "ownerName": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "workspaceUuid": { "type": "keyword" },
      "folderPath": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
      "fileSize": { "type": "long" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" },
      "indexedAt": { "type": "date" }
    }
  },
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "standard": {
          "type": "standard"
        }
      }
    }
  }
}
```

**Rationale**:
- `text` fields for full-text search with `standard` analyzer (tokenization, lowercasing)
- `.keyword` sub-fields for exact match filtering, sorting, and aggregations
- Single shard sufficient for development; scale to multiple shards in production
- `content` field stores extracted text — no `.keyword` sub-field (content can be very large)
- `date` fields for range filtering
- `workspaceUuid` as keyword for mandatory workspace isolation filter

**Alternatives Considered**:
- Multiple indices per workspace: Unnecessary complexity, harder to manage
- Nested objects for metadata: Overkill for current scope; can add later for custom metadata (Step 10)

---

## R-003: Search Query Strategy

**Question**: How to implement relevance-ranked multi-field search with highlighting?

**Decision**: Use OpenSearch `multi_match` query with `best_fields` type across `fileName^3`, `content`, `ownerName^2`, and `folderPath`, combined with `bool` query for filters.

**Query Structure**:
```json
{
  "query": {
    "bool": {
      "must": [
        { "multi_match": { "query": "quarterly report", "fields": ["fileName^3", "content", "ownerName^2", "folderPath"], "type": "best_fields" } }
      ],
      "filter": [
        { "term": { "workspaceUuid": "ws-uuid" } },
        { "terms": { "fileType": ["pdf", "document"] } },
        { "range": { "updatedAt": { "gte": "2025-01-01", "lte": "2026-05-06" } } }
      ]
    }
  },
  "highlight": {
    "fields": { "content": { "fragment_size": 150, "number_of_fragments": 3 } },
    "pre_tags": ["<mark>"],
    "post_tags": ["</mark>"]
  },
  "sort": [{ "_score": "desc" }],
  "from": 0,
  "size": 20
}
```

**Rationale**:
- `fileName^3` boost ensures file name matches rank highest
- `best_fields` takes the best-matching field's score (not sum), good for distinct field content
- `filter` context for workspace/type/date — filters don't affect score, are cached by OpenSearch
- Highlighting on `content` field provides search snippet context
- `<mark>` tags for frontend rendering

---

## R-004: Autocomplete Implementation

**Question**: Best approach for fast (<300ms) autocomplete on file/folder names?

**Decision**: Use OpenSearch `match_phrase_prefix` query limited to `fileName` and `folderPath` fields, combined with Redis for recent search terms.

**OpenSearch Query**:
```json
{
  "query": {
    "bool": {
      "must": { "match_phrase_prefix": { "fileName": { "query": "bud", "max_expansions": 10 } } },
      "filter": { "term": { "workspaceUuid": "ws-uuid" } }
    }
  },
  "_source": ["fileUuid", "fileName", "folderPath", "fileType"],
  "size": 5
}
```

**Redis Recent Searches**:
- Key: `search:recent:{userId}` (sorted set, score = timestamp)
- On search execution: `ZADD search:recent:{userId} {timestamp} "{query}"`
- On autocomplete: `ZREVRANGEBYSCORE search:recent:{userId} +inf -inf LIMIT 0 5`
- Max 20 entries per user; trim with `ZREMRANGEBYRANK`

**Rationale**:
- `match_phrase_prefix` gives fast prefix matching without completion suggester overhead
- Limited to 5 results (+ 5 recent) keeps response snappy
- Redis sorted set gives time-ordered recency with O(log N) operations

---

## R-005: Indexing Worker Design

**Question**: How should the Python worker index files into OpenSearch?

**Decision**: Add a `search_indexer.py` processor that:
1. Consumes from Redis `search:index` queue
2. Reads file metadata from MySQL
3. Fetches extracted text from the `extracted_text` column (stored by existing metadata processor)
4. Writes/updates/deletes document in OpenSearch index

**Worker Queue Message Format**:
```json
{
  "action": "index|delete",
  "fileId": "file-uuid",
  "workspaceId": "workspace-uuid",
  "organizationId": "org-uuid"
}
```

**Rationale**:
- Reuses existing worker infrastructure (Redis queue, MySQL connection, error handling)
- Idempotent: re-indexing the same file produces the same result
- Delete action removes from index when file is deleted
- Uses `opensearch-py` client (official Python client for OpenSearch)

**Error Handling**:
- Failed indexing → retry up to 3 times with exponential backoff
- After 3 failures → push to `search:index:dlq` (dead letter queue)
- Log all failures with file ID and error details

---

## R-006: Docker OpenSearch Setup

**Question**: How to add OpenSearch to the existing docker-compose for local development?

**Decision**: Add OpenSearch 2.11 single-node container with security disabled for local dev:

```yaml
opensearch:
  image: opensearchproject/opensearch:2.11.0
  container_name: cms-opensearch
  environment:
    - discovery.type=single-node
    - DISABLE_SECURITY_PLUGIN=true
    - OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
  volumes:
    - opensearch-data:/usr/share/opensearch/data
  healthcheck:
    test: ["CMD-SHELL", "curl -s http://localhost:9200/_cluster/health | grep -q '\"status\":\"green\"\\|\"status\":\"yellow\"'"]
    interval: 10s
    timeout: 5s
    retries: 10
```

**Rationale**:
- Single-node for development (no cluster coordination overhead)
- Security disabled for local dev simplicity (production would enable TLS + auth)
- 512MB heap sufficient for development workloads
- Port 9200 for REST API access

---

## R-007: File Type Categorization

**Question**: How to map MIME types to user-friendly file type categories for filtering?

**Decision**: Map MIME types to categories in both backend and index:

| Category | MIME Types |
|----------|-----------|
| `pdf` | application/pdf |
| `image` | image/* |
| `document` | application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.*, text/* |
| `spreadsheet` | application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.* |
| `presentation` | application/vnd.ms-powerpoint, application/vnd.openxmlformats-officedocument.presentationml.* |
| `video` | video/* |
| `audio` | audio/* |
| `archive` | application/zip, application/x-rar*, application/x-7z*, application/gzip |
| `other` | everything else |

**Rationale**: Users think in categories ("show me PDFs") not MIME types. The mapping is done at index time so filtering is a simple `terms` query on the `fileType` keyword field.

---

## Summary

All technical unknowns resolved. Key decisions:
1. OpenSearch 2.x with `opensearch-java` client (backend) and `opensearch-py` (worker)
2. Single `cms_files` index with text+keyword field strategy
3. `multi_match` + `bool` query for search with highlighting
4. `match_phrase_prefix` + Redis sorted sets for autocomplete
5. Async indexing via existing Redis queue → Python worker pipeline
6. Docker single-node OpenSearch with security disabled for local dev
7. MIME-to-category mapping for user-friendly type filters
