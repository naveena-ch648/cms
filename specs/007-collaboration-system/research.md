# Research: Collaboration System

**Feature**: 007-collaboration-system  
**Date**: 2026-05-06  
**Purpose**: Resolve design decisions and technology choices for the collaboration feature set.

---

## R1: Comment System Extension Strategy

**Question**: How to extend the existing Comment entity (from feature 006) to support folder-level comments, @mentions, and the enhanced collaboration use case?

**Decision**: Extend the existing `comments` table with a nullable `folder_id` column. Comments can target either a file OR a folder (one must be set). Mentions are extracted from comment content and stored in a separate `mentions` table for notification generation.

**Rationale**: 
- Reusing the existing table avoids data migration and maintains comment history from feature 006.
- A separate `mentions` table decouples notification logic from comment content parsing.
- The `folder_id` extension is minimal (nullable FK column).

**Alternatives Considered**:
- **Separate discussion table**: Rejected — would duplicate threading logic and complicate queries.
- **Mentions stored as JSON in comment**: Rejected — hard to query for "all mentions of user X" and index.

---

## R2: @Mention Parsing & Storage

**Question**: How to detect, store, and render @mentions within comment text?

**Decision**: Parse mentions on the backend when a comment is created. Pattern: `@[username]` or `@[userId]`. Store each mention as a row in the `mentions` table linking comment → mentioned user. The frontend renders mentions by scanning comment text for `@[...]` patterns and rendering them as highlighted spans.

**Rationale**:
- Backend parsing ensures mentions are always consistent regardless of client.
- Separate storage enables efficient "find all mentions for user X" queries for notifications.
- Frontend rendering from text patterns is simpler than rich-text/delta formats.

**Alternatives Considered**:
- **Rich text editor with mention nodes**: Rejected — over-engineered for v1, adds WYSIWYG complexity.
- **Client-side only parsing**: Rejected — unreliable, different clients could produce inconsistent mentions.

---

## R3: Notification System Design

**Question**: How to implement in-app notifications without WebSockets?

**Decision**: Notifications are persisted in a `notifications` table with type, recipient, content preview, target link, and read status. The frontend polls a count endpoint on interval (every 30 seconds) and fetches the full list on demand. Redis caches the unread count per user.

**Rationale**:
- Polling is explicitly acceptable per spec ("Real-time updates are out of scope for v1").
- Redis cache prevents repeated COUNT queries on the notifications table.
- Persisted notifications enable history viewing and "mark all as read" operations.

**Alternatives Considered**:
- **Server-Sent Events (SSE)**: Rejected — adds connection management complexity for v1.
- **WebSocket**: Rejected — explicitly out of scope per spec assumptions.
- **Redis pub/sub only (no persistence)**: Rejected — notifications must survive browser close and page refresh.

---

## R4: Task Model Design

**Question**: How to design the task entity to be lightweight yet functional?

**Decision**: Tasks are linked to a file (required), have a creator, an assignee (workspace member), optional due date, status (OPEN/DONE), and optional description. No sub-tasks, no dependencies, no priority levels beyond the presence of a due date.

**Rationale**:
- The spec explicitly states "this is not a full project management tool."
- Minimal fields reduce implementation time while covering all acceptance scenarios.
- The "My Tasks" view is simply a query filtered by assignee.

**Alternatives Considered**:
- **Priority field**: Rejected — adds complexity without clear spec requirement.
- **Multiple status options (TODO/IN_PROGRESS/DONE)**: Rejected — spec only requires open→done and reopen.

---

## R5: Activity Timeline Data Source

**Question**: Build a new activity events table or reuse the existing `audit_events` table?

**Decision**: Reuse the existing `audit_events` table (AuditEvent entity). Create an ActivityService that queries audit_events filtered by resource (file/folder) and event type. Add new event types for collaboration actions (COMMENT_CREATED, TASK_CREATED, TASK_COMPLETED, etc.).

**Rationale**:
- AuditEvent already captures file uploads, version changes, permission changes, and shares.
- Adding collaboration event types extends the existing pattern consistently.
- Avoids data duplication between audit and activity systems.

**Alternatives Considered**:
- **Separate activity_events table**: Rejected — duplicates existing audit infrastructure, creates maintenance burden.
- **Denormalized activity cache**: Rejected — premature optimization; audit_events with proper indexing suffices at spec scale.

---

## R6: Folder Discussion Implementation

**Question**: How to enable comments on folders when the existing comments table has a NOT NULL file_id?

**Decision**: Migration V007 alters the `comments` table to make `file_id` nullable and adds a `folder_id` column. A CHECK constraint ensures exactly one of file_id or folder_id is set (XOR). The existing Comment entity is extended with an optional Folder relationship.

**Rationale**:
- Reuses the same threading, mention, and notification infrastructure.
- The XOR constraint maintains data integrity at the database level.

**Alternatives Considered**:
- **Separate folder_comments table**: Rejected — duplicates threading logic.
- **Polymorphic target_type + target_id**: Rejected — loses FK integrity and complicates JPA mapping.

---

## R7: Comment-to-File Access Control

**Question**: What access level is required to comment/create tasks?

**Decision**: Any user with at least VIEWER access to the file/folder can read and create comments. Task creation requires at least VIEWER access. Task assignment is restricted to members of the same workspace. Comment deletion is restricted to the comment author or workspace admins.

**Rationale**:
- Matches spec FR-013: "at least viewer access to the file."
- Matches spec FR-014: "task assignment to members of the same workspace."
- Low barrier to commenting encourages collaboration.

**Alternatives Considered**:
- **EDITOR required to comment**: Rejected — too restrictive; viewers should be able to discuss.
- **Anyone in org can comment**: Rejected — violates folder/file permission isolation.
