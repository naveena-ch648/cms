# Implementation Plan: Collaboration System

**Branch**: `007-collaboration-system` | **Date**: 2026-05-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-collaboration-system/spec.md`

## Summary

Build a comprehensive collaboration layer enabling file/folder comments with threading, @mentions with in-app notifications, lightweight task management tied to files, and an activity timeline aggregating all file events. The system extends the existing Comment entity from feature 006, adds new entities (Task, Notification, Mention), and builds a cohesive frontend panel experience.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.5 backend), TypeScript 5.6 (React 18 frontend)  
**Primary Dependencies**: Spring Data JPA, Spring Security, Spring Data Redis, jjwt 0.12.6, React 18, React Router 6.28, Axios 1.7.7, Vite 6  
**Storage**: MySQL 8.0 (port 3307, root/root), Redis 7 (port 6379 for caching & notification counts)  
**Testing**: JUnit 5 (backend), Vitest (frontend)  
**Target Platform**: Docker-based web service (Linux containers)  
**Project Type**: Web application (REST API + SPA)  
**Performance Goals**: <200ms for standard API responses, <2s for comment posting (including notification dispatch)  
**Constraints**: No WebSocket for v1 (polling/manual refresh acceptable), max 2-level comment threading, 50 comments per page  
**Scale/Scope**: 500+ comments per file, 200 unread notifications per user, 1000+ activity events per file

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Notes |
|-----------|------------|-------|
| I. Code Quality | ✓ PASS | Modular services (CommentService, TaskService, NotificationService, ActivityService). Typed DTOs. Clear interfaces. |
| II. Testing Standards | ✓ PASS | Unit tests for services, integration tests for controllers, edge case coverage. |
| III. User Experience | ✓ PASS | Consistent panel UI, loading/error states, predictable navigation, structured API responses. |
| IV. Performance | ✓ PASS | Redis caching for notification counts, paginated APIs, indexed queries. |
| V. Reliability | ✓ PASS | Notification dispatch is async (tolerant of failures), cascade-delete for comments/tasks on file delete. |
| VI. Security | ✓ PASS | RBAC enforced (file access check before comment/task operations), author-only deletion, workspace-scoped task assignment. |
| VIII. Observability | ✓ PASS | Audit events for all actions, error logging. |
| IX. Developer Experience | ✓ PASS | Consistent patterns from prior features, clear API contracts. |

**GATE STATUS**: PASS — No violations. Proceeding to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/007-collaboration-system/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── comments-api.md
│   ├── tasks-api.md
│   ├── notifications-api.md
│   └── activity-api.md
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/cms/
│   ├── entity/
│   │   ├── Comment.java           (EXTEND - add folder support, mention markers)
│   │   ├── Task.java              (NEW)
│   │   ├── Notification.java      (NEW)
│   │   └── Mention.java           (NEW)
│   ├── repository/
│   │   ├── CommentRepository.java (EXTEND - add folder queries)
│   │   ├── TaskRepository.java    (NEW)
│   │   ├── NotificationRepository.java (NEW)
│   │   └── MentionRepository.java (NEW)
│   ├── dto/
│   │   └── collaboration/
│   │       ├── TaskDto.java       (NEW)
│   │       ├── NotificationDto.java (NEW)
│   │       └── ActivityEventDto.java (NEW)
│   ├── service/
│   │   ├── CommentService.java    (EXTEND - mentions, folder comments)
│   │   ├── TaskService.java       (NEW)
│   │   ├── NotificationService.java (NEW)
│   │   ├── MentionService.java    (NEW)
│   │   └── ActivityService.java   (NEW)
│   └── controller/
│       ├── CommentController.java (EXTEND - folder endpoints)
│       ├── TaskController.java    (NEW)
│       ├── NotificationController.java (NEW)
│       └── ActivityController.java (NEW)
├── src/main/resources/db/migration/
│   └── V007__collaboration_system.sql (NEW)

frontend/
├── src/
│   ├── api/
│   │   ├── comments.ts        (EXTEND)
│   │   ├── tasks.ts           (NEW)
│   │   ├── notifications.ts   (NEW)
│   │   └── activity.ts        (NEW)
│   ├── types/
│   │   └── collaboration.ts   (NEW)
│   ├── components/
│   │   └── collaboration/
│   │       ├── CommentPanel.tsx        (NEW - full-featured comment panel)
│   │       ├── CommentItem.tsx         (NEW)
│   │       ├── MentionInput.tsx        (NEW - textarea with @mention autocomplete)
│   │       ├── TaskPanel.tsx           (NEW)
│   │       ├── TaskItem.tsx            (NEW)
│   │       ├── TaskForm.tsx            (NEW)
│   │       ├── ActivityTimeline.tsx    (NEW)
│   │       ├── ActivityItem.tsx        (NEW)
│   │       ├── NotificationBell.tsx    (NEW)
│   │       ├── NotificationDropdown.tsx (NEW)
│   │       └── CollaborationSidebar.tsx (NEW - tabbed panel: comments/tasks/activity)
│   └── pages/
│       └── WorkspacePage.tsx   (MODIFY - integrate CollaborationSidebar)
```

**Structure Decision**: Web application pattern (frontend + backend). Extends existing code from features 005/006 rather than replacing. New entities under `entity/`, new DTOs under `dto/collaboration/`, dedicated controllers per domain.
