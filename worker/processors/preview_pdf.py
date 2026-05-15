"""PDF preview processor — generates page images and thumbnails for PDF files."""

import io
import os
import tempfile
import uuid

import pymysql
from PIL import Image
from pdf2image import convert_from_path

from config import Config
from processors.storage import get_object, put_object

THUMBNAIL_SIZE = (256, 256)
PREVIEW_DPI = 150
MAX_PAGES = 100


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        charset="utf8mb4",
    )


def _update_preview_record(conn, file_id, preview_type, status, **kwargs):
    """Update or create preview record in DB."""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id FROM files WHERE uuid = %s", (file_id,)
        )
        file_row = cur.fetchone()
        if not file_row:
            return
        db_file_id = file_row[0]

        cur.execute(
            "SELECT id FROM previews WHERE file_id = %s AND type = %s",
            (db_file_id, preview_type),
        )
        existing = cur.fetchone()

        if existing:
            sets = ", ".join(f"{k} = %s" for k in kwargs.keys())
            values = list(kwargs.values()) + [status, existing[0]]
            cur.execute(
                f"UPDATE previews SET {sets}, status = %s WHERE id = %s",
                values,
            )
        else:
            preview_uuid = str(uuid.uuid4())
            cols = ["uuid", "file_id", "type", "status"] + list(kwargs.keys())
            placeholders = ", ".join(["%s"] * len(cols))
            col_names = ", ".join(cols)
            values = [preview_uuid, db_file_id, preview_type, status] + list(kwargs.values())
            cur.execute(
                f"INSERT INTO previews ({col_names}) VALUES ({placeholders})",
                values,
            )
        conn.commit()


def _update_job_status(conn, file_id, job_type, status):
    """Update preview job status."""
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM files WHERE uuid = %s", (file_id,))
        file_row = cur.fetchone()
        if not file_row:
            return
        db_file_id = file_row[0]

        cur.execute(
            """UPDATE preview_jobs SET status = %s, 
               started_at = CASE WHEN %s = 'PROCESSING' THEN NOW() ELSE started_at END,
               completed_at = CASE WHEN %s IN ('COMPLETED', 'FAILED') THEN NOW() ELSE completed_at END,
               attempts = attempts + 1
               WHERE file_id = %s AND job_type = %s AND status IN ('QUEUED', 'PROCESSING')
               ORDER BY queued_at DESC LIMIT 1""",
            (status, status, status, db_file_id, job_type),
        )
        conn.commit()


def process_pdf_thumbnail(file_id: str, org_id: str, storage_bucket: str, storage_key: str):
    """Generate a 256x256 thumbnail from the first page of a PDF."""
    conn = get_db_connection()

    try:
        _update_job_status(conn, file_id, "THUMBNAIL", "PROCESSING")

        # Download PDF from PostgreSQL storage
        pdf_data = get_object(storage_bucket, storage_key)

        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
            tmp.write(pdf_data)
            tmp_path = tmp.name

        try:
            # Convert first page to image
            images = convert_from_path(tmp_path, first_page=1, last_page=1, dpi=72)
            if not images:
                raise RuntimeError("No pages extracted from PDF")

            img = images[0]
            img.thumbnail(THUMBNAIL_SIZE, Image.LANCZOS)

            thumb_buffer = io.BytesIO()
            img.save(thumb_buffer, format="JPEG", quality=85)
            thumb_buffer.seek(0)

            # Upload thumbnail
            thumb_key = f"thumbnails/{file_id}/thumbnail.jpg"
            put_object(storage_bucket, thumb_key, thumb_buffer.getvalue(), "image/jpeg")

            # Update preview record
            _update_preview_record(
                conn, file_id, "THUMBNAIL", "COMPLETED",
                storage_bucket=storage_bucket,
                thumbnail_key=thumb_key,
                mime_type="image/jpeg",
                width=img.width,
                height=img.height,
                generated_at=None,  # Will use NOW() via SQL default
            )
            _update_job_status(conn, file_id, "THUMBNAIL", "COMPLETED")

            print(f"PDF thumbnail generated for {file_id}")

        finally:
            os.unlink(tmp_path)

    except Exception as e:
        print(f"PDF thumbnail generation failed for {file_id}: {e}")
        _update_job_status(conn, file_id, "THUMBNAIL", "FAILED")
        raise
    finally:
        conn.close()


def process_pdf_preview(file_id: str, org_id: str, storage_bucket: str, storage_key: str):
    """Generate full page-by-page preview images from a PDF."""
    conn = get_db_connection()

    try:
        _update_job_status(conn, file_id, "FULL_PREVIEW", "PROCESSING")

        # Download PDF from PostgreSQL storage
        pdf_data = get_object(storage_bucket, storage_key)

        with tempfile.NamedTemporaryFile(suffix=".pdf", delete=False) as tmp:
            tmp.write(pdf_data)
            tmp_path = tmp.name

        try:
            # Convert all pages (up to MAX_PAGES)
            images = convert_from_path(tmp_path, dpi=PREVIEW_DPI, last_page=MAX_PAGES)
            if not images:
                raise RuntimeError("No pages extracted from PDF")

            key_prefix = f"previews/{file_id}"
            total_size = 0
            width = images[0].width if images else 0
            height = images[0].height if images else 0

            for i, img in enumerate(images, start=1):
                page_buffer = io.BytesIO()
                img.save(page_buffer, format="PNG")
                page_buffer.seek(0)
                page_size = page_buffer.getbuffer().nbytes
                total_size += page_size

                page_key = f"{key_prefix}/page-{i}.png"
                put_object(storage_bucket, page_key, page_buffer.getvalue(), "image/png")

            # Update preview record
            _update_preview_record(
                conn, file_id, "FULL_PREVIEW", "COMPLETED",
                storage_bucket=storage_bucket,
                storage_key_prefix=key_prefix,
                page_count=len(images),
                mime_type="image/png",
                width=width,
                height=height,
                file_size_bytes=total_size,
            )
            _update_job_status(conn, file_id, "FULL_PREVIEW", "COMPLETED")

            print(f"PDF preview generated for {file_id}: {len(images)} pages")

        finally:
            os.unlink(tmp_path)

    except Exception as e:
        print(f"PDF preview generation failed for {file_id}: {e}")
        _update_job_status(conn, file_id, "FULL_PREVIEW", "FAILED")
        raise
    finally:
        conn.close()
