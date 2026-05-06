export interface MentionDto {
  userId: string;
  name: string;
}

export interface Comment {
  id: string;
  content: string;
  author: {
    id: string;
    name: string;
    email: string;
  };
  parentId: string | null;
  replies: Comment[];
  mentions: MentionDto[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentRequest {
  content: string;
  parentId?: string;
}

export interface TaskItem {
  id: string;
  fileId: string;
  title: string;
  description: string | null;
  status: 'OPEN' | 'DONE';
  dueDate: string | null;
  overdue: boolean;
  creator: {
    id: string;
    name: string;
    email: string;
  };
  assignee: {
    id: string;
    name: string;
    email: string;
  };
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  status?: 'OPEN' | 'DONE';
  title?: string;
  description?: string;
  assigneeId?: string;
  dueDate?: string | null;
}

export interface NotificationItem {
  id: string;
  type: 'MENTION' | 'TASK_ASSIGNED' | 'TASK_COMPLETED';
  title: string;
  message: string | null;
  targetType: string;
  targetId: string;
  actor: {
    id: string;
    name: string;
  } | null;
  read: boolean;
  readAt: string | null;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface ActivityEvent {
  id: number;
  eventType: string;
  category: 'COMMENT' | 'TASK' | 'UPLOAD' | 'VERSION' | 'SHARE' | 'OTHER';
  actor: {
    id: string;
    name: string;
  } | null;
  description: string;
  details: string | null;
  createdAt: string;
}

export interface WorkspaceMember {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
