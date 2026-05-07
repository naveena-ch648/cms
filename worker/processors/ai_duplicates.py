"""AI Duplicate Detection — SHA-256 exact match and Qdrant near-duplicate detection."""

import hashlib

import boto3
import pymysql
from sentence_transformers import SentenceTransformer
from qdrant_client import QdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue

from config import Config
from processors.embedding_config import EmbeddingConfig
from processors.ai_tagger import extract_text


_s3_client = None
_qdrant_client = None
_embedding_model = None


def get_s3_client():
    global _s3_client
    if _s3_client is None:
        _s3_client = boto3.client(
            "s3",
            endpoint_url=Config.MINIO_ENDPOINT,
            aws_access_key_id=Config.MINIO_ACCESS_KEY,
            aws_secret_access_key=Config.MINIO_SECRET_KEY,
            region_name=Config.MINIO_REGION,
        )
    return _s3_client


def get_qdrant_client():
    global _qdrant_client
    if _qdrant_client is None:
        _qdrant_client = QdrantClient(host=EmbeddingConfig.QDRANT_HOST, port=EmbeddingConfig.QDRANT_PORT)
    return _qdrant_client


def get_embedding_model():
    global _embedding_model
    if _embedding_model is None:
        _embedding_model = SentenceTransformer(EmbeddingConfig.EMBEDDING_MODEL)
    return _embedding_model


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        cursorclass=pymysql.cursors.DictCursor,
    )


def process_duplicate_detection(file_id, org_id, storage_bucket, storage_key):
    """Detect duplicates using SHA-256 exact match and Qdrant cosine similarity."""
    # Step 1: SHA-256 exact match
    exact_match = _check_exact_duplicate(file_id, org_id, storage_bucket, storage_key)

    # Step 2: Near-duplicate detection via embeddings
    near_duplicates = _check_near_duplicates(file_id, org_id, storage_bucket, storage_key)

    has_duplicates = exact_match is not None or len(near_duplicates) > 0
    confidence = 100.0 if exact_match else (max(d["similarity"] for d in near_duplicates) if near_duplicates else 0.0)

    return {
        "exact_match": exact_match,
        "near_duplicates": near_duplicates,
    }, round(confidence, 2)


def _check_exact_duplicate(file_id, org_id, storage_bucket, storage_key):
    """Check for exact duplicate by SHA-256 hash."""
    s3 = get_s3_client()
    try:
        response = s3.get_object(Bucket=storage_bucket, Key=storage_key)
        content = response["Body"].read()
        sha256 = hashlib.sha256(content).hexdigest()
    except Exception as e:
        print(f"Failed to compute SHA-256 for duplicate check: {e}")
        return None

    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """SELECT uuid, name FROM files 
                   WHERE checksum_sha256 = %s 
                   AND organization_id = %s 
                   AND uuid != %s 
                   AND status = 'ACTIVE'
                   LIMIT 1""",
                (sha256, org_id, file_id),
            )
            row = cursor.fetchone()
            if row:
                return {
                    "file_id": row["uuid"],
                    "file_name": row["name"],
                    "similarity": 100.0,
                }
    finally:
        conn.close()

    return None


def _check_near_duplicates(file_id, org_id, storage_bucket, storage_key, threshold=0.85):
    """Check for near-duplicates using Qdrant vector similarity."""
    # Extract text and create embedding
    text = extract_text(storage_bucket, storage_key, "text/plain", max_chars=5000)
    if not text or len(text.strip()) < 50:
        return []

    model = get_embedding_model()
    embedding = model.encode(text[:3000]).tolist()

    qdrant = get_qdrant_client()
    try:
        results = qdrant.search(
            collection_name=EmbeddingConfig.COLLECTION_NAME,
            query_vector=embedding,
            query_filter=Filter(
                must=[FieldCondition(key="organization_id", match=MatchValue(value=org_id))]
            ),
            limit=5,
            score_threshold=threshold,
        )
    except Exception as e:
        print(f"Qdrant search failed for duplicate detection: {e}")
        return []

    # Get file info for matching documents
    near_duplicates = []
    seen_docs = set()
    conn = get_db_connection()
    try:
        for hit in results:
            doc_id = hit.payload.get("document_id", "")
            if doc_id == file_id or doc_id in seen_docs:
                continue
            seen_docs.add(doc_id)

            with conn.cursor() as cursor:
                cursor.execute("SELECT uuid, name FROM files WHERE uuid = %s AND status = 'ACTIVE'", (doc_id,))
                row = cursor.fetchone()
                if row:
                    near_duplicates.append({
                        "file_id": row["uuid"],
                        "file_name": row["name"],
                        "similarity": round(hit.score * 100, 1),
                    })
    finally:
        conn.close()

    return near_duplicates[:3]
