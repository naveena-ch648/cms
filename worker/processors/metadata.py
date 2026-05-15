"""Metadata processor — extracts and stores file metadata."""

import json
import tempfile
import os

import magic
import pymysql

from config import Config
from processors.storage import get_object_range


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        charset="utf8mb4",
    )


def process_metadata(file_id: str, org_id: str):
    """Extract metadata from file and update DB."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT storage_bucket, storage_key, mime_type FROM files WHERE uuid = %s",
                (file_id,),
            )
            row = cur.fetchone()
            if not row:
                print(f"File {file_id} not found in database")
                return

            bucket, key, mime_type = row

            s3 = None  # unused

            # Download first 8KB for magic detection
            header_bytes = get_object_range(bucket, key, 0, 8191)

            # Detect MIME type via libmagic
            detected_mime = magic.from_buffer(header_bytes, mime=True)

            metadata = {
                "detectedMimeType": detected_mime,
                "mimeMatch": detected_mime == mime_type,
            }

            # Update DB with extracted metadata
            cur.execute(
                "UPDATE files SET metadata = %s WHERE uuid = %s",
                (json.dumps(metadata), file_id),
            )
            conn.commit()
            print(f"Metadata extracted for file {file_id}: {detected_mime}")

    finally:
        conn.close()
