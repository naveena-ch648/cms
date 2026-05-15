"""Thumbnail processor — generates WebP thumbnails for image files."""

import io
import tempfile

import pymysql
from PIL import Image

from config import Config
from processors.storage import get_object, put_object

THUMBNAIL_MAX_SIZE = (300, 300)
SUPPORTED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/tiff"}


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        charset="utf8mb4",
    )


def process_thumbnail(file_id: str, org_id: str):
    """Generate thumbnail for an image file and store it in MinIO."""
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

            if mime_type not in SUPPORTED_TYPES:
                return

            s3 = None  # unused

            # Download original
            image_data = get_object(bucket, key)

            # Generate thumbnail
            img = Image.open(io.BytesIO(image_data))
            img.thumbnail(THUMBNAIL_MAX_SIZE, Image.LANCZOS)

            # Convert to WebP
            thumb_buffer = io.BytesIO()
            img.save(thumb_buffer, format="WebP", quality=80)
            thumb_buffer.seek(0)

            # Upload thumbnail
            thumb_key = f"thumbnails/{file_id}.webp"
            put_object(bucket, thumb_key, thumb_buffer.getvalue(), "image/webp")

            # Update DB
            cur.execute(
                "UPDATE files SET thumbnail_key = %s WHERE uuid = %s",
                (thumb_key, file_id),
            )
            conn.commit()
            print(f"Thumbnail generated for file {file_id}")

    finally:
        conn.close()
