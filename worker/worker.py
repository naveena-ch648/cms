"""Main worker process — polls PostgreSQL job_queue table (replaces Redis BRPOP)."""

import json
import signal
import sys
import time
import threading
import traceback

import psycopg2
import psycopg2.extras

from config import Config
from processors.thumbnail import process_thumbnail
from processors.metadata import process_metadata
from processors.preview_pdf import process_pdf_preview, process_pdf_thumbnail
from processors.preview_image import process_image_thumbnail
from processors.preview_video import process_video_thumbnail
from processors.preview_office import process_office_preview
from processors.search_indexer import process_search_index
from processors.embeddings import process_embedding
from processors.embedding_config import EmbeddingConfig
from processors.sync_worker import run_integration_worker
from processors.webhook_worker import run_webhook_worker
from processors.ai_dispatcher import run_ai_worker


running = True


def signal_handler(sig, frame):
    global running
    print("Shutting down gracefully...")
    running = False


def get_pg_conn():
    return psycopg2.connect(
        host=Config.PG_HOST,
        port=Config.PG_PORT,
        dbname=Config.PG_DB,
        user=Config.PG_USER,
        password=Config.PG_PASSWORD,
    )


def claim_job(conn, queue_names: list):
    """Atomically claim one PENDING job from any of the given queues. Returns (id, queue_name, payload) or None."""
    placeholders = ",".join(["%s"] * len(queue_names))
    with conn.cursor(cursor_factory=psycopg2.extras.DictCursor) as cur:
        cur.execute(f"""
            UPDATE job_queue
            SET status = 'PROCESSING', updated_at = NOW()
            WHERE id = (
                SELECT id FROM job_queue
                WHERE queue_name IN ({placeholders})
                  AND status = 'PENDING'
                  AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
                ORDER BY created_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, queue_name, payload, retry_count
        """, queue_names)
        row = cur.fetchone()
        conn.commit()
        if row:
            return dict(row)
    return None


def ack_job(conn, job_id: int):
    with conn.cursor() as cur:
        cur.execute("DELETE FROM job_queue WHERE id = %s", (job_id,))
    conn.commit()


def fail_job(conn, job_id: int, error_msg: str, max_retries: int, retry_delays: list, dlq_name: str):
    with conn.cursor() as cur:
        cur.execute("SELECT retry_count, queue_name, payload FROM job_queue WHERE id = %s", (job_id,))
        row = cur.fetchone()
        if not row:
            return
        retry_count, queue_name, payload = row
        new_retry = retry_count + 1
        if new_retry < max_retries:
            delay = retry_delays[retry_count] if retry_count < len(retry_delays) else 60
            cur.execute("""
                UPDATE job_queue
                SET status = 'PENDING', retry_count = %s, error_msg = %s,
                    next_attempt_at = NOW() + (%s || ' seconds')::interval,
                    updated_at = NOW()
                WHERE id = %s
            """, (new_retry, error_msg[:500], str(delay), job_id))
            print(f"Retrying job {job_id} (attempt {new_retry}/{max_retries}) after {delay}s")
        else:
            # Move to DLQ by changing queue_name and resetting status
            cur.execute("""
                UPDATE job_queue
                SET status = 'PENDING', queue_name = %s, error_msg = %s, updated_at = NOW()
                WHERE id = %s
            """, (dlq_name, error_msg[:500], job_id))
            print(f"Job {job_id} moved to DLQ ({dlq_name}) after {max_retries} failures")
    conn.commit()


def process_job(conn, job_data: dict):
    payload = json.loads(job_data["payload"]) if isinstance(job_data["payload"], str) else job_data["payload"]
    file_id = payload.get("fileId")
    org_id = payload.get("organizationId")
    action = payload.get("action", "process")
    mime_type = payload.get("mimeType", "")
    storage_bucket = payload.get("storageBucket", "")
    storage_key = payload.get("storageKey", "")

    print(f"Processing file {file_id} (org={org_id}, action={action}, mime={mime_type})")
    _dispatch_action(action, mime_type, file_id, org_id, storage_bucket, storage_key, conn)


def _dispatch_action(action, mime_type, file_id, org_id, storage_bucket, storage_key, conn):
    if action == "preview":
        if mime_type == "application/pdf":
            process_pdf_preview(file_id, org_id, storage_bucket, storage_key)
            process_pdf_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type.startswith("image/"):
            process_image_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type.startswith("video/"):
            process_video_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type in (
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
        ):
            process_office_preview(file_id, org_id, storage_bucket, storage_key)
        else:
            print(f"Unsupported mime type for preview: {mime_type}")

    elif action == "thumbnail":
        if mime_type == "application/pdf":
            process_pdf_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type.startswith("image/"):
            process_image_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type.startswith("video/"):
            process_video_thumbnail(file_id, org_id, storage_bucket, storage_key)
        elif mime_type in (
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.ms-excel",
            "application/vnd.ms-powerpoint",
        ):
            process_office_preview(file_id, org_id, storage_bucket, storage_key)
        else:
            print(f"Unsupported mime type for thumbnail: {mime_type}")

    else:
        try:
            process_thumbnail(file_id, org_id)
        except Exception as e:
            print(f"Thumbnail processing failed for file {file_id}: {e}")
        try:
            process_metadata(file_id, org_id)
        except Exception as e:
            print(f"Metadata processing failed for file {file_id}: {e}")


def main():
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    print(f"Worker starting — PostgreSQL at {Config.PG_HOST}:{Config.PG_PORT}/{Config.PG_DB}")
    print(f"Polling queues: {Config.QUEUE_NAME}, {Config.SEARCH_INDEX_QUEUE}, {EmbeddingConfig.EMBEDDING_QUEUE}")
    print(f"Integration/webhook/AI workers running as sub-threads")

    conn = get_pg_conn()

    # Sub-threads receive their own connections
    int_conn = get_pg_conn()
    integration_thread = threading.Thread(
        target=run_integration_worker,
        args=(int_conn, lambda: running),
        daemon=True,
    )
    integration_thread.start()

    wh_conn = get_pg_conn()
    webhook_thread = threading.Thread(
        target=run_webhook_worker,
        args=(wh_conn, lambda: running),
        daemon=True,
    )
    webhook_thread.start()

    ai_conn = get_pg_conn()
    ai_thread = threading.Thread(
        target=run_ai_worker,
        args=(ai_conn, lambda: running),
        daemon=True,
    )
    ai_thread.start()

    MAIN_QUEUES = [Config.QUEUE_NAME, Config.SEARCH_INDEX_QUEUE, EmbeddingConfig.EMBEDDING_QUEUE]

    while running:
        try:
            job = claim_job(conn, MAIN_QUEUES)
            if job is None:
                time.sleep(Config.POLL_INTERVAL)
                continue

            job_id = job["id"]
            queue_name = job["queue_name"]
            payload = json.loads(job["payload"]) if isinstance(job["payload"], str) else job["payload"]

            try:
                if queue_name == Config.SEARCH_INDEX_QUEUE:
                    process_search_index(payload)
                    ack_job(conn, job_id)
                elif queue_name == EmbeddingConfig.EMBEDDING_QUEUE:
                    process_embedding(payload)
                    ack_job(conn, job_id)
                else:
                    process_job(conn, job)
                    ack_job(conn, job_id)

            except Exception as e:
                print(f"Job {job_id} failed: {e}")
                traceback.print_exc()
                if queue_name == Config.SEARCH_INDEX_QUEUE:
                    fail_job(conn, job_id, str(e), Config.SEARCH_INDEX_MAX_RETRIES,
                             Config.RETRY_DELAYS, Config.SEARCH_INDEX_DLQ)
                elif queue_name == EmbeddingConfig.EMBEDDING_QUEUE:
                    fail_job(conn, job_id, str(e), EmbeddingConfig.MAX_RETRIES,
                             Config.RETRY_DELAYS, EmbeddingConfig.EMBEDDING_DLQ)
                else:
                    fail_job(conn, job_id, str(e), Config.MAX_RETRIES,
                             Config.RETRY_DELAYS, Config.DEAD_LETTER_QUEUE)

        except psycopg2.OperationalError as e:
            print(f"PostgreSQL connection lost, reconnecting in 5s: {e}")
            time.sleep(5)
            try:
                conn = get_pg_conn()
            except Exception as ex:
                print(f"Reconnect failed: {ex}")
        except Exception as e:
            print(f"Unexpected error: {e}")
            traceback.print_exc()
            time.sleep(1)

    print("Worker stopped.")


if __name__ == "__main__":
    main()

