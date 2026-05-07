# Quickstart: AI Automation

**Feature**: 016-ai-automation  
**Prerequisites**: Docker containers running (cms-mysql, cms-redis, cms-minio, cms-backend, cms-worker, cms-frontend)

## Setup

### 1. Start services

```bash
cd docker && docker-compose up -d
```

### 2. Configure AI (Admin)

Navigate to Admin Console → AI Configuration and enable desired features. Or use the API:

```bash
curl -X PUT http://localhost:8080/api/v1/ai/config \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "enabledFeatures": ["TAG", "SUMMARIZE", "CLASSIFY", "DETECT_DUPLICATES", "DETECT_SENSITIVE", "RECOMMEND_WORKFLOW"],
    "confidenceThreshold": 70
  }'
```

### 3. Ensure OpenAI API key is configured

Set `OPENAI_API_KEY` environment variable in docker-compose.yml or `.env`.

## Verification Flow

### Auto-Tagging & Classification

1. Upload a document (e.g., a PDF contract)
2. Wait ~60 seconds for AI processing
3. Open file detail panel → AI Suggestions section
4. Verify suggested tags and category appear with confidence scores
5. Accept or reject suggestions

### Summarization

1. Upload a multi-page text document
2. Wait for processing to complete
3. View the generated summary in file detail panel
4. Click "Regenerate" to test re-analysis

### Duplicate Detection

1. Upload a document
2. Upload the same document again (or a slightly modified version)
3. Verify duplicate warning appears with link to original file

### Sensitive Data Detection

1. Upload a document containing test credit card numbers (e.g., `4111-1111-1111-1111`)
2. Verify sensitivity badge appears with detection details
3. Verify sharing restrictions are suggested

### Workflow Recommendations

1. Configure a workflow mapping (Admin → AI Config): `"Contract": "<workflow-id>"`
2. Upload a document that classifies as "Contract"
3. Verify workflow recommendation appears in file detail panel
4. Click "Apply" to submit to recommended workflow

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| No AI suggestions appear | Worker not running or OPENAI_API_KEY not set | Check worker logs; verify env var |
| "Processing pending" stuck | Redis connection issue or queue backlog | Check Redis connectivity; monitor queue length |
| Low confidence scores | Document has minimal text content | Expected for images/very short documents |
| Duplicate detection misses similar files | Embeddings not generated yet | Ensure embedding worker has processed both files |
