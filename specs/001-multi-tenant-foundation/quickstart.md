# Quickstart: Multi-Tenant Foundation

**Feature**: 001-multi-tenant-foundation
**Prerequisites**: Docker, Docker Compose, JDK 17+, Node.js 18+

---

## 1. Start Infrastructure

```bash
docker compose up -d mysql redis
```

Wait for MySQL to be ready on port 3307 and Redis on port 6379.

## 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend starts on `http://localhost:8080`. Flyway runs migrations automatically.

## 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on `http://localhost:3000`.

## 4. Verify Setup

### Create an organization (platform admin bootstrap)

```bash
curl -X POST http://localhost:8080/api/v1/organizations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <platform-admin-token>" \
  -d '{
    "name": "Acme Corporation",
    "slug": "acme-corp",
    "billingContactEmail": "billing@acme.com"
  }'
```

### Register a user

```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "email": "jane@acme.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "password": "SecurePass123!",
    "roleId": "<editor-role-uuid>"
  }'
```

### Sign in

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jane@acme.com",
    "password": "SecurePass123!"
  }'
```

### Create a workspace

```bash
curl -X POST http://localhost:8080/api/v1/workspaces \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access-token>" \
  -d '{
    "name": "Project Alpha",
    "description": "Main project workspace"
  }'
```

## 5. Run Tests

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend
npm test
```

## 6. Full Stack (Docker Compose)

```bash
docker compose up --build
```

This starts MySQL (3307), Redis (6379), backend (8080), and frontend (3000) together.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_HOST` | localhost | MySQL hostname |
| `MYSQL_PORT` | 3307 | MySQL port |
| `MYSQL_DATABASE` | cms | Database name |
| `MYSQL_USERNAME` | cms_user | DB username |
| `MYSQL_PASSWORD` | cms_pass | DB password |
| `REDIS_HOST` | localhost | Redis hostname |
| `REDIS_PORT` | 6379 | Redis port |
| `JWT_SECRET` | (generated) | JWT signing secret |
| `JWT_ACCESS_TTL` | 900 | Access token TTL in seconds |
| `JWT_REFRESH_TTL` | 604800 | Refresh token TTL in seconds |
