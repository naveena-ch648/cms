"""Image preview processor — generates thumbnails for image files."""

import io
import uuid

import boto3
import pymysql
from PIL import Image

from config import Config

THUMBNAIL_SIZE = (256, 256)


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


def process_image_thumbnail(file_id: str, org_id: str, storage_bucket: str, storage_key: str):
    """Generate a 256x256 thumbnail for an image file."""
    conn = get_db_connection()
    s3 = get_s3_client()

    try:
        # Update job status
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM files WHERE uuid = %s", (file_id,))
            file_row = cur.fetchone()
            if not file_row:
                print(f"File {file_id} not found in database")
                return
            db_file_id = file_row[0]

            cur.execute(
                """UPDATE preview_jobs SET status = 'PROCESSING', started_at = NOW(), attempts = attempts + 1
                   WHERE file_id = %s AND job_type = 'THUMBNAIL' AND status IN ('QUEUED', 'PROCESSING')
                   ORDER BY queued_at DESC LIMIT 1""",
                (db_file_id,),
            )
            conn.commit()

        # Download image from MinIO
        response = s3.get_object(Bucket=storage_bucket, Key=storage_key)
        image_data = response["Body"].read()

        # Generate thumbnail
        img = Image.open(io.BytesIO(image_data))
        img.thumbnail(THUMBNAIL_SIZE, Image.LANCZOS)

        # Convert to JPEG
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")

        thumb_buffer = io.BytesIO()
        img.save(thumb_buffer, format="JPEG", quality=85)
        thumb_buffer.seek(0)

        # Upload thumbnail
        thumb_key = f"thumbnails/{file_id}/thumbnail.jpg"
        s3.put_object(
            Bucket=storage_bucket,
            Key=thumb_key,
            Body=thumb_buffer.getvalue(),
            ContentType="image/jpeg",
        )

        # Update preview record
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id FROM previews WHERE file_id = %s AND type = 'THUMBNAIL'",
                (db_file_id,),
            )
            existing = cur.fetchone()

            if existing:
                cur.execute(
                    """UPDATE previews SET status = 'COMPLETED', storage_bucket = %s, 
                       thumbnail_key = %s, mime_type = 'image/jpeg', width = %s, height = %s,
                       generated_at = NOW() WHERE id = %s""",
                    (storage_bucket, thumb_key, img.width, img.height, existing[0]),
                )
            else:
                preview_uuid = str(uuid.uuid4())
                cur.execute(
                    """INSERT INTO previews (uuid, file_id, type, status, storage_bucket, thumbnail_key,
                       mime_type, width, height, generated_at)
                       VALUES (%s, %s, 'THUMBNAIL', 'COMPLETED', %s, %s, 'image/jpeg', %s, %s, NOW())""",
                    (preview_uuid, db_file_id, storage_bucket, thumb_key, img.width, img.height),
                )

            # Update job status to completed
            cur.execute(
                """UPDATE preview_jobs SET status = 'COMPLETED', completed_at = NOW()
                   WHERE file_id = %s AND job_type = 'THUMBNAIL' AND status = 'PROCESSING'
                   ORDER BY queued_at DESC LIMIT 1""",
                (db_file_id,),
            )
            conn.commit()

        print(f"Image thumbnail generated for {file_id}")

    except Exception as e:
        print(f"Image thumbnail generation failed for {file_id}: {e}")
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM files WHERE uuid = %s", (file_id,))
            file_row = cur.fetchone()
            if file_row:
                cur.execute(
                    """UPDATE preview_jobs SET status = 'FAILED', error_message = %s
                       WHERE file_id = %s AND job_type = 'THUMBNAIL' AND status = 'PROCESSING'
                       ORDER BY queued_at DESC LIMIT 1""",
                    (str(e)[:1000], file_row[0]),
                )
                conn.commit()
        raise
    finally:
        conn.close()
