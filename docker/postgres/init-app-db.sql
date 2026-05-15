-- cms_app database: replaces Redis + MinIO for operational data
-- Runs automatically on first container start (after 01-init.sql)

\c postgres
CREATE DATABASE cms_app OWNER cmsuser;
\c cms_app

-- ─────────────────────────────────────────────────────
-- FILE STORAGE  (replaces MinIO)
-- ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS file_storage (
    id           BIGSERIAL    PRIMARY KEY,
    bucket       VARCHAR(128) NOT NULL,
    storage_key  TEXT         NOT NULL,
    content_type VARCHAR(127) NOT NULL DEFAULT 'application/octet-stream',
    content      BYTEA        NOT NULL,
    size_bytes   BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (bucket, storage_key)
);
CREATE INDEX IF NOT EXISTS idx_file_storage_bucket_key ON file_storage (bucket, storage_key);

-- ─────────────────────────────────────────────────────
-- UPLOAD SESSIONS  (replaces Redis upload_session:{id})
-- ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS upload_sessions (
    session_id        VARCHAR(36)  PRIMARY KEY,
    file_name         TEXT         NOT NULL,
    folder_id         BIGINT       NOT NULL,
    folder_uuid       VARCHAR(36)  NOT NULL,
    organization_id   BIGINT       NOT NULL,
    workspace_id      BIGINT       NOT NULL,
    uploaded_by       BIGINT       NOT NULL,
    total_size        BIGINT       NOT NULL,
    chunk_size        BIGINT       NOT NULL,
    total_chunks      INT          NOT NULL,
    completed_chunks  TEXT         NOT NULL DEFAULT '[]',
    mime_type         VARCHAR(127) NOT NULL DEFAULT 'application/octet-stream',
    bucket            VARCHAR(128) NOT NULL,
    storage_key       TEXT         NOT NULL,
    description       TEXT,
    tags              TEXT         NOT NULL DEFAULT '[]',
    on_duplicate      VARCHAR(20)  NOT NULL DEFAULT 'rename',
    status            VARCHAR(20)  NOT NULL DEFAULT 'INITIATED',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_activity_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_expires ON upload_sessions (expires_at);

-- Stores each chunk's etag keyed by (session_id, chunk_number)
CREATE TABLE IF NOT EXISTS upload_session_parts (
    session_id   VARCHAR(36) NOT NULL,
    chunk_number INT         NOT NULL,
    etag         TEXT        NOT NULL,
    data         BYTEA,
    PRIMARY KEY (session_id, chunk_number)
);

-- ─────────────────────────────────────────────────────
-- JOB QUEUE  (replaces all Redis list queues)
-- ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS job_queue (
    id           BIGSERIAL    PRIMARY KEY,
    queue_name   VARCHAR(64)  NOT NULL,
    payload      TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, PROCESSING, DONE, FAILED
    retry_count  INT          NOT NULL DEFAULT 0,
    error_msg    TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_job_queue_queue_status ON job_queue (queue_name, status, created_at);

-- ─────────────────────────────────────────────────────
-- JWT TOKENS  (replaces Redis jwt:refresh:*, jwt:blocklist:*)
-- ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS jwt_tokens (
    jti        VARCHAR(64)  PRIMARY KEY,
    token_type VARCHAR(20)  NOT NULL,   -- REFRESH, BLOCKLIST, LOCKOUT, PENDING_2FA, FAILED_ATTEMPTS, EMAIL_OTP
    value      TEXT         NOT NULL,   -- userId / "blocked" / "locked" / count
    expires_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_jwt_tokens_type_expires ON jwt_tokens (token_type, expires_at);

-- ─────────────────────────────────────────────────────
-- INTEGRATION JOB STATE  (replaces Redis integration:job:* hashes)
-- ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS integration_job_state (
    job_id       VARCHAR(64)  PRIMARY KEY,
    field        VARCHAR(64)  NOT NULL,
    value        TEXT,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (job_id, field)
);
