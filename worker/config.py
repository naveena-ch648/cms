import os


class Config:
    REDIS_HOST = os.environ.get("REDIS_HOST", "localhost")
    REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))

    MINIO_ENDPOINT = os.environ.get("MINIO_ENDPOINT", "http://localhost:9000")
    MINIO_ACCESS_KEY = os.environ.get("MINIO_ACCESS_KEY", "minioadmin")
    MINIO_SECRET_KEY = os.environ.get("MINIO_SECRET_KEY", "minioadmin")
    MINIO_REGION = os.environ.get("MINIO_REGION", "us-east-1")
    MINIO_USE_SSL = os.environ.get("MINIO_USE_SSL", "false").lower() == "true"

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
