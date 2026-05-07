"""AI Dispatcher — consumes ai:process Redis queue and routes jobs to processors."""

import json
import time
import traceback
from datetime import datetime, timezone

import pymysql
import redis

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


def run_ai_worker(r: redis.Redis, is_running):
    """Main loop for AI worker — consumes from ai:process queue."""
    print(f"AI worker started — listening on queue: {Config.AI_QUEUE}")

    while is_running():
        try:
            result = r.brpop([Config.AI_QUEUE], timeout=5)
            if result is None:
                continue

            _, payload = result
            job_data = json.loads(payload)
            job_id = job_data.get("jobId", "unknown")
            retry_count = job_data.get("_retries", 0)

            try:
                process_ai_job(job_data)
            except Exception as e:
                print(f"AI job error: {e}")
                traceback.print_exc()

                if retry_count < Config.AI_MAX_RETRIES:
                    job_data["_retries"] = retry_count + 1
                    delay = min(5 * (2 ** retry_count), 60)
                    time.sleep(delay)
                    r.lpush(Config.AI_QUEUE, json.dumps(job_data))
                    print(f"Retrying AI job {job_id} (attempt {retry_count + 1}/{Config.AI_MAX_RETRIES}) after {delay}s")
                else:
                    r.lpush(Config.AI_DLQ, json.dumps(job_data))
                    print(f"AI job {job_id} moved to DLQ after {Config.AI_MAX_RETRIES} failures")

        except redis.ConnectionError:
            print("AI worker: Redis connection lost, reconnecting in 5s...")
            time.sleep(5)
        except Exception as e:
            print(f"AI worker unexpected error: {e}")
            traceback.print_exc()
            time.sleep(1)

    print("AI worker stopped.")
