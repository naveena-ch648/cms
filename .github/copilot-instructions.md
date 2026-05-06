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
- 008-keyword-search-filters: Added Java 17 (Spring Boot 3.3.5 backend), Python 3.11 (indexing worker), TypeScript 5.6 (React 18 frontend) + Spring Boot 3.3.5, OpenSearch Java Client 2.x, React 18, Axios 1.7.7, Vite 6; Python: opensearch-py, redis-py, boto3, pymysql
- 006-file-preview-engine: Added Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6
- 005-rbac-sharing: Added Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend) + Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
