"""AI Summarizer — generates document summaries using LLM."""

import json

from openai import OpenAI

from config import Config
from processors.ai_prompts import SUMMARIZATION_PROMPT
from processors.ai_tagger import extract_text, get_openai_client


def process_summarization(file_id, org_id, storage_bucket, storage_key, mime_type):
    """Generate a summary for a document using LLM."""
    text = extract_text(storage_bucket, storage_key, mime_type, max_chars=30000)
    if not text or len(text.strip()) < 50:
        return {
            "summary": "Summary not available for this file type.",
            "word_count": 0,
            "key_topics": [],
        }, 0.0

    # For very large documents, chunk and summarize
    if len(text) > 15000:
        return _summarize_large_document(text)

    return _summarize_text(text)


def _summarize_text(text):
    """Summarize a single text chunk."""
    client = get_openai_client()
    response = client.chat.completions.create(
        model=Config.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": SUMMARIZATION_PROMPT},
            {"role": "user", "content": f"Summarize this document:\n\n{text[:12000]}"},
        ],
        response_format={"type": "json_object"},
        temperature=0.3,
        max_tokens=1000,
    )

    result_text = response.choices[0].message.content
    result = json.loads(result_text)

    summary = result.get("summary", "")
    word_count = result.get("word_count", len(summary.split()))
    key_topics = result.get("key_topics", [])[:5]

    # Confidence based on text quality
    confidence = min(95.0, 60.0 + len(text) / 500)

    return {
        "summary": summary,
        "word_count": word_count,
        "key_topics": key_topics,
    }, round(confidence, 2)


def _summarize_large_document(text):
    """Summarize a large document by chunking and combining summaries."""
    chunk_size = 10000
    chunks = [text[i:i + chunk_size] for i in range(0, len(text), chunk_size)][:5]

    chunk_summaries = []
    client = get_openai_client()

    for i, chunk in enumerate(chunks):
        response = client.chat.completions.create(
            model=Config.OPENAI_MODEL,
            messages=[
                {"role": "system", "content": "Summarize this section of a larger document in 2-3 sentences. Respond in plain text."},
                {"role": "user", "content": chunk},
            ],
            temperature=0.3,
            max_tokens=200,
        )
        chunk_summaries.append(response.choices[0].message.content)

    # Combine chunk summaries into final summary
    combined = "\n".join(chunk_summaries)
    response = client.chat.completions.create(
        model=Config.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": SUMMARIZATION_PROMPT},
            {"role": "user", "content": f"Create a unified summary from these section summaries of a document:\n\n{combined}"},
        ],
        response_format={"type": "json_object"},
        temperature=0.3,
        max_tokens=1000,
    )

    result_text = response.choices[0].message.content
    result = json.loads(result_text)

    summary = result.get("summary", "")
    word_count = result.get("word_count", len(summary.split()))
    key_topics = result.get("key_topics", [])[:5]

    confidence = min(90.0, 55.0 + len(text) / 1000)

    return {
        "summary": summary,
        "word_count": word_count,
        "key_topics": key_topics,
    }, round(confidence, 2)
