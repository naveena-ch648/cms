"""AI Sensitive Data Detection — regex patterns + LLM contextual detection."""

import re

from processors.ai_tagger import extract_text


# Regex patterns for sensitive data types
SENSITIVE_PATTERNS = {
    "SSN": {
        "pattern": r'\b\d{3}-\d{2}-\d{4}\b',
        "severity": "HIGH",
    },
    "CREDIT_CARD": {
        "pattern": r'\b(?:4\d{3}|5[1-5]\d{2}|3[47]\d{2}|6(?:011|5\d{2}))[- ]?\d{4}[- ]?\d{4}[- ]?\d{4}\b',
        "severity": "HIGH",
    },
    "EMAIL": {
        "pattern": r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b',
        "severity": "LOW",
    },
    "PHONE": {
        "pattern": r'\b(?:\+?1[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b',
        "severity": "MEDIUM",
    },
    "PASSPORT": {
        "pattern": r'\b[A-Z]{1,2}\d{6,9}\b',
        "severity": "HIGH",
    },
    "IBAN": {
        "pattern": r'\b[A-Z]{2}\d{2}[A-Z0-9]{4}\d{7}(?:[A-Z0-9]?){0,16}\b',
        "severity": "HIGH",
    },
    "IP_ADDRESS": {
        "pattern": r'\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b',
        "severity": "LOW",
    },
    "AWS_KEY": {
        "pattern": r'\b(?:AKIA|ABIA|ACCA|ASIA)[0-9A-Z]{16}\b',
        "severity": "CRITICAL",
    },
    "PRIVATE_KEY": {
        "pattern": r'-----BEGIN (?:RSA |EC |DSA )?PRIVATE KEY-----',
        "severity": "CRITICAL",
    },
}

SEVERITY_ORDER = {"CRITICAL": 4, "HIGH": 3, "MEDIUM": 2, "LOW": 1, "NONE": 0}


def process_sensitive_detection(file_id, org_id, storage_bucket, storage_key, mime_type):
    """Scan document for sensitive data patterns."""
    text = extract_text(storage_bucket, storage_key, mime_type, max_chars=50000)
    if not text or len(text.strip()) < 10:
        return {
            "has_sensitive_data": False,
            "severity": "NONE",
            "detections": [],
        }, 0.0

    detections = []
    max_severity = "NONE"

    for data_type, config in SENSITIVE_PATTERNS.items():
        pattern = config["pattern"]
        severity = config["severity"]
        matches = re.findall(pattern, text)
        count = len(matches)

        if count > 0:
            detections.append({
                "type": data_type,
                "count": count,
                "severity": severity,
            })
            if SEVERITY_ORDER.get(severity, 0) > SEVERITY_ORDER.get(max_severity, 0):
                max_severity = severity

    has_sensitive = len(detections) > 0
    confidence = 95.0 if has_sensitive else 90.0  # High confidence for regex matches

    return {
        "has_sensitive_data": has_sensitive,
        "severity": max_severity,
        "detections": detections,
    }, round(confidence, 2)
