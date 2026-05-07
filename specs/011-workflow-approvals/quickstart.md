# Quickstart: Document Workflow & Approvals Engine

**Feature**: 011-workflow-approvals  
**Validates**: Migration, state transitions, approvals, triggers

---

## Prerequisites

- Backend running on port 8080
- MySQL running on port 3307 (root/root)
- Redis running on port 6379
- Existing workspace and users from prior features

---

## Step 1: Verify Migration

```bash
# Check that V18 migration applied (workflow tables exist)
mysql -h localhost -P 3307 -u root -proot cms -e "SHOW TABLES LIKE 'workflow%'; SHOW TABLES LIKE 'approval%';"
```

Expected: `workflow_transitions`, `workflow_triggers`, `approval_requests`, `approval_decisions` tables exist. Files table has `workflow_state` column.

---

## Step 2: Verify Default Workflow State

```bash
# Upload a file — it should get DRAFT state by default
curl -s http://localhost:8080/api/v1/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test.pdf" \
  -F "folderId=$FOLDER_ID" | jq '.data.workflowState'
```

Expected: `"DRAFT"`

---

## Step 3: Transition a Document

```bash
# Transition from DRAFT → REVIEW
curl -s -X POST http://localhost:8080/api/v1/files/$FILE_ID/workflow/transition \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetState": "REVIEW", "comment": "Ready for review"}' | jq '.data'
```

Expected: Response shows `fromState: "DRAFT"`, `toState: "REVIEW"`.

---

## Step 4: Verify Invalid Transition Blocked

```bash
# Try to skip from REVIEW → PUBLISHED (should fail)
curl -s -X POST http://localhost:8080/api/v1/files/$FILE_ID/workflow/transition \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetState": "PUBLISHED"}' | jq '.error'
```

Expected: Error message about invalid transition.

---

## Step 5: Submit for Approval

```bash
# Submit document for approval with 2 reviewers
curl -s -X POST http://localhost:8080/api/v1/files/$FILE_ID/approvals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"reviewerIds": ["$REVIEWER1_UUID", "$REVIEWER2_UUID"], "comment": "Please review"}' | jq '.data'
```

Expected: Approval request created with status "PENDING", two reviewers listed.

---

## Step 6: Reviewer Approves

```bash
# Reviewer 1 approves
curl -s -X POST http://localhost:8080/api/v1/approvals/$APPROVAL_ID/decisions \
  -H "Authorization: Bearer $REVIEWER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVED", "comment": "Looks good!"}' | jq '.data'

# Reviewer 2 approves (triggers auto-transition)
curl -s -X POST http://localhost:8080/api/v1/approvals/$APPROVAL_ID/decisions \
  -H "Authorization: Bearer $REVIEWER2_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"decision": "APPROVED", "comment": "Confirmed"}' | jq '.data'
```

Expected: After second approval, `approvalStatus` shows "APPROVED" and document auto-transitions to APPROVED state.

---

## Step 7: Verify Workflow History

```bash
curl -s http://localhost:8080/api/v1/files/$FILE_ID/workflow/history \
  -H "Authorization: Bearer $TOKEN" | jq '.data'
```

Expected: Shows transitions DRAFT→REVIEW and REVIEW→APPROVED with timestamps and actors.

---

## Step 8: Create a Trigger

```bash
curl -s -X POST http://localhost:8080/api/v1/workspaces/$WORKSPACE_ID/workflow-triggers \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Require metadata before publish",
    "triggerState": "PUBLISHED",
    "triggerType": "PREREQUISITE",
    "config": {"requireMetadataFields": ["department"]},
    "enabled": true
  }' | jq '.data'
```

Expected: Trigger created and active.

---

## Step 9: Verify Prerequisite Trigger Blocks Transition

```bash
# Try to transition to PUBLISHED without required metadata
curl -s -X POST http://localhost:8080/api/v1/files/$FILE_ID/workflow/transition \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"targetState": "PUBLISHED"}' | jq '.error'
```

Expected: Error about missing required metadata field "department".

---

## Step 10: Tag Autocomplete for Approvals (Frontend)

1. Navigate to workspace → select a file
2. Click workflow state badge → see available transitions
3. Click "Submit for Approval" → reviewer selection modal appears
4. Select reviewers → submit → see pending approval indicator
5. Switch to reviewer user → see "Pending Approvals" in dashboard
6. Click approve → document state updates automatically
