"""Webhook delivery worker — processes webhook:deliver queue from PostgreSQL."""

import hashlib
import hmac
import json
import time
from datetime import datetime

import psycopg2
import pymysql
import urllib.request
import urllib.error

from config import Config


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


def compute_signature(payload: str, secret: str) -> str:
    """Compute HMAC-SHA256 signature."""
    sig = hmac.new(secret.encode("utf-8"), payload.encode("utf-8"), hashlib.sha256).hexdigest()
    return f"sha256={sig}"


def deliver_webhook(delivery_data: dict, db):
    """Deliver a single webhook."""
    url = delivery_data["url"]
    payload = delivery_data["payload"]
    secret = delivery_data.get("secret")
    webhook_id = delivery_data["webhookId"]
    event_type = delivery_data["eventType"]
    event_id = delivery_data["eventId"]

    headers = {
        "Content-Type": "application/json",
        "X-Webhook-Event": event_type,
        "X-Webhook-Delivery": event_id,
        "User-Agent": "CMS-Webhook/1.0",
    }

    if secret:
        signature = compute_signature(payload, secret)
        headers["X-Webhook-Signature"] = signature

    start_time = time.time()
    response_status = None
    response_body = None
    response_time_ms = None
    status = "SUCCESS"
    error_message = None

    try:
        req = urllib.request.Request(
            url,
            data=payload.encode("utf-8"),
            headers=headers,
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            response_status = resp.status
            response_body = resp.read().decode("utf-8", errors="replace")[:2000]
            response_time_ms = int((time.time() - start_time) * 1000)

        if response_status >= 400:
            status = "FAILED"
            error_message = f"HTTP {response_status}"

    except urllib.error.HTTPError as e:
        response_status = e.code
        response_body = e.read().decode("utf-8", errors="replace")[:2000]
        response_time_ms = int((time.time() - start_time) * 1000)
        status = "FAILED"
        error_message = f"HTTP {e.code}: {e.reason}"

    except Exception as e:
        response_time_ms = int((time.time() - start_time) * 1000)
        status = "FAILED"
        error_message = str(e)[:500]

    # Record delivery in database
    try:
        with db.cursor() as cursor:
            cursor.execute(
                """INSERT INTO webhook_deliveries 
                   (webhook_id, event_type, event_id, payload, response_status, 
                    response_body, response_time_ms, attempt_number, status, 
                    error_message, delivered_at, created_at)
                   VALUES (%s, %s, %s, %s, %s, %s, %s, 1, %s, %s, %s, NOW())""",
                (webhook_id, event_type, event_id, payload, response_status,
                 response_body, response_time_ms, status, error_message,
                 datetime.utcnow() if status == "SUCCESS" else None)
            )

            # Update consecutive failures on webhook
            if status == "SUCCESS":
                cursor.execute(
                    "UPDATE webhooks SET consecutive_failures = 0 WHERE id = %s",
                    (webhook_id,)
                )
            else:
                cursor.execute(
                    "UPDATE webhooks SET consecutive_failures = consecutive_failures + 1 WHERE id = %s",
                    (webhook_id,)
                )
                # Disable webhook after 10 consecutive failures
                cursor.execute(
                    "UPDATE webhooks SET status = 'DISABLED' WHERE id = %s AND consecutive_failures >= 10",
                    (webhook_id,)
                )
    except Exception as e:
        print(f"[WEBHOOK] Failed to record delivery: {e}")

    return status


def run_webhook_worker(conn, running_flag):
    """Main loop for the webhook delivery worker."""
    print("[WEBHOOK] Delivery worker started, polling for deliveries...")
    db = get_db_connection()

    while running_flag():
        try:
            job = _claim_job(conn, ["webhook:deliver"])
            if job:
                job_id, _queue, payload, _retries = job
                try:
                    delivery_data = json.loads(payload)
                    event_type = delivery_data.get("eventType", "unknown")
                    url = delivery_data.get("url", "unknown")
                    print(f"[WEBHOOK] Delivering {event_type} to {url}")
                    deliver_webhook(delivery_data, db)
                    _ack_job(conn, job_id)
                except Exception as e:
                    print(f"[WEBHOOK] Delivery error: {e}")
                    _ack_job(conn, job_id)  # Don't retry webhook deliveries
            else:
                time.sleep(Config.POLL_INTERVAL)
        except psycopg2.OperationalError:
            print("[WEBHOOK] PG connection lost, reconnecting...")
            time.sleep(5)
            try:
                conn = _get_pg_conn()
            except Exception:
                pass
        except pymysql.err.OperationalError:
            print("[WEBHOOK] DB connection lost, reconnecting...")
            try:
                db.close()
            except Exception:
                pass
            db = get_db_connection()
        except Exception as e:
            print(f"[WEBHOOK] Error: {e}")
            time.sleep(1)


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
