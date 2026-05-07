"""AI Workflow Recommendation — rule-based workflow mapping from document classification."""

import json

import pymysql

from config import Config


# Default workflow mappings based on document classification
DEFAULT_WORKFLOW_MAPPINGS = {
    "Contract": {
        "workflow_id": "contract-review",
        "workflow_name": "Contract Review & Approval",
        "description": "Route to legal team for review, then to signatories",
        "steps": ["Legal Review", "Manager Approval", "Signature Collection"],
    },
    "Invoice": {
        "workflow_id": "invoice-processing",
        "workflow_name": "Invoice Processing",
        "description": "Validate, approve, and queue for payment",
        "steps": ["Verification", "Budget Approval", "Payment Queue"],
    },
    "Report": {
        "workflow_id": "report-distribution",
        "workflow_name": "Report Distribution",
        "description": "Review and distribute to stakeholders",
        "steps": ["Quality Check", "Manager Review", "Distribution"],
    },
    "Policy": {
        "workflow_id": "policy-approval",
        "workflow_name": "Policy Review & Approval",
        "description": "Multi-stage approval for policy documents",
        "steps": ["Draft Review", "Legal Compliance", "Executive Approval", "Publishing"],
    },
    "Memo": {
        "workflow_id": "memo-acknowledgment",
        "workflow_name": "Memo Acknowledgment",
        "description": "Distribute and track acknowledgment",
        "steps": ["Review", "Distribution", "Acknowledgment Tracking"],
    },
    "Correspondence": {
        "workflow_id": "correspondence-response",
        "workflow_name": "Correspondence Response",
        "description": "Route to responsible party for response",
        "steps": ["Triage", "Assignment", "Response Draft", "Send"],
    },
}


def get_db_connection():
    return pymysql.connect(
        host=Config.MYSQL_HOST,
        port=Config.MYSQL_PORT,
        user=Config.MYSQL_USER,
        password=Config.MYSQL_PASSWORD,
        database=Config.MYSQL_DATABASE,
        cursorclass=pymysql.cursors.DictCursor,
    )


def process_workflow_recommendation(file_id, org_id):
    """Recommend a workflow based on the file's classification and tags."""
    # Get file classification from existing AI job results
    classification = _get_file_classification(file_id)
    org_mappings = _get_org_workflow_mappings(org_id)

    if not classification:
        return {
            "recommended": False,
            "reason": "No classification available yet",
        }, 0.0

    category = classification.get("category", "")
    mappings = org_mappings if org_mappings else DEFAULT_WORKFLOW_MAPPINGS

    if category in mappings:
        workflow = mappings[category]
        confidence = min(90.0, classification.get("confidence", 70.0) * 0.9)
        return {
            "recommended": True,
            "workflow_id": workflow.get("workflow_id", ""),
            "workflow_name": workflow.get("workflow_name", ""),
            "description": workflow.get("description", ""),
            "steps": workflow.get("steps", []),
            "reason": f"Based on document classification: {category}",
        }, round(confidence, 2)

    return {
        "recommended": False,
        "reason": f"No workflow mapped for category: {category}",
    }, 50.0


def _get_file_classification(file_id):
    """Get the most recent classification result for a file."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(
                """SELECT result, confidence FROM ai_jobs 
                   WHERE file_id = (SELECT id FROM files WHERE uuid = %s LIMIT 1)
                   AND type = 'CLASSIFY'
                   AND status = 'COMPLETED'
                   ORDER BY created_at DESC LIMIT 1""",
                (file_id,),
            )
            row = cursor.fetchone()
            if row and row["result"]:
                result = json.loads(row["result"])
                result["confidence"] = float(row["confidence"]) if row["confidence"] else 70.0
                return result
    finally:
        conn.close()
    return None


def _get_org_workflow_mappings(org_id):
    """Get organization-specific workflow mappings from ai_config."""
    conn = get_db_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute("SELECT ai_config FROM organizations WHERE id = %s", (org_id,))
            row = cursor.fetchone()
            if row and row["ai_config"]:
                config = json.loads(row["ai_config"])
                return config.get("workflow_mappings")
    finally:
        conn.close()
    return None
