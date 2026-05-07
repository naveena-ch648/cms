# CMS Platform

A multi-tenant content management system with file upload, preview, search, and collaboration features.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.3.5, Spring Security, Spring Data JPA |
| Frontend | TypeScript 5.6, React 18, React Router 6, Vite 6 |
| Database | MySQL 8.0 |
| Cache | Redis 7 |
| Object Storage | MinIO |
| Search | OpenSearch 2.11 |
| Worker | Python 3.11 (thumbnail/metadata processing) |

## Architecture

```
┌──────────┐     ┌──────────┐     ┌──────────┐
│ Frontend │────▶│  Nginx   │────▶│ Backend  │
│  React   │     │ (port 3000)    │ (port 8080)
└──────────┘     └──────────┘     └────┬─────┘
                                       │
                  ┌────────────────────┬┴──────────────┐
                  ▼                    ▼               ▼
            ┌──────────┐       ┌──────────┐    ┌──────────┐
            │  MySQL   │       │  Redis   │    │  MinIO   │
            │(port 3307)│      │(port 6379)│   │(port 9000)│
            └──────────┘       └──────────┘    └──────────┘
                                     │
                                     ▼
                              ┌──────────┐     ┌────────────┐
                              │  Worker  │────▶│ OpenSearch  │
                              │ (Python) │     │ (port 9200) │
                              └──────────┘     └────────────┘
```

## Prerequisites

- Docker & Docker Compose
- Java 17+ (for local backend development)
- Node.js 18+ (for local frontend development)
- Python 3.11+ (for local worker development)

## Quick Start

Start all services with Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up --build
```

The application will be available at:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| MinIO Console | http://localhost:9001 |
| OpenSearch | http://localhost:9200 |

## Default Credentials

| Service | Username | Password |
|---------|----------|----------|
| Application | admin@cms.com | admin123 |
| MySQL | root | root |
| MinIO | minioadmin | minioadmin |

## Project Structure

```
├── backend/          # Spring Boot API server
│   └── src/main/java/com/cms/
│       ├── controller/   # REST controllers
│       ├── entity/       # JPA entities
│       ├── repository/   # Spring Data repositories
│       ├── service/      # Business logic
│       ├── security/     # JWT auth, filters
│       ├── config/       # App configuration
│       └── dto/          # Request/response DTOs
├── frontend/         # React SPA
│   └── src/
│       ├── api/          # Axios API clients
│       ├── components/   # Reusable components
│       ├── pages/        # Route pages
│       ├── contexts/     # React contexts
│       └── hooks/        # Custom hooks
├── worker/           # Python background processor
│   └── processors/   # Thumbnail & metadata extraction
├── docker/           # Docker Compose & service configs
│   ├── docker-compose.yml
│   ├── mysql/init.sql
│   └── redis/
├── docs/             # API documentation
└── specs/            # Feature specifications
```

## Local Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

Requires MySQL on port 3307 and Redis on port 6379 (start with `docker compose -f docker/docker-compose.yml up mysql redis minio opensearch`).

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on http://localhost:5173 with hot reload.

### Worker

The worker runs as part of Docker Compose automatically. To run it standalone:

```bash
docker compose -f docker/docker-compose.yml up worker --build
```

## Key Features

- **Multi-tenant** — Organization-based isolation with workspace separation
- **File Management** — Upload (single & chunked), download, move, copy, trash/restore
- **File Preview** — PDF, image, text, and office document previews with thumbnail generation
- **Search** — Full-text keyword search powered by OpenSearch with autocomplete
- **RBAC** — Role-based access control with sharing and permission management
- **Collaboration** — Comments, notifications, real-time updates

## API Overview

All API endpoints are prefixed with `/api/v1`. Authentication uses JWT Bearer tokens.

```
POST   /api/v1/auth/login          # Login
POST   /api/v1/auth/refresh        # Refresh token
GET    /api/v1/workspaces          # List workspaces
GET    /api/v1/folders              # List folders
GET    /api/v1/files                # List files in folder
POST   /api/v1/files/upload        # Upload file
GET    /api/v1/files/{id}/download  # Download file
GET    /api/v1/files/{id}/preview   # Preview file
POST   /api/v1/search              # Search files
```

## Environment Variables

### Backend

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3307/cms` | MySQL connection URL |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `JWT_SECRET` | — | JWT signing key (min 256 bits) |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `OPENSEARCH_HOST` | `localhost` | OpenSearch host |
| `OPENSEARCH_PORT` | `9200` | OpenSearch port |

## Stopping Services

```bash
docker compose -f docker/docker-compose.yml down
```

To also remove persisted data:

```bash
docker compose -f docker/docker-compose.yml down -v
```
