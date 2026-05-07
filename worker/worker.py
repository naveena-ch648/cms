"""Main worker process — consumes file:process Redis queue."""

import json
import signal
import sys
import time
import traceback

import redis
from botocore.exceptions import ClientError

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


running = True


def signal_handler(sig, frame):
    global running
    print("Shutting down gracefully...")
    running = False


def get_redis_client():
    return redis.Redis(host=Config.REDIS_HOST, port=Config.REDIS_PORT, decode_responses=True)


def process_job(r: redis.Redis, job_data: dict):
    file_id = job_data.get("fileId")
    org_id = job_data.get("organizationId")
    action = job_data.get("action", "process")
    mime_type = job_data.get("mimeType", "")
    storage_bucket = job_data.get("storageBucket", "")
    storage_key = job_data.get("storageKey", "")

    print(f"Processing file {file_id} (org={org_id}, action={action}, mime={mime_type})")

    try:
        _dispatch_action(action, mime_type, file_id, org_id, storage_bucket, storage_key, r)
    except ClientError as e:
        error_code = e.response.get("Error", {}).get("Code", "")
        if error_code in ("NoSuchKey", "NoSuchBucket", "404"):
            print(f"File not found in storage (bucket={storage_bucket}, key={storage_key}): {error_code} — skipping")
            return
        raise


def _dispatch_action(action, mime_type, file_id, org_id, storage_bucket, storage_key, r):
    if action == "preview":
        # Full preview generation based on mime type
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
            return

    elif action == "thumbnail":
        # Thumbnail-only generation
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
            return

    else:
        # Legacy action: run original thumbnail + metadata processors
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

    print(f"Worker starting — Redis at {Config.REDIS_HOST}:{Config.REDIS_PORT}")
    print(f"Listening on queues: {Config.QUEUE_NAME}, {Config.SEARCH_INDEX_QUEUE}, {EmbeddingConfig.EMBEDDING_QUEUE}")

    r = get_redis_client()

    while running:
        try:
            result = r.brpop([Config.QUEUE_NAME, Config.SEARCH_INDEX_QUEUE, EmbeddingConfig.EMBEDDING_QUEUE], timeout=5)
            if result is None:
                continue

            queue_name, payload = result
            job_data = json.loads(payload)

            if queue_name == Config.SEARCH_INDEX_QUEUE:
                # Search indexing job
                retry_count = job_data.get("_retries", 0)
                try:
                    process_search_index(job_data)
                except Exception as e:
                    print(f"Search index job failed: {e}")
                    traceback.print_exc()
                    if retry_count < Config.SEARCH_INDEX_MAX_RETRIES:
                        job_data["_retries"] = retry_count + 1
                        r.lpush(Config.SEARCH_INDEX_QUEUE, json.dumps(job_data))
                        print(f"Retrying search index job (attempt {retry_count + 1}/{Config.SEARCH_INDEX_MAX_RETRIES})")
                    else:
                        r.lpush(Config.SEARCH_INDEX_DLQ, json.dumps(job_data))
                        print(f"Search index job moved to DLQ after {Config.SEARCH_INDEX_MAX_RETRIES} failures")
            elif queue_name == EmbeddingConfig.EMBEDDING_QUEUE:
                # Embedding job
                retry_count = job_data.get("_retries", 0)
                try:
                    process_embedding(job_data)
                except Exception as e:
                    print(f"Embedding job failed: {e}")
                    traceback.print_exc()
                    if retry_count < EmbeddingConfig.MAX_RETRIES:
                        job_data["_retries"] = retry_count + 1
                        r.lpush(EmbeddingConfig.EMBEDDING_QUEUE, json.dumps(job_data))
                        print(f"Retrying embedding job (attempt {retry_count + 1}/{EmbeddingConfig.MAX_RETRIES})")
                    else:
                        r.lpush(EmbeddingConfig.EMBEDDING_DLQ, json.dumps(job_data))
                        print(f"Embedding job moved to DLQ after {EmbeddingConfig.MAX_RETRIES} failures")
            else:
                # File processing job
                retry_count = job_data.get("_retries", 0)
                try:
                    process_job(r, job_data)
                except Exception as e:
                    print(f"Job failed: {e}")
                    traceback.print_exc()

                    if retry_count < Config.MAX_RETRIES:
                        delay = Config.RETRY_DELAYS[retry_count]
                        job_data["_retries"] = retry_count + 1
                        time.sleep(delay)
                        r.lpush(Config.QUEUE_NAME, json.dumps(job_data))
                        print(f"Retrying job (attempt {retry_count + 1}/{Config.MAX_RETRIES}) after {delay}s")
                    else:
                        r.lpush(Config.DEAD_LETTER_QUEUE, json.dumps(job_data))
                        print(f"Job moved to dead letter queue after {Config.MAX_RETRIES} failures")

        except redis.ConnectionError:
            print("Redis connection lost, reconnecting in 5s...")
            time.sleep(5)
            r = get_redis_client()
        except Exception as e:
            print(f"Unexpected error: {e}")
            traceback.print_exc()
            time.sleep(1)

    print("Worker stopped.")


if __name__ == "__main__":
    main()

