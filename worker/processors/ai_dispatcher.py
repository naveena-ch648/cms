"""AI Dispatcher — consumes ai:process queue from PostgreSQL and routes jobs to processors."""

import json
import time
import traceback
from datetime import datetime, timezone

import psycopg2
import psycopg2.extras
import pymysql

from config import Config


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        cursorclass=pymysql.cursors.DictCursor,
    )


def update_job_status(job_uuid, status, result=None, confidence=None, error_message=None):
    """Update AI job status in the database."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
            if status == "PROCESSING":
                cursor.execute(
                    "UPDATE ai_jobs SET status = %s, started_at = %s WHERE uuid = %s",
                    (status, now, job_uuid),
                )
            elif status == "COMPLETED":
                cursor.execute(
                    "UPDATE ai_jobs SET status = %s, result = %s, confidence = %s, completed_at = %s WHERE uuid = %s",
                    (status, json.dumps(result) if result else None, confidence, now, job_uuid),
                )
            elif status == "FAILED":
                cursor.execute(
                    "UPDATE ai_jobs SET status = %s, error_message = %s, retry_count = retry_count + 1 WHERE uuid = %s",
                    (status, error_message, job_uuid),
                )
            conn.commit()
    finally:
        conn.close()


def get_job_retry_count(job_uuid):
    """Get current retry count for a job."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT retry_count FROM ai_jobs WHERE uuid = %s", (job_uuid,))
            row = cursor.fetchone()
            return row["retry_count"] if row else 0
    finally:
        conn.close()


def process_ai_job(job_data):
    """Route an AI job to the appropriate processor."""
    job_id = job_data.get("jobId")
    job_type = job_data.get("type")
    file_id = job_data.get("fileId")
    org_id = job_data.get("orgId")
    storage_key = job_data.get("storageKey")
    storage_bucket = job_data.get("storageBucket")
    mime_type = job_data.get("mimeType")

    print(f"Processing AI job: id={job_id}, type={job_type}, file={file_id}")

    update_job_status(job_id, "PROCESSING")

    try:
        result = None
        confidence = None

        if job_type == "TAG":
            from processors.ai_tagger import process_tagging
            result, confidence = process_tagging(file_id, org_id, storage_bucket, storage_key, mime_type)

        elif job_type == "CLASSIFY":
            from processors.ai_tagger import process_classification
            result, confidence = process_classification(file_id, org_id, storage_bucket, storage_key, mime_type)

        elif job_type == "SUMMARIZE":
            from processors.ai_summarizer import process_summarization
            result, confidence = process_summarization(file_id, org_id, storage_bucket, storage_key, mime_type)

        elif job_type == "DETECT_DUPLICATES":
            from processors.ai_duplicates import process_duplicate_detection
            result, confidence = process_duplicate_detection(file_id, org_id, storage_bucket, storage_key)

        elif job_type == "DETECT_SENSITIVE":
            from processors.ai_sensitive import process_sensitive_detection
            result, confidence = process_sensitive_detection(file_id, org_id, storage_bucket, storage_key, mime_type)

        elif job_type == "RECOMMEND_WORKFLOW":
            from processors.ai_workflow import process_workflow_recommendation
            result, confidence = process_workflow_recommendation(file_id, org_id)

        else:
            raise ValueError(f"Unknown AI job type: {job_type}")

        update_job_status(job_id, "COMPLETED", result=result, confidence=confidence)
        print(f"AI job completed: id={job_id}, type={job_type}, confidence={confidence}")

    except Exception as e:
        error_msg = str(e)
        print(f"AI job failed: id={job_id}, type={job_type}, error={error_msg}")
        traceback.print_exc()
        update_job_status(job_id, "FAILED", error_message=error_msg)
        raise


def run_ai_worker(conn, is_running):
    """Main loop for AI worker — polls ai:process queue in PostgreSQL."""
    print(f"AI worker started — polling queue: {Config.AI_QUEUE}")

    while is_running():
        try:
            job = _claim_job(conn, [Config.AI_QUEUE])
            if job is None:
                time.sleep(Config.POLL_INTERVAL)
                continue

            job_id, queue_name, payload, retry_count = job
            try:
                job_data = json.loads(payload)
                process_ai_job(job_data)
                _ack_job(conn, job_id)
            except Exception as e:
                print(f"AI job error: {e}")
                traceback.print_exc()
                _fail_job(conn, job_id, str(e), Config.AI_MAX_RETRIES, Config.AI_QUEUE, Config.AI_DLQ)

        except psycopg2.OperationalError:
            print("AI worker: PostgreSQL connection lost, reconnecting in 5s...")
            time.sleep(5)
            try:
                conn = _get_pg_conn()
            except Exception:
                pass
        except Exception as e:
            print(f"AI worker unexpected error: {e}")
            traceback.print_exc()
            time.sleep(1)

    print("AI worker stopped.")


def _get_pg_conn():
    return psycopg2.connect(
        host=Config.PG_HOST,
        port=Config.PG_PORT,
        dbname=Config.PG_DB,
        user=Config.PG_USER,
        password=Config.PG_PASSWORD,
    )


def _claim_job(conn, queue_names):
    with conn.cursor() as cur:
        cur.execute(
            """UPDATE job_queue SET status='PROCESSING', updated_at=NOW()
               WHERE id = (
                   SELECT id FROM job_queue
                   WHERE queue_name = ANY(%s) AND status='PENDING'
                     AND (next_attempt_at IS NULL OR next_attempt_at <= NOW())
                   ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED
               ) RETURNING id, queue_name, payload, retry_count""",
            (queue_names,),
        )
        row = cur.fetchone()
        conn.commit()
        return row


def _ack_job(conn, job_id):
    with conn.cursor() as cur:
        cur.execute("DELETE FROM job_queue WHERE id = %s", (job_id,))
    conn.commit()


def _fail_job(conn, job_id, error_msg, max_retries, queue_name, dlq_name):
    with conn.cursor() as cur:
        cur.execute("SELECT retry_count FROM job_queue WHERE id = %s", (job_id,))
        row = cur.fetchone()
        if not row:
            conn.commit()
            return
        retry_count = row[0] + 1
        if retry_count < max_retries:
            delay = min(5 * (2 ** retry_count), 300)
            cur.execute(
                """UPDATE job_queue SET status='PENDING', retry_count=%s, error_msg=%s,
                   next_attempt_at=NOW() + (%s || ' seconds')::interval WHERE id=%s""",
                (retry_count, error_msg[:2000], str(delay), job_id),
            )
        else:
            cur.execute(
                """UPDATE job_queue SET queue_name=%s, status='PENDING', retry_count=%s,
                   error_msg=%s, next_attempt_at=NULL WHERE id=%s""",
                (dlq_name, retry_count, error_msg[:2000], job_id),
            )
    conn.commit()
