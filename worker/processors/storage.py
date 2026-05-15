"""PostgreSQL-backed storage helper — replaces all boto3/S3/MinIO calls."""

import io
import psycopg2
from config import Config


def _get_pg_conn():
    return psycopg2.connect(
        host=Config.PG_HOST,
        port=Config.PG_PORT,
        dbname=Config.PG_DB,
        user=Config.PG_USER,
        password=Config.PG_PASSWORD,
    )


def get_object(bucket: str, key: str) -> bytes:
    """Read file content from file_storage table. Returns raw bytes."""
    conn = _get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT content FROM file_storage WHERE bucket=%s AND storage_key=%s",
                (bucket, key),
            )
            row = cur.fetchone()
            if row is None:
                raise FileNotFoundError(f"Object not found: bucket={bucket}, key={key}")
            data = row[0]
            return bytes(data) if data is not None else b""
    finally:
        conn.close()


def get_object_range(bucket: str, key: str, start: int, end: int) -> bytes:
    """Read a byte range from a stored file. Falls back to slicing in Python."""
    data = get_object(bucket, key)
    return data[start : end + 1]


def put_object(bucket: str, key: str, body: bytes, content_type: str = "application/octet-stream") -> None:
    """Write/replace file content in file_storage table."""
    conn = _get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO file_storage (bucket, storage_key, content_type, content, size_bytes)
                VALUES (%s, %s, %s, %s, %s)
                ON CONFLICT (bucket, storage_key)
                    DO UPDATE SET content=EXCLUDED.content,
                                  content_type=EXCLUDED.content_type,
                                  size_bytes=EXCLUDED.size_bytes,
                                  updated_at=NOW()
                """,
                (bucket, key, content_type, psycopg2.Binary(body), len(body)),
            )
        conn.commit()
    finally:
        conn.close()


def delete_object(bucket: str, key: str) -> None:
    conn = _get_pg_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "DELETE FROM file_storage WHERE bucket=%s AND storage_key=%s",
                (bucket, key),
            )
        conn.commit()
    finally:
        conn.close()
