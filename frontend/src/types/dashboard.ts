export interface DashboardSummary {
  recentFilesCount: number;
  unreadNotifications: number;
  pendingApprovals: number;
  storageUsedBytes: number;
  storageMaxBytes: number;
  storagePercentage: number;
  activeAlertsCount: number;
}

export interface RecentFile {
  id: string;
  name: string;
  mimeType: string;
  sizeBytes: number;
  workspaceId: string;
  workspaceName: string;
  folderId: string;
  folderPath: string;
  lastAccessedAt: string;
  updatedAt: string;
}

export type ActivityActionType =
  | 'FILE_UPLOADED'
  | 'FILE_DOWNLOADED'
  | 'FILE_SHARED'
  | 'FILE_MOVED'
  | 'FILE_DELETED'
  | 'FOLDER_CREATED'
  | 'COMMENT_ADDED'
  | 'APPROVAL_SUBMITTED'
  | 'APPROVAL_DECIDED'
  | 'WORKFLOW_TRANSITIONED';

export interface ActivityEvent {
  id: string;
  actorName: string;
  actionType: ActivityActionType;
  targetType: string;
  targetId: string;
  targetName: string;
  workspaceName: string;
  metadata: Record<string, string> | null;
  createdAt: string;
}

export interface SharedItem {
  id: string;
  fileName: string;
  fileId: string;
  sharedBy: string;
  sharedWith: string;
  sharedAt: string;
  expiresAt: string | null;
  type: 'SHARED_BY_ME' | 'SHARED_WITH_ME';
}

export type AlertType =
  | 'STORAGE_WARNING'
  | 'STORAGE_CRITICAL'
  | 'LINK_EXPIRING'
  | 'UPLOAD_FAILED'
  | 'SYSTEM_ANNOUNCEMENT';

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';

export interface Alert {
  id: string;
  alertType: AlertType;
  severity: AlertSeverity;
  title: string;
  message: string;
  targetType: string | null;
  targetId: string | null;
  createdAt: string;
}
