"""Thumbnail processor — generates WebP thumbnails for image files."""

import io
import tempfile

import boto3
import pymysql
from PIL import Image

from config import Config

THUMBNAIL_MAX_SIZE = (300, 300)
SUPPORTED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/tiff"}


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=Config.MINIO_ENDPOINT,
        aws_access_key_id=Config.MINIO_ACCESS_KEY,
        aws_secret_access_key=Config.MINIO_SECRET_KEY,
        region_name=Config.MINIO_REGION,
    )


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

            s3 = get_s3_client()

            # Download original
            response = s3.get_object(Bucket=bucket, Key=key)
            image_data = response["Body"].read()

            # Generate thumbnail
            img = Image.open(io.BytesIO(image_data))
            img.thumbnail(THUMBNAIL_MAX_SIZE, Image.LANCZOS)

            # Convert to WebP
            thumb_buffer = io.BytesIO()
            img.save(thumb_buffer, format="WebP", quality=80)
            thumb_buffer.seek(0)

            # Upload thumbnail
            thumb_key = f"thumbnails/{file_id}.webp"
            s3.put_object(
                Bucket=bucket,
                Key=thumb_key,
                Body=thumb_buffer.getvalue(),
                ContentType="image/webp",
            )

            # Update DB
            cur.execute(
                "UPDATE files SET thumbnail_key = %s WHERE uuid = %s",
                (thumb_key, file_id),
            )
            conn.commit()
            print(f"Thumbnail generated for file {file_id}")

    finally:
        conn.close()
