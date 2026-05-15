-- PostgreSQL + pgvector initialization for CMS vector embeddings
-- This script runs automatically on first container start

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- ─────────────────────────────────────────────
-- document_embeddings
-- Mirrors the Qdrant "document_chunks" collection.
-- Stores chunked text embeddings for RAG retrieval.
-- Embedding model: all-MiniLM-L6-v2 (dimension 384)
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS document_embeddings (
    id              BIGSERIAL PRIMARY KEY,
    chunk_id        UUID          NOT NULL DEFAULT gen_random_uuid(),  -- Qdrant point ID equivalent
    document_id     UUID          NOT NULL,
    organization_id BIGINT        NOT NULL,
    workspace_id    BIGINT        NOT NULL,
    file_name       TEXT          NOT NULL,
    page_number     INT           DEFAULT 0,
    char_start      INT           DEFAULT 0,
    char_end        INT           DEFAULT 0,
    chunk_index     INT           DEFAULT 0,
    token_count     INT           DEFAULT 0,
    chunk_text      TEXT          NOT NULL,
    embedding_model VARCHAR(100)  NOT NULL DEFAULT 'all-MiniLM-L6-v2',
    embedding       VECTOR(384),                                        -- 384-dim for all-MiniLM-L6-v2
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────
-- Indexes
-- ─────────────────────────────────────────────

-- HNSW index for fast approximate nearest-neighbour search (cosine distance)
CREATE INDEX IF NOT EXISTS idx_doc_embeddings_vector_hnsw
    ON document_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Exact filters used during RAG retrieval
CREATE INDEX IF NOT EXISTS idx_doc_embeddings_org_workspace
    ON document_embeddings (organization_id, workspace_id);

CREATE INDEX IF NOT EXISTS idx_doc_embeddings_document_id
    ON document_embeddings (document_id);

CREATE INDEX IF NOT EXISTS idx_doc_embeddings_chunk_id
    ON document_embeddings (chunk_id);

-- ─────────────────────────────────────────────
-- embedding_sync_log
-- Tracks which documents have been synced from
-- Qdrant into PostgreSQL (for dual-write / migration).
-- ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS embedding_sync_log (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID         NOT NULL,
    organization_id BIGINT       NOT NULL,
    workspace_id    BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING, SYNCED, FAILED
    chunk_count     INT          DEFAULT 0,
    error_message   TEXT,
    synced_at       TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sync_log_document_id
    ON embedding_sync_log (document_id);

CREATE INDEX IF NOT EXISTS idx_sync_log_status
    ON embedding_sync_log (status);
