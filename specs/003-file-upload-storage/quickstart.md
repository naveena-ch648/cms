# Quickstart: File Upload & Storage System

**Feature**: 003-file-upload-storage  
**Prerequisites**: Features 001 (multi-tenant) and 002 (workspace-folder) fully implemented

---

## Infrastructure Setup

### 1. Add MinIO to Docker Compose

Add the MinIO service to `docker/docker-compose.yml`:

```yaml
minio:
  image: minio/minio:latest
  container_name: cms-minio
  command: server /data --console-address ":9001"
  ports:
    - "9000:9000"   # S3 API
    - "9001:9001"   # Web Console
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  volumes:
    - minio-data:/data
  healthcheck:
    test: ["CMD", "mc", "ready", "local"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### 2. Add Python Worker Service

```yaml
worker:
  build:
    context: ../worker
    dockerfile: Dockerfile
  container_name: cms-worker
  environment:
    REDIS_HOST: redis
    REDIS_PORT: 6379
    MINIO_ENDPOINT: minio:9000
    MINIO_ACCESS_KEY: minioadmin
    MINIO_SECRET_KEY: minioadmin
    WORKER_CONCURRENCY: 2
  depends_on:
    redis:
      condition: service_healthy
    minio:
      condition: service_healthy
```

### 3. Backend Dependencies (pom.xml)

```xml
<!-- AWS S3 SDK (for MinIO) -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.0</version>
</dependency>

<!-- Apache Tika (MIME type detection) -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
```

### 4. Backend Configuration (application.yml)

```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  region: us-east-1

file-upload:
  max-single-upload-size: 104857600  # 100 MB
  default-chunk-size: 5242880        # 5 MB
  session-ttl-hours: 24
  max-concurrent-chunks: 3
```

### 5. Python Worker Setup

```
worker/
├── Dockerfile
├── requirements.txt        # redis, boto3, Pillow, python-magic
├── worker.py               # Main worker loop (BRPOP from Redis)
├── processors/
│   ├── thumbnail.py        # Image thumbnail generation
│   └── metadata.py         # File metadata extraction
└── config.py               # Environment config
```

---

## Quick Verification Flow

1. **Start services**: `cd docker && docker-compose up -d`
2. **Login**: `POST /api/auth/login` → get JWT token
3. **Small file upload**: `POST /api/files/upload` with multipart/form-data
4. **Verify in folder**: `GET /api/files?folderId={uuid}` → file appears
5. **Download**: `GET /api/files/{id}/download` → 302 redirect to MinIO
6. **Chunked upload**: `POST /api/files/upload/initiate` → upload chunks → complete
7. **Check MinIO console**: http://localhost:9001 (minioadmin/minioadmin)

---

## Key Integration Points

| Component | Connects To | How |
|-----------|------------|-----|
| Spring Boot | MinIO | AWS S3 SDK v2 configured with MinIO endpoint |
| Spring Boot | Redis | Spring Data Redis (upload session state, job queue) |
| Spring Boot | MySQL | Spring Data JPA (file metadata, quota) |
| Python Worker | Redis | `redis-py` BRPOP on `file:process` queue |
| Python Worker | MinIO | `boto3` S3 client for thumbnail storage |
| React Frontend | Spring Boot | Axios for API calls |
| React Frontend | MinIO | Direct presigned URL uploads for chunks |
