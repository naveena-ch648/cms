"""Office preview processor — converts Office docs to PDF via LibreOffice, then renders as images."""

import io
import os
import subprocess
import tempfile
import uuid

import boto3
import pymysql
from PIL import Image
from pdf2image import convert_from_path

from config import Config

THUMBNAIL_SIZE = (256, 256)
PREVIEW_DPI = 150
MAX_PAGES = 100


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


def _convert_to_pdf(input_path: str, output_dir: str) -> str:
    """Convert Office document to PDF using LibreOffice headless."""
    cmd = [
        "libreoffice", "--headless", "--convert-to", "pdf",
        "--outdir", output_dir, input_path,
    ]
    result = subprocess.run(cmd, capture_output=True, timeout=120)
    if result.returncode != 0:
        stderr = result.stderr.decode("utf-8", errors="replace")
        raise RuntimeError(f"LibreOffice conversion failed: {stderr}")

    # Find the output PDF
    base_name = os.path.splitext(os.path.basename(input_path))[0]
    pdf_path = os.path.join(output_dir, f"{base_name}.pdf")
    if not os.path.exists(pdf_path):
        raise RuntimeError(f"Expected PDF not found: {pdf_path}")
    return pdf_path


def process_office_preview(file_id: str, org_id: str, storage_bucket: str, storage_key: str):
    """Convert Office doc to PDF, then render pages as images. Also generates thumbnail."""
    conn = get_db_connection()
    s3 = get_s3_client()

    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM files WHERE uuid = %s", (file_id,))
            file_row = cur.fetchone()
            if not file_row:
                print(f"File {file_id} not found in database")
                return
            db_file_id = file_row[0]

            # Mark jobs as processing
            cur.execute(
                """UPDATE preview_jobs SET status = 'PROCESSING', started_at = NOW(), attempts = attempts + 1
                   WHERE file_id = %s AND status IN ('QUEUED', 'PROCESSING')""",
                (db_file_id,),
            )
            conn.commit()

        # Download file from MinIO
        response = s3.get_object(Bucket=storage_bucket, Key=storage_key)
        file_data = response["Body"].read()

        # Determine extension from storage key
        ext = os.path.splitext(storage_key)[1] or ".docx"

        with tempfile.TemporaryDirectory() as tmp_dir:
            input_path = os.path.join(tmp_dir, f"input{ext}")
            with open(input_path, "wb") as f:
                f.write(file_data)

            # Convert to PDF
            pdf_path = _convert_to_pdf(input_path, tmp_dir)

            # Render pages from the PDF
            images = convert_from_path(pdf_path, dpi=PREVIEW_DPI, last_page=MAX_PAGES)
            if not images:
                raise RuntimeError("No pages extracted from converted PDF")

            # Generate and upload thumbnail from first page
            thumb_img = images[0].copy()
            thumb_img.thumbnail(THUMBNAIL_SIZE, Image.LANCZOS)
            if thumb_img.mode in ("RGBA", "P"):
                thumb_img = thumb_img.convert("RGB")

            thumb_buffer = io.BytesIO()
            thumb_img.save(thumb_buffer, format="JPEG", quality=85)
            thumb_buffer.seek(0)

            thumb_key = f"thumbnails/{file_id}/thumbnail.jpg"
            s3.put_object(
                Bucket=storage_bucket,
                Key=thumb_key,
                Body=thumb_buffer.getvalue(),
                ContentType="image/jpeg",
            )

            # Upload page images
            key_prefix = f"previews/{file_id}"
            total_size = 0
            width = images[0].width
            height = images[0].height

            for i, img in enumerate(images, start=1):
                page_buffer = io.BytesIO()
                img.save(page_buffer, format="PNG")
                page_buffer.seek(0)
                page_size = page_buffer.getbuffer().nbytes
                total_size += page_size

                page_key = f"{key_prefix}/page-{i}.png"
                s3.put_object(
                    Bucket=storage_bucket,
                    Key=page_key,
                    Body=page_buffer.getvalue(),
                    ContentType="image/png",
                )

            # Update DB records
            with conn.cursor() as cur:
                # Thumbnail record
                cur.execute(
                    "SELECT id FROM previews WHERE file_id = %s AND type = 'THUMBNAIL'",
                    (db_file_id,),
                )
                existing_thumb = cur.fetchone()
                if existing_thumb:
                    cur.execute(
                        """UPDATE previews SET status = 'COMPLETED', storage_bucket = %s,
                           thumbnail_key = %s, mime_type = 'image/jpeg', width = %s, height = %s,
                           generated_at = NOW() WHERE id = %s""",
                        (storage_bucket, thumb_key, thumb_img.width, thumb_img.height, existing_thumb[0]),
                    )
                else:
                    cur.execute(
                        """INSERT INTO previews (uuid, file_id, type, status, storage_bucket, thumbnail_key,
                           mime_type, width, height, generated_at)
                           VALUES (%s, %s, 'THUMBNAIL', 'COMPLETED', %s, %s, 'image/jpeg', %s, %s, NOW())""",
                        (str(uuid.uuid4()), db_file_id, storage_bucket, thumb_key, thumb_img.width, thumb_img.height),
                    )

                # Full preview record
                cur.execute(
                    "SELECT id FROM previews WHERE file_id = %s AND type = 'FULL_PREVIEW'",
                    (db_file_id,),
                )
                existing_preview = cur.fetchone()
                if existing_preview:
                    cur.execute(
                        """UPDATE previews SET status = 'COMPLETED', storage_bucket = %s,
                           storage_key_prefix = %s, page_count = %s, mime_type = 'image/png',
                           width = %s, height = %s, file_size_bytes = %s, generated_at = NOW()
                           WHERE id = %s""",
                        (storage_bucket, key_prefix, len(images), width, height, total_size, existing_preview[0]),
                    )
                else:
                    cur.execute(
                        """INSERT INTO previews (uuid, file_id, type, status, storage_bucket,
                           storage_key_prefix, page_count, mime_type, width, height, file_size_bytes, generated_at)
                           VALUES (%s, %s, 'FULL_PREVIEW', 'COMPLETED', %s, %s, %s, 'image/png', %s, %s, %s, NOW())""",
                        (str(uuid.uuid4()), db_file_id, storage_bucket, key_prefix, len(images), width, height, total_size),
                    )

                # Update jobs
                cur.execute(
                    """UPDATE preview_jobs SET status = 'COMPLETED', completed_at = NOW()
                       WHERE file_id = %s AND status = 'PROCESSING'""",
                    (db_file_id,),
                )
                conn.commit()

            print(f"Office preview generated for {file_id}: {len(images)} pages")

    except Exception as e:
        print(f"Office preview generation failed for {file_id}: {e}")
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM files WHERE uuid = %s", (file_id,))
            file_row = cur.fetchone()
            if file_row:
                cur.execute(
                    """UPDATE preview_jobs SET status = 'FAILED', error_message = %s
                       WHERE file_id = %s AND status = 'PROCESSING'""",
                    (str(e)[:1000], file_row[0]),
                )
                conn.commit()
        raise
    finally:
        conn.close()
