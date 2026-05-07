"""AI Tagger & Classifier — uses LLM to generate tags and classify documents."""

import json

import boto3
from openai import OpenAI

from config import Config
from processors.ai_prompts import TAGGING_PROMPT, CLASSIFICATION_PROMPT


_openai_client = None
_s3_client = None


def get_openai_client():
    global _openai_client
    if _openai_client is None:
        _openai_client = OpenAI(api_key=Config.OPENAI_API_KEY)
    return _openai_client


def get_s3_client():
    global _s3_client
    if _s3_client is None:
        _s3_client = boto3.client(
            "s3",
            endpoint_url=Config.MINIO_ENDPOINT,
            aws_access_key_id=Config.MINIO_ACCESS_KEY,
            aws_secret_access_key=Config.MINIO_SECRET_KEY,
            region_name=Config.MINIO_REGION,
        )
    return _s3_client


def extract_text(storage_bucket, storage_key, mime_type, max_chars=15000):
    """Extract text content from a file stored in MinIO."""
    s3 = get_s3_client()
    try:
        response = s3.get_object(Bucket=storage_bucket, Key=storage_key)
        raw_bytes = response["Body"].read()

        if mime_type == "application/pdf":
            return _extract_pdf_text(raw_bytes, max_chars)
        elif mime_type in ("text/plain", "text/markdown", "text/csv", "application/json"):
            return raw_bytes.decode("utf-8", errors="replace")[:max_chars]
        elif "word" in mime_type or "document" in mime_type:
            return _extract_docx_text(raw_bytes, max_chars)
        else:
            # Attempt as plain text
            text = raw_bytes.decode("utf-8", errors="replace")[:max_chars]
            if len(text.strip()) < 50:
                return None
            return text
    except Exception as e:
        print(f"Text extraction failed: {e}")
        return None


def _extract_pdf_text(raw_bytes, max_chars):
    """Extract text from PDF bytes."""
    try:
        import fitz  # PyMuPDF
        doc = fitz.open(stream=raw_bytes, filetype="pdf")
        text = ""
        for page in doc:
            text += page.get_text()
            if len(text) >= max_chars:
                break
        doc.close()
        return text[:max_chars]
    except Exception as e:
        print(f"PDF extraction failed: {e}")
        return None


def _extract_docx_text(raw_bytes, max_chars):
    """Extract text from DOCX bytes using basic zip parsing."""
    try:
        import zipfile
        import io
        import xml.etree.ElementTree as ET

        zf = zipfile.ZipFile(io.BytesIO(raw_bytes))
        if "word/document.xml" not in zf.namelist():
            return None
        xml_content = zf.read("word/document.xml")
        tree = ET.fromstring(xml_content)
        ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        paragraphs = tree.findall(".//w:p", ns)
        text = "\n".join(
            "".join(node.text or "" for node in p.findall(".//w:t", ns))
            for p in paragraphs
        )
        return text[:max_chars]
    except Exception as e:
        print(f"DOCX extraction failed: {e}")
        return None


def process_tagging(file_id, org_id, storage_bucket, storage_key, mime_type):
    """Generate tags for a document using LLM."""
    text = extract_text(storage_bucket, storage_key, mime_type)
    if not text or len(text.strip()) < 20:
        return {"suggested_tags": [], "confidence_per_tag": {}}, 0.0

    client = get_openai_client()
    response = client.chat.completions.create(
        model=Config.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": TAGGING_PROMPT},
            {"role": "user", "content": f"Analyze this document and suggest relevant tags:\n\n{text[:8000]}"},
        ],
        response_format={"type": "json_object"},
        temperature=0.3,
        max_tokens=500,
    )

    result_text = response.choices[0].message.content
    result = json.loads(result_text)

    suggested_tags = result.get("tags", [])[:10]
    confidence_per_tag = result.get("confidence", {})

    # Normalize confidence values
    for tag in suggested_tags:
        if tag not in confidence_per_tag:
            confidence_per_tag[tag] = 75.0

    avg_confidence = sum(confidence_per_tag.get(t, 75.0) for t in suggested_tags) / max(len(suggested_tags), 1)

    return {
        "suggested_tags": suggested_tags,
        "confidence_per_tag": confidence_per_tag,
    }, round(avg_confidence, 2)


def process_classification(file_id, org_id, storage_bucket, storage_key, mime_type):
    """Classify a document into a category using LLM."""
    text = extract_text(storage_bucket, storage_key, mime_type)
    if not text or len(text.strip()) < 20:
        return {"category": "Other", "confidence": 0.0, "alternatives": []}, 0.0

    client = get_openai_client()
    response = client.chat.completions.create(
        model=Config.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": CLASSIFICATION_PROMPT},
            {"role": "user", "content": f"Classify this document:\n\n{text[:8000]}"},
        ],
        response_format={"type": "json_object"},
        temperature=0.2,
        max_tokens=300,
    )

    result_text = response.choices[0].message.content
    result = json.loads(result_text)

    category = result.get("category", "Other")
    confidence = result.get("confidence", 50.0)
    alternatives = result.get("alternatives", [])[:3]

    return {
        "category": category,
        "confidence": confidence,
        "alternatives": alternatives,
    }, round(confidence, 2)
