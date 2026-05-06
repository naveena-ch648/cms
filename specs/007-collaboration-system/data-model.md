# Data Model: Collaboration System

**Feature**: 007-collaboration-system  
**Date**: 2026-05-06

---

## Entity Relationship Overview

```
User ──┬── Comment (author) ──── Mention (mentioned user)
       ├── Task (creator/assignee)
       ├── Notification (recipient)
       └── AuditEvent (actor)

File ──┬── Comment (file-level discussion)
       └── Task (linked file)

Folder ── Comment (folder-level discussion)

Comment ── Comment (parent → replies, max 2 levels)
```

---

## Entities

### Comment (EXTENDED from feature 006)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files(id), NULLABLE | Target file (XOR with folder_id) |
| folder_id | BIGINT | FK → folders(id), NULLABLE | Target folder (XOR with file_id) |
| user_id | BIGINT | FK → users(id), NOT NULL | Comment author |
| parent_id | BIGINT | FK → comments(id), NULLABLE | Parent for threading |
| content | TEXT | NOT NULL, max 5000 chars | Comment body (may contain @[userId] markers) |
| created_at | DATETIME | NOT NULL, auto | Creation timestamp |
| updated_at | DATETIME | NOT NULL, auto | Last update timestamp |

**Validation Rules**:
- Exactly one of file_id or folder_id must be non-null (XOR constraint)
- Threading depth: max 2 levels (parent can have replies, replies cannot have replies)
- Content: 1–5000 characters

**Indexes**: (file_id, created_at), (folder_id, created_at), (parent_id), (user_id)

---

### Mention (NEW)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| comment_id | BIGINT | FK → comments(id), NOT NULL, ON DELETE CASCADE | Source comment |
| mentioned_user_id | BIGINT | FK → users(id), NOT NULL, ON DELETE CASCADE | The @mentioned user |
| created_at | DATETIME | NOT NULL, auto | When mention was created |

**Validation Rules**:
- A user cannot be mentioned more than once in the same comment (UNIQUE: comment_id + mentioned_user_id)
- Self-mentions are allowed but do not generate notifications

**Indexes**: (mentioned_user_id, created_at DESC), (comment_id)

---

### Task (NEW)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| file_id | BIGINT | FK → files(id), NOT NULL, ON DELETE CASCADE | Linked file |
| creator_id | BIGINT | FK → users(id), NOT NULL | Who created the task |
| assignee_id | BIGINT | FK → users(id), NOT NULL | Who is assigned |
| title | VARCHAR(255) | NOT NULL | Task title |
| description | TEXT | NULLABLE | Optional details |
| status | ENUM('OPEN','DONE') | NOT NULL, DEFAULT 'OPEN' | Current status |
| due_date | DATE | NULLABLE | Optional deadline |
| completed_at | DATETIME | NULLABLE | When status changed to DONE |
| created_at | DATETIME | NOT NULL, auto | Creation timestamp |
| updated_at | DATETIME | NOT NULL, auto | Last update timestamp |

**Validation Rules**:
- Title: 1–255 characters
- Description: max 2000 characters
- Assignee must be a member of the same workspace as the file
- Status transitions: OPEN → DONE, DONE → OPEN (reopen)

**State Transitions**:
```
OPEN ──(complete)──→ DONE
DONE ──(reopen)───→ OPEN
```

**Indexes**: (file_id, status), (assignee_id, status, due_date), (creator_id)

---

### Notification (NEW)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | BIGINT | PK, AUTO_INCREMENT | Internal ID |
| uuid | CHAR(36) | UNIQUE, NOT NULL | Public identifier |
| recipient_id | BIGINT | FK → users(id), NOT NULL, ON DELETE CASCADE | Who receives it |
| type | ENUM('MENTION','TASK_ASSIGNED','TASK_COMPLETED') | NOT NULL | Notification category |
| title | VARCHAR(255) | NOT NULL | Display title |
| message | VARCHAR(500) | NULLABLE | Preview text |
| target_type | VARCHAR(50) | NOT NULL | Target entity type (FILE, FOLDER, TASK) |
| target_id | VARCHAR(36) | NOT NULL | UUID of target entity |
| actor_id | BIGINT | FK → users(id), NULLABLE | Who triggered it |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE | Read status |
| read_at | DATETIME | NULLABLE | When marked as read |
| created_at | DATETIME | NOT NULL, auto | Creation timestamp |

**Validation Rules**:
- Notifications for self-actions are not generated (actor_id ≠ recipient_id)
- Max 200 unread notifications displayed; older ones still queryable

**Indexes**: (recipient_id, is_read, created_at DESC), (target_type, target_id)

---

### AuditEvent (EXISTING — extended usage)

No schema changes. New event types added:

| Event Type | Resource Type | Description |
|------------|--------------|-------------|
| COMMENT_CREATED | Comment | User posted a comment |
| COMMENT_DELETED | Comment | User deleted a comment |
| TASK_CREATED | Task | User created a task |
| TASK_COMPLETED | Task | Task marked as done |
| TASK_REOPENED | Task | Task reopened |
| MENTION_CREATED | Mention | User mentioned someone |

The activity timeline queries `audit_events` filtered by file/folder resource.

---

## Migration: V007__collaboration_system.sql

```sql
-- Extend comments table for folder discussions
ALTER TABLE comments 
    MODIFY COLUMN file_id BIGINT NULL,
    ADD COLUMN folder_id BIGINT NULL AFTER file_id,
    ADD CONSTRAINT fk_comment_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    ADD INDEX idx_comment_folder (folder_id, created_at);

-- Mentions table
CREATE TABLE mentions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mention_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_mention_user FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_mention_comment_user (comment_id, mentioned_user_id),
    INDEX idx_mention_user (mentioned_user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tasks table
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM('OPEN', 'DONE') NOT NULL DEFAULT 'OPEN',
    due_date DATE,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_task_file_status (file_id, status),
    INDEX idx_task_assignee (assignee_id, status, due_date),
    INDEX idx_task_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL,
    type ENUM('MENTION', 'TASK_ASSIGNED', 'TASK_COMPLETED') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500),
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    actor_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notification_recipient (recipient_id, is_read, created_at DESC),
    INDEX idx_notification_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
