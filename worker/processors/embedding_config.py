"""Configuration for the embedding processor."""

import os


class EmbeddingConfig:
    QDRANT_HOST = os.environ.get("QDRANT_HOST", "localhost")
    QDRANT_PORT = int(os.environ.get("QDRANT_PORT", 6333))
    COLLECTION_NAME = "document_chunks"

    EMBEDDING_MODEL = os.environ.get("EMBEDDING_MODEL", "all-MiniLM-L6-v2")
    EMBEDDING_DIMENSION = int(os.environ.get("EMBEDDING_DIMENSION", 384))

    # Chunking parameters
    CHUNK_SIZE = 512  # tokens
    CHUNK_OVERLAP = 50  # tokens
    MIN_CHUNK_SIZE = 50  # tokens - skip very small fragments
    BATCH_SIZE = 32  # embeddings per batch

    # Queue names
    EMBEDDING_QUEUE = os.environ.get("EMBEDDING_QUEUE", "embedding:process")
    EMBEDDING_DLQ = os.environ.get("EMBEDDING_DLQ", "embedding:process:dlq")
    MAX_RETRIES = 3
