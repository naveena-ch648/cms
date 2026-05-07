import apiClient from './client';

export interface IntegrationConnection {
  id: string;
  provider: string;
  providerAccountId: string;
  status: string;
  connectedAt: string;
  lastUsedAt: string | null;
}

export interface DriveItem {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  modifiedTime: string;
  isFolder: boolean;
  iconLink?: string;
}

export interface DriveBrowseResponse {
  items: DriveItem[];
  nextPageToken: string | null;
}

export interface ImportJobResponse {
  jobId: string;
  status: string;
  totalItems: number;
  message: string;
}

export interface ExportJobResponse {
  jobId: string;
  status: string;
  totalItems: number;
  message: string;
}

export interface JobStatus {
  id: string;
  type: string;
  status: string;
  totalItems: number;
  completedItems: number;
  failedItems: number;
  errors: string[];
  startedAt: string;
  completedAt: string | null;
}

export interface SyncLink {
  id: string;
  folderId: string;
  folderName?: string;
  externalFolderName: string;
  provider: string;
  direction: string;
  syncIntervalMinutes: number;
  status: string;
  lastSyncAt: string | null;
  nextSyncAt: string | null;
}

export interface SyncJob {
  id: string;
  status: string;
  direction: string;
  itemsSynced: number;
  itemsFailed: number;
  itemsConflicted: number;
  bytesTransferred: number;
  startedAt: string;
  completedAt: string | null;
}

const BASE = '/integrations';

export const integrationsApi = {
  // OAuth
  connectGoogleDrive: () =>
    apiClient.get<{ data: { authorizationUrl: string } }>(`${BASE}/google-drive/connect`).then(r => r.data.data),

  getConnections: () =>
    apiClient.get<{ data: IntegrationConnection[] }>(`${BASE}/connections`).then(r => r.data.data),

  disconnectConnection: (connectionId: string) =>
    apiClient.delete<{ data: { status: string } }>(`${BASE}/connections/${connectionId}`).then(r => r.data.data),

  // Browse Drive
  browseDrive: (params?: { folderId?: string; query?: string; pageToken?: string }) =>
    apiClient.get<{ data: DriveBrowseResponse }>(`${BASE}/google-drive/browse`, { params }).then(r => r.data.data),

  // Import
  importFromDrive: (data: { connectionId: string; driveFileIds: string[]; targetFolderId: string; preserveStructure?: boolean }) =>
    apiClient.post<{ data: ImportJobResponse }>(`${BASE}/google-drive/import`, data).then(r => r.data.data),

  // Export
  exportToDrive: (data: { connectionId: string; fileIds: string[]; targetDriveFolderId: string; conflictStrategy?: string }) =>
    apiClient.post<{ data: ExportJobResponse }>(`${BASE}/google-drive/export`, data).then(r => r.data.data),

  // Job status
  getJobStatus: (jobId: string) =>
    apiClient.get<{ data: JobStatus }>(`${BASE}/jobs/${jobId}`).then(r => r.data.data),

  // Sync links
  createSyncLink: (data: { connectionId: string; folderId: string; externalFolderId: string; externalFolderName: string; direction?: string; syncIntervalMinutes?: number }) =>
    apiClient.post<{ data: SyncLink }>(`${BASE}/sync-links`, data).then(r => r.data.data),

  getSyncLinks: () =>
    apiClient.get<{ data: SyncLink[] }>(`${BASE}/sync-links`).then(r => r.data.data),

  updateSyncLink: (syncLinkId: string, data: { direction?: string; syncIntervalMinutes?: number; status?: string }) =>
    apiClient.put<{ data: SyncLink }>(`${BASE}/sync-links/${syncLinkId}`, data).then(r => r.data.data),

  deleteSyncLink: (syncLinkId: string) =>
    apiClient.delete(`${BASE}/sync-links/${syncLinkId}`),

  getSyncJobs: (syncLinkId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<{ data: SyncJob[] }>(`${BASE}/sync-links/${syncLinkId}/jobs`, { params }).then(r => r.data.data),
};
