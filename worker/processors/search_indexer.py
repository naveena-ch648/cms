"""Search indexer processor — consumes search:index queue, indexes files into OpenSearch."""

import json
import traceback
from datetime import datetime, timezone

import pymysql
from opensearchpy import OpenSearch

from config import Config

# MIME type to file category mapping
MIME_TO_CATEGORY = {
    "application/pdf": "pdf",
    "image/jpeg": "image",
    "image/png": "image",
    "image/gif": "image",
    "image/webp": "image",
    "image/svg+xml": "image",
    "image/bmp": "image",
    "image/tiff": "image",
    "video/mp4": "video",
    "video/avi": "video",
    "video/quicktime": "video",
    "video/x-msvideo": "video",
    "video/webm": "video",
    "audio/mpeg": "audio",
    "audio/wav": "audio",
    "audio/ogg": "audio",
    "audio/flac": "audio",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "document",
    "application/msword": "document",
    "application/rtf": "document",
    "text/plain": "document",
    "text/markdown": "document",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "spreadsheet",
    "application/vnd.ms-excel": "spreadsheet",
    "text/csv": "spreadsheet",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation": "presentation",
    "application/vnd.ms-powerpoint": "presentation",
    "application/zip": "archive",
    "application/x-rar-compressed": "archive",
    "application/gzip": "archive",
    "application/x-tar": "archive",
    "application/x-7z-compressed": "archive",
}


def get_file_category(mime_type: str) -> str:
    """Map a MIME type to a user-friendly file category."""
    if mime_type in MIME_TO_CATEGORY:
        return MIME_TO_CATEGORY[mime_type]
    # Fallback heuristics
    if mime_type.startswith("image/"):
        return "image"
    if mime_type.startswith("video/"):
        return "video"
    if mime_type.startswith("audio/"):
        return "audio"
    if mime_type.startswith("text/"):
        return "document"
    return "other"


def get_db_connection():
    """Create a MySQL connection."""
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def get_opensearch_client():
    """Create an OpenSearch client."""
    return OpenSearch(
        hosts=[{"host": Config.OPENSEARCH_HOST, "port": Config.OPENSEARCH_PORT}],
        http_compress=True,
        use_ssl=False,
        verify_certs=False,
    )


def build_folder_path(cursor, folder_id: int) -> str:
    """Recursively build the full folder path from the folder hierarchy."""
    path_parts = []
    current_id = folder_id

    while current_id is not None:
        cursor.execute(
            "SELECT id, name, parent_id FROM folders WHERE id = %s", (current_id,)
        )
        folder = cursor.fetchone()
        if folder is None:
            break
        path_parts.insert(0, folder["name"])
        current_id = folder["parent_id"]

    return "/" + "/".join(path_parts) if path_parts else "/"


def fetch_file_metadata(file_uuid: str) -> dict | None:
    """Fetch file metadata from MySQL by UUID."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                SELECT f.uuid, f.name, f.mime_type, f.size_bytes, f.folder_id,
                       f.created_at, f.updated_at,
                       u.uuid as owner_uuid, u.first_name, u.last_name,
                       w.uuid as workspace_uuid,
                       fo.uuid as folder_uuid
                FROM files f
                JOIN users u ON f.uploaded_by = u.id
                JOIN workspaces w ON f.workspace_id = w.id
                JOIN folders fo ON f.folder_id = fo.id
                WHERE f.uuid = %s AND f.status = 'ACTIVE'
                """,
                (file_uuid,),
            )
            row = cursor.fetchone()
            if row is None:
                return None

            folder_path = build_folder_path(cursor, row["folder_id"])

            return {
                "fileUuid": row["uuid"],
                "fileName": row["name"],
                "content": "",  # Extracted text not available yet; indexed empty
                "fileType": get_file_category(row["mime_type"]),
                "mimeType": row["mime_type"],
                "ownerUuid": row["owner_uuid"],
                "ownerName": f"{row['first_name']} {row['last_name']}",
                "workspaceUuid": row["workspace_uuid"],
                "folderPath": folder_path,
                "folderUuid": row["folder_uuid"],
                "fileSize": row["size_bytes"],
                "createdAt": row["created_at"].isoformat() if row["created_at"] else None,
                "updatedAt": row["updated_at"].isoformat() if row["updated_at"] else None,
                "indexedAt": datetime.now(timezone.utc).isoformat(),
            }
    finally:
        conn.close()


def index_file(os_client: OpenSearch, file_uuid: str):
    """Index a file document into OpenSearch."""
    doc = fetch_file_metadata(file_uuid)
    if doc is None:
        print(f"[search_indexer] File not found or inactive: {file_uuid}, skipping index")
        return

    os_client.index(
        index=Config.OPENSEARCH_INDEX,
        id=file_uuid,
        body=doc,
    )
    print(f"[search_indexer] Indexed file: {file_uuid} ({doc['fileName']})")


def delete_file_from_index(os_client: OpenSearch, file_uuid: str):
    """Delete a file document from OpenSearch."""
    try:
        os_client.delete(index=Config.OPENSEARCH_INDEX, id=file_uuid)
        print(f"[search_indexer] Deleted from index: {file_uuid}")
    except Exception as e:
        if "NotFoundError" in type(e).__name__ or "404" in str(e):
            print(f"[search_indexer] File not in index (already deleted): {file_uuid}")
        else:
            raise


def process_search_index(job_data: dict):
    """Process a single search index event."""
    action = job_data.get("action")
    file_uuid = job_data.get("fileId")

    if not file_uuid:
        print("[search_indexer] Missing fileId in job data, skipping")
        return

    os_client = get_opensearch_client()

    if action == "index":
        index_file(os_client, file_uuid)
    elif action == "delete":
        delete_file_from_index(os_client, file_uuid)
    else:
        print(f"[search_indexer] Unknown action: {action}, skipping")
