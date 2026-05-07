"""Integration sync worker — processes import/export jobs from Redis queues."""

import json
import io
import time
import uuid
from datetime import datetime

import pymysql
import redis
import boto3
from botocore.config import Config as BotoConfig

from config import Config

# Google Drive API
try:
    from google.oauth2.credentials import Credentials
    from googleapiclient.discovery import build
    from googleapiclient.http import MediaIoBaseDownload, MediaIoBaseUpload
    HAS_GOOGLE_API = True
except ImportError:
    HAS_GOOGLE_API = False
    print("WARNING: google-api-python-client not installed. Import/export disabled.")


def get_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=Config.MINIO_ENDPOINT,
        aws_access_key_id=Config.MINIO_ACCESS_KEY,
        aws_secret_access_key=Config.MINIO_SECRET_KEY,
        region_name=Config.MINIO_REGION,
        config=BotoConfig(signature_version="s3v4"),
    )


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        cursorclass=pymysql.cursors.DictCursor,
        autocommit=True,
    )


def get_drive_service(access_token):
    if not HAS_GOOGLE_API:
        raise RuntimeError("google-api-python-client not installed")
    creds = Credentials(token=access_token)
    return build("drive", "v3", credentials=creds)


def process_import_job(r: redis.Redis, job_json: str):
    """Process a Google Drive import job."""
    job = json.loads(job_json)
    job_id = job["jobId"]
    org_id = job["organizationId"]
    user_id = job["userId"]
    drive_file_ids = job["driveFileIds"]
    target_folder_id = job["targetFolderId"]
    access_token = job["accessToken"]
    preserve_structure = job.get("preserveStructure", False)

    print(f"[IMPORT] Job {job_id}: {len(drive_file_ids)} files -> folder {target_folder_id}")

    # Update job status
    r.hset(f"integration:job:{job_id}", "status", "IN_PROGRESS")

    completed = 0
    failed = 0
    s3 = get_s3_client()
    db = get_db_connection()

    try:
        drive = get_drive_service(access_token)

        for drive_file_id in drive_file_ids:
            try:
                _import_single_file(drive, s3, db, drive_file_id, org_id, user_id, target_folder_id)
                completed += 1
                r.hset(f"integration:job:{job_id}", "completedItems", str(completed))
            except Exception as e:
                print(f"[IMPORT] Failed to import {drive_file_id}: {e}")
                failed += 1
                r.hset(f"integration:job:{job_id}", "failedItems", str(failed))

        status = "COMPLETED" if failed == 0 else "COMPLETED_WITH_ERRORS"
        r.hset(f"integration:job:{job_id}", "status", status)
        r.hset(f"integration:job:{job_id}", "completedAt", datetime.utcnow().isoformat())
        print(f"[IMPORT] Job {job_id} done: {completed} imported, {failed} failed")

    except Exception as e:
        print(f"[IMPORT] Job {job_id} failed: {e}")
        r.hset(f"integration:job:{job_id}", "status", "FAILED")
        r.hset(f"integration:job:{job_id}", "error", str(e))
    finally:
        db.close()


def _import_single_file(drive, s3, db, drive_file_id, org_id, user_id, target_folder_id):
    """Download a single file from Google Drive and store in MinIO + DB."""
    # Get file metadata from Drive
    file_meta = drive.files().get(
        fileId=drive_file_id,
        fields="id,name,mimeType,size,modifiedTime"
    ).execute()

    file_name = file_meta["name"]
    mime_type = file_meta.get("mimeType", "application/octet-stream")
    file_size = int(file_meta.get("size", 0))

    # Handle Google Docs native formats by exporting
    export_mime = _get_export_mime(mime_type)
    if export_mime:
        request = drive.files().export_media(fileId=drive_file_id, mimeType=export_mime)
        mime_type = export_mime
        file_name = _adjust_filename(file_name, export_mime)
    else:
        request = drive.files().get_media(fileId=drive_file_id)

    # Download file content
    buffer = io.BytesIO()
    downloader = MediaIoBaseDownload(buffer, request)
    done = False
    while not done:
        _, done = downloader.next_chunk()

    buffer.seek(0)
    file_size = buffer.getbuffer().nbytes

    # Upload to MinIO
    file_uuid = str(uuid.uuid4())
    bucket = "cms-files"
    storage_key = f"org-{org_id}/{file_uuid}/{file_name}"

    s3.put_object(
        Bucket=bucket,
        Key=storage_key,
        Body=buffer.getvalue(),
        ContentType=mime_type,
    )

    # Insert file record into DB
    with db.cursor() as cursor:
        cursor.execute(
            """INSERT INTO files (uuid, organization_id, folder_id, uploaded_by, name, 
               original_name, mime_type, size, storage_bucket, storage_key, status, created_at, updated_at)
               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, 'ACTIVE', NOW(), NOW())""",
            (file_uuid, org_id, target_folder_id, user_id, file_name,
             file_name, mime_type, file_size, bucket, storage_key)
        )

    print(f"[IMPORT] Imported: {file_name} ({file_size} bytes)")


def process_export_job(r: redis.Redis, job_json: str):
    """Process an export-to-Drive job."""
    job = json.loads(job_json)
    job_id = job["jobId"]
    org_id = job["organizationId"]
    file_ids = job["fileIds"]
    target_drive_folder_id = job["targetDriveFolderId"]
    access_token = job["accessToken"]
    conflict_strategy = job.get("conflictStrategy", "SKIP")

    print(f"[EXPORT] Job {job_id}: {len(file_ids)} files -> Drive folder {target_drive_folder_id}")

    r.hset(f"integration:job:{job_id}", "status", "IN_PROGRESS")

    completed = 0
    failed = 0
    s3 = get_s3_client()
    db = get_db_connection()

    try:
        drive = get_drive_service(access_token)

        for file_id in file_ids:
            try:
                _export_single_file(drive, s3, db, file_id, target_drive_folder_id, conflict_strategy)
                completed += 1
                r.hset(f"integration:job:{job_id}", "completedItems", str(completed))
            except Exception as e:
                print(f"[EXPORT] Failed to export {file_id}: {e}")
                failed += 1
                r.hset(f"integration:job:{job_id}", "failedItems", str(failed))

        status = "COMPLETED" if failed == 0 else "COMPLETED_WITH_ERRORS"
        r.hset(f"integration:job:{job_id}", "status", status)
        r.hset(f"integration:job:{job_id}", "completedAt", datetime.utcnow().isoformat())
        print(f"[EXPORT] Job {job_id} done: {completed} exported, {failed} failed")

    except Exception as e:
        print(f"[EXPORT] Job {job_id} failed: {e}")
        r.hset(f"integration:job:{job_id}", "status", "FAILED")
        r.hset(f"integration:job:{job_id}", "error", str(e))
    finally:
        db.close()


def _export_single_file(drive, s3, db, file_uuid, target_drive_folder_id, conflict_strategy):
    """Upload a single file from MinIO to Google Drive."""
    with db.cursor() as cursor:
        cursor.execute(
            "SELECT name, mime_type, size, storage_bucket, storage_key FROM files WHERE uuid = %s",
            (file_uuid,)
        )
        file_record = cursor.fetchone()

    if not file_record:
        raise ValueError(f"File not found: {file_uuid}")

    # Check for conflicts in target Drive folder
    existing = drive.files().list(
        q=f"name='{file_record['name']}' and '{target_drive_folder_id}' in parents and trashed=false",
        fields="files(id,name)"
    ).execute().get("files", [])

    if existing and conflict_strategy == "SKIP":
        print(f"[EXPORT] Skipping {file_record['name']} — already exists in Drive")
        return
    elif existing and conflict_strategy == "REPLACE":
        # Delete existing file
        drive.files().delete(fileId=existing[0]["id"]).execute()

    # Download from MinIO
    response = s3.get_object(Bucket=file_record["storage_bucket"], Key=file_record["storage_key"])
    content = response["Body"].read()

    # Upload to Drive
    file_metadata = {
        "name": file_record["name"],
        "parents": [target_drive_folder_id],
    }
    media = MediaIoBaseUpload(io.BytesIO(content), mimetype=file_record["mime_type"], resumable=True)
    drive.files().create(body=file_metadata, media_body=media, fields="id").execute()

    print(f"[EXPORT] Exported: {file_record['name']}")


def _get_export_mime(google_mime_type):
    """Convert Google Docs MIME types to exportable formats."""
    exports = {
        "application/vnd.google-apps.document": "application/pdf",
        "application/vnd.google-apps.spreadsheet": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.google-apps.presentation": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.google-apps.drawing": "image/png",
    }
    return exports.get(google_mime_type)


def _adjust_filename(name, export_mime):
    """Adjust filename extension for exported Google Docs."""
    extensions = {
        "application/pdf": ".pdf",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": ".xlsx",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation": ".pptx",
        "image/png": ".png",
    }
    ext = extensions.get(export_mime, "")
    if ext and not name.lower().endswith(ext):
        return name + ext
    return name


def run_integration_worker(r: redis.Redis, running_flag):
    """Main loop for the integration worker."""
    print("[INTEGRATION] Worker started, waiting for jobs...")
    while running_flag():
        # Check import queue
        job_json = r.rpop("integration:import")
        if job_json:
            try:
                process_import_job(r, job_json)
            except Exception as e:
                print(f"[INTEGRATION] Import job error: {e}")
            continue

        # Check export queue
        job_json = r.rpop("integration:export")
        if job_json:
            try:
                process_export_job(r, job_json)
            except Exception as e:
                print(f"[INTEGRATION] Export job error: {e}")
            continue

        # No jobs, wait briefly
        time.sleep(1)
