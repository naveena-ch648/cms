import os


class Config:
    # PostgreSQL (replaces Redis + MinIO)
    PG_HOST = os.environ.get("PG_HOST", "localhost")
    PG_PORT = int(os.environ.get("PG_PORT", 5433))
    PG_DB = os.environ.get("PG_DB", "cms_app")
    PG_USER = os.environ.get("PG_USER", "cmsuser")
    PG_PASSWORD = os.environ.get("PG_PASSWORD", "cmspassword")

    MYSQL_HOST = os.environ.get("MYSQL_HOST", "localhost")
    MYSQL_PORT = int(os.environ.get("MYSQL_PORT", 3306))
    MYSQL_DATABASE = os.environ.get("MYSQL_DATABASE", "cms")
    MYSQL_USER = os.environ.get("MYSQL_USER", "cmsuser")
    MYSQL_PASSWORD = os.environ.get("MYSQL_PASSWORD", "cmspassword")

    WORKER_CONCURRENCY = int(os.environ.get("WORKER_CONCURRENCY", 2))
    QUEUE_NAME = "file:process"
    DEAD_LETTER_QUEUE = "file:process:dead"
    MAX_RETRIES = 3
    RETRY_DELAYS = [5, 25, 125]  # seconds
    POLL_INTERVAL = float(os.environ.get("POLL_INTERVAL", 2.0))  # seconds between DB polls

    OPENSEARCH_HOST = os.environ.get("OPENSEARCH_HOST", "localhost")
    OPENSEARCH_PORT = int(os.environ.get("OPENSEARCH_PORT", 9200))
    OPENSEARCH_INDEX = "cms_files"
    SEARCH_INDEX_QUEUE = "search:index"
    SEARCH_INDEX_DLQ = "search:index:dlq"
    SEARCH_INDEX_MAX_RETRIES = 3

    # AI Automation
    OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY", "")
    OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
    AI_QUEUE = "ai:process"
    AI_DLQ = "ai:process:dlq"
    AI_MAX_RETRIES = 3
    AI_CONFIDENCE_THRESHOLD = int(os.environ.get("AI_CONFIDENCE_THRESHOLD", 70))
