# Research: AI Automation

**Feature**: 016-ai-automation  
**Date**: 2026-05-06

## R1: AI Processing Pipeline Architecture

**Decision**: Extend existing Redis queue pattern with a dedicated `ai:process` queue. AI workers run as additional threads in the existing worker process, consuming jobs after file upload completes.

**Rationale**: The existing worker architecture already handles file processing via Redis queues with retry logic and DLQ. Adding another queue for AI processing maintains consistency and avoids introducing new infrastructure. AI jobs are enqueued by the backend after the standard file processing completes.

**Alternatives considered**:
- Separate microservice for AI processing: Rejected — adds deployment complexity without proportional benefit for current scale.
- Inline processing during upload: Rejected — violates the requirement that AI processing must not block file access.
- Celery/RQ instead of Redis lists: Rejected — existing pattern uses raw Redis BRPOP; switching frameworks adds unnecessary dependency.

## R2: LLM Integration for Tagging & Summarization

**Decision**: Use the existing configurable LLM provider (OpenAI API via `OPENAI_API_KEY`) for auto-tagging, classification, and summarization. The Python worker calls the OpenAI API with structured prompts that return JSON-formatted results.

**Rationale**: The project already has OpenAI API integration configured for the RAG Q&A feature. Reusing the same provider avoids additional vendor dependencies. Structured JSON output with function calling ensures parseable results.

**Alternatives considered**:
- Local LLM (Ollama/llama.cpp): Rejected — higher resource requirements for dev/Docker environments; can be added later as alternative provider.
- spaCy/NLTK-only approach: Rejected — insufficient for quality summarization and nuanced classification compared to LLM.
- Pre-trained classifiers (fine-tuned BERT): Rejected — requires training data and model management; LLM zero-shot approach provides acceptable accuracy without training overhead.

## R3: Duplicate Detection Strategy

**Decision**: Use a two-tier approach: (1) exact duplicate detection via SHA-256 checksum (already stored on FileEntity), and (2) near-duplicate detection via cosine similarity of document embeddings already stored in Qdrant (from feature 009-ai-document-qa).

**Rationale**: SHA-256 checksums are already computed during upload. For semantic similarity, the existing Qdrant embeddings collection (`document_chunks`) contains document vectors. Querying Qdrant for similar vectors avoids building a separate fingerprinting system.

**Alternatives considered**:
- SimHash/MinHash: Rejected — requires additional infrastructure; existing embeddings already capture semantic similarity.
- File-level MD5 only: Rejected — catches only exact duplicates, misses reformatted/slightly-edited versions.
- Dedicated fingerprint collection in Qdrant: Rejected — document_chunks collection already provides similarity search; adding a separate collection is redundant.

## R4: Sensitive Data Detection Approach

**Decision**: Use regex-based pattern matching for structured PII (SSN, credit cards, phone numbers, emails, passport numbers) combined with LLM-based entity recognition for unstructured sensitive content (health records, financial account details). Patterns are configurable per organization.

**Rationale**: Regex provides high-precision, fast detection for well-defined patterns (credit card Luhn validation, SSN format). LLM augments with contextual understanding for ambiguous cases. This hybrid approach balances speed with accuracy.

**Alternatives considered**:
- Regex-only: Rejected — misses contextual sensitive data (e.g., health conditions mentioned in prose).
- LLM-only: Rejected — slower and more expensive for high-volume scanning of well-defined patterns.
- Presidio (Microsoft): Rejected — additional dependency; regex + LLM achieves similar results with existing infrastructure.

## R5: Workflow Recommendation Engine

**Decision**: Rule-based matching using document classification + organization workflow configuration. If a document is classified as a type that maps to a configured workflow, suggest that workflow. Historical pattern matching is deferred to a later iteration.

**Rationale**: The spec describes pattern-based recommendations, but a rule-based approach (classification → workflow mapping) provides predictable, explainable results for v1. Historical pattern matching requires sufficient usage data that won't exist initially.

**Alternatives considered**:
- ML-based collaborative filtering: Rejected — requires training data from user behavior that doesn't exist yet.
- Only historical patterns: Rejected — cold-start problem with new installations.
- No recommendations (manual only): Rejected — spec explicitly requires this capability.

## R6: Job Queue Design

**Decision**: Single Redis queue `ai:process` with job types discriminated by a `type` field (TAG, SUMMARIZE, DETECT_DUPLICATES, DETECT_SENSITIVE, RECOMMEND_WORKFLOW). Jobs are enqueued after standard file processing completes. DLQ at `ai:process:dlq`.

**Rationale**: Using a single queue simplifies worker management while the type field allows the worker to route to the appropriate processor. This matches the existing `file:process` queue pattern.

**Alternatives considered**:
- Separate queues per AI task: Rejected — complicates worker configuration; sequential processing of related tasks is simpler.
- Fan-out (publish one job → multiple processors): Rejected — some tasks depend on others (e.g., classification before workflow recommendation); sequential processing ensures ordering.

## R7: Storage of AI Results

**Decision**: Store AI analysis results in a new `ai_jobs` MySQL table that records job type, status, results (JSON), confidence scores, and file reference. Tags are written to the existing `file_tags` table. Summary and classification stored as file metadata via the existing metadata system.

**Rationale**: Centralizing AI job tracking in one table provides audit trail and retry capability. Writing accepted results to existing tables (file_tags, metadata) ensures they integrate with existing search and filtering.

**Alternatives considered**:
- Store only in file_tags/metadata (no ai_jobs table): Rejected — loses processing history, retry state, and confidence scores.
- Separate tables per AI feature: Rejected — over-normalized; one table with type discrimination is sufficient.

## R8: Admin Configuration

**Decision**: Add an `ai_config` column (JSON) to the organization table to store per-organization AI feature toggles and settings (enabled features, confidence thresholds, sensitivity patterns). Exposed via existing admin APIs.

**Rationale**: Per-organization JSON config avoids creating additional tables for what is essentially a settings blob. The admin console already manages organization-level settings.

**Alternatives considered**:
- Separate ai_config table: Rejected — over-engineering for a settings blob.
- Environment variables only: Rejected — doesn't support per-organization customization.
- Feature flags service: Rejected — adds infrastructure; JSON column is sufficient for current needs.
