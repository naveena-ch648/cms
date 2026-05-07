# Data Model: AI Automation

**Feature**: 016-ai-automation  
**Date**: 2026-05-06

## Entities

### AIJob

Tracks all AI processing tasks for files with their status, results, and audit trail.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files(id), NOT NULL | Target file |
| organization_id | BIGINT | FK → organizations(id), NOT NULL | Tenant isolation |
| type | ENUM | NOT NULL | AI task type: TAG, SUMMARIZE, CLASSIFY, DETECT_DUPLICATES, DETECT_SENSITIVE, RECOMMEND_WORKFLOW |
| status | ENUM | NOT NULL, DEFAULT 'PENDING' | PENDING, PROCESSING, COMPLETED, FAILED |
| result | JSON | NULL | Structured result (tags, summary, classification, duplicates, sensitivity labels, recommendations) |
| confidence | DECIMAL(5,2) | NULL | Overall confidence score (0.00-100.00) |
| retry_count | INT | NOT NULL, DEFAULT 0 | Number of retries attempted |
| error_message | TEXT | NULL | Error details on failure |
| triggered_by | ENUM | NOT NULL, DEFAULT 'SYSTEM' | SYSTEM (auto on upload) or USER (manual trigger) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Job creation time |
| started_at | TIMESTAMP | NULL | Processing start time |
| completed_at | TIMESTAMP | NULL | Processing completion time |

**Indexes**:
- `idx_ai_jobs_file_id` on (file_id)
- `idx_ai_jobs_org_status` on (organization_id, status)
- `idx_ai_jobs_file_type` on (file_id, type)

### Result JSON Schemas

#### TAG result
```json
{
  "suggested_tags": ["legal", "contract", "NDA"],
  "confidence_per_tag": {"legal": 95.0, "contract": 92.0, "NDA": 88.0}
}
```

#### CLASSIFY result
```json
{
  "category": "Contract",
  "confidence": 91.5,
  "alternatives": [
    {"category": "Policy", "confidence": 45.2},
    {"category": "Report", "confidence": 12.1}
  ]
}
```

#### SUMMARIZE result
```json
{
  "summary": "This document outlines...",
  "word_count": 187,
  "key_topics": ["terms of service", "data privacy", "liability"]
}
```

#### DETECT_DUPLICATES result
```json
{
  "exact_match": null,
  "near_duplicates": [
    {"file_id": "uuid-123", "file_name": "contract_v1.pdf", "similarity": 92.3}
  ]
}
```

#### DETECT_SENSITIVE result
```json
{
  "has_sensitive_data": true,
  "severity": "HIGH",
  "detections": [
    {"type": "CREDIT_CARD", "count": 2, "severity": "HIGH"},
    {"type": "EMAIL", "count": 5, "severity": "LOW"},
    {"type": "PHONE", "count": 3, "severity": "MEDIUM"}
  ]
}
```

#### RECOMMEND_WORKFLOW result
```json
{
  "recommended_workflow": "Legal Review",
  "reason": "Document classified as Contract — matches configured legal review workflow",
  "workflow_id": "uuid-456"
}
```

## Relationships

```
Organization (1) ──── (*) AIJob
FileEntity   (1) ──── (*) AIJob
FileEntity   (1) ──── (*) FileTag (existing — AI-accepted tags written here)
```

## State Transitions

### AIJob Status
```
PENDING → PROCESSING → COMPLETED
                     → FAILED → PENDING (retry, max 3)
                              → FAILED (permanent, moved to DLQ)
```

## Existing Entities Extended

### FileEntity (existing)
No schema changes. AI results reference files via `file_id` FK. The existing `tags` JSON column and `file_tags` table store accepted tags.

### Organization (existing)
Add `ai_config` JSON column to store per-org AI settings:

```json
{
  "enabled_features": ["TAG", "SUMMARIZE", "CLASSIFY", "DETECT_DUPLICATES", "DETECT_SENSITIVE", "RECOMMEND_WORKFLOW"],
  "confidence_threshold": 70,
  "sensitivity_patterns": {
    "custom_patterns": [{"name": "Employee ID", "pattern": "EMP-\\d{6}"}]
  },
  "workflow_mappings": {
    "Contract": "workflow-uuid-1",
    "Invoice": "workflow-uuid-2"
  }
}
```

## Redis Queues

| Queue | Purpose | Format |
|-------|---------|--------|
| `ai:process` | AI job processing | `{"jobId": "uuid", "fileId": "uuid", "orgId": 1, "type": "TAG", "storageKey": "...", "storageBucket": "...", "mimeType": "..."}` |
| `ai:process:dlq` | Failed AI jobs (after 3 retries) | Same format as above |

## Migration: V23__ai_automation.sql

```sql
CREATE TABLE ai_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    type ENUM('TAG', 'SUMMARIZE', 'CLASSIFY', 'DETECT_DUPLICATES', 'DETECT_SENSITIVE', 'RECOMMEND_WORKFLOW') NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    result JSON NULL,
    confidence DECIMAL(5,2) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    triggered_by ENUM('SYSTEM', 'USER') NOT NULL DEFAULT 'SYSTEM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_ai_jobs_file_id (file_id),
    INDEX idx_ai_jobs_org_status (organization_id, status),
    INDEX idx_ai_jobs_file_type (file_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE organizations ADD COLUMN ai_config JSON NULL;
```
