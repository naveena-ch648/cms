# cms1 Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-06

## Active Technologies
- Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6 (001-multi-tenant-foundation)
- MySQL 8.0 (port 3307) with Flyway migrations, Redis 7 (port 6379) for caching (001-multi-tenant-foundation)
- Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (workers), TypeScript 5.6 (React 18 frontend) + Spring Boot 3.3.5, AWS S3 SDK v2 (for MinIO), Spring Data JPA, Spring Data Redis, Apache Tika, React 18, Axios; Python: redis-py, boto3, Pillow, python-magic (001-multi-tenant-foundation)
- MySQL 8.0 (file metadata, quotas), MinIO (file content, thumbnails), Redis 7 (upload sessions, job queue) (001-multi-tenant-foundation)
- MySQL 8.0 (permissions, shared_links tables), Redis 7 (permission cache with 5min TTL), MinIO (file content) (005-rbac-sharing)
- MySQL 8.0 (port 3307, root/root), Redis 7 (port 6379 for caching & notification counts) (006-file-preview-engine)
- Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (indexing worker), TypeScript 5.6 (React 18 frontend) + Spring Boot 3.3.5, OpenSearch Java Client 2.x, React 18, Axios 1.7.7, Vite 6; Python: opensearch-py, redis-py, boto3, pymysql (008-keyword-search-filters)
- OpenSearch 2.x (search index), MySQL 8.0 (search history/recent searches), Redis 7 (autocomplete cache, indexing queue) (008-keyword-search-filters)
- Java 17 (Spring Boot 3.3.5 backend + RAG orchestration), Python 3.11 (embedding workers), TypeScript 5.6 (React 18 frontend) + Spring Boot 3.3.5, LangChain4j 0.35+, Qdrant Java Client, OpenAI API (configurable LLM provider); Python: sentence-transformers, qdrant-client, redis-py, boto3, langchain; React 18, Axios 1.7.7, Vite 6 (009-ai-document-qa)
- MySQL 8.0 (conversations, messages, embedding jobs), Qdrant (vector embeddings), MinIO (document content), Redis 7 (job queue, conversation cache) (009-ai-document-qa)
- Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6, OpenSearch Java Client 2.x (010-metadata-tagging)
- MySQL 8.0 (port 3307, metadata_fields/metadata_values/tags tables), Redis 7 (tag autocomplete cache), OpenSearch 2.11.0 (metadata index for filtering) (010-metadata-tagging)
- MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V18), Redis 7 (port 6379) for notification counts (011-workflow-approvals)
- MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V19), Redis 7 (port 6379) for notification counts and dashboard caching (012-dashboard-notifications)
- Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, OpenSearch Java Client 2.x, React 18, React Router 6.28, Axios 1.7.7, Vite 6 (013-audit-compliance)
- MySQL 8.0 (port 3307, root/root) with Flyway migrations (next: V20), Redis 7 (port 6379) for event buffering and alert rate tracking, OpenSearch 2.x for audit event indexing (013-audit-compliance)

- Java 17+ (Spring Boot 3.x backend), TypeScript 5.x (React 18 frontend) + Spring Boot 3.x, Spring Security, Spring Data JPA, React 18, React Router, Axios, jjwt (JWT library) (001-multi-tenant-foundation)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

npm test; npm run lint

## Code Style

Java 17+ (Spring Boot 3.x backend), TypeScript 5.x (React 18 frontend): Follow standard conventions

## Recent Changes
- 013-audit-compliance: Added Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, OpenSearch Java Client 2.x, React 18, React Router 6.28, Axios 1.7.7, Vite 6
- 012-dashboard-notifications: Added Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6
- 011-workflow-approvals: Added Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
