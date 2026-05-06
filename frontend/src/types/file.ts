export interface FileInfo {
  id: string;
  name: string;
  originalName: string;
  sizeBytes: number;
  mimeType: string;
  folderId: string;
  folderName?: string;
  workspaceId: string;
  status: 'ACTIVE' | 'TRASHED' | 'DELETED';
  checksumSha256?: string;
  description?: string;
  tags: string[];
  downloadCount: number;
  lastAccessedAt?: string;
  uploadedBy: { id: string; name: string };
  uploadCompletedAt?: string;
  thumbnailUrl?: string;
  previewable: boolean;
  trashedAt?: string;
  permanentDeleteAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UploadSession {
  sessionId: string;
  chunkSize: number;
  totalChunks: number;
  expiresAt: string;
}

export interface UploadSessionStatus {
  sessionId: string;
  fileName: string;
  totalChunks: number;
  completedChunks: number;
  percentComplete: number;
  status: 'INITIATED' | 'IN_PROGRESS' | 'COMPLETING' | 'FAILED';
  expiresAt: string;
  lastActivityAt: string;
}

export interface StorageQuota {
  maxStorageBytes: number;
  usedStorageBytes: number;
  maxFileSizeBytes: number;
  usedPercentage: number;
  trashRetentionDays: number;
}

export interface UploadProgress {
  fileId: string;
  fileName: string;
  totalBytes: number;
  uploadedBytes: number;
  percentage: number;
  status: 'queued' | 'uploading' | 'processing' | 'completed' | 'failed' | 'paused';
  error?: string;
  startedAt?: number;
  estimatedTimeRemaining?: number;
}

export interface ChunkStatus {
  chunkNumber: number;
  received: boolean;
  completedChunks: number;
  totalChunks: number;
}

export interface FileUploadRequest {
  file: File;
  folderId: string;
  description?: string;
  tags?: string[];
  onDuplicate?: 'rename' | 'replace' | 'error';
}

export interface FileMoveRequest {
  targetFolderId: string;
  onDuplicate?: 'rename' | 'replace' | 'error';
}

export interface FileUpdateRequest {
  name?: string;
  description?: string;
  tags?: string[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FileVersion {
  id: string;
  versionNumber: number;
  fileName: string;
  sizeBytes: number;
  mimeType: string;
  checksumSha256?: string;
  changeNote?: string;
  uploadedBy: { id: string; name: string };
  createdAt: string;
  isCurrent: boolean;
}

export interface VersionComparison {
  version1: {
    id: string;
    versionNumber: number;
    sizeBytes: number;
    mimeType: string;
    checksumSha256: string;
    uploadedBy: { id: string; name: string };
    createdAt: string;
    downloadUrl: string;
  };
  version2: {
    id: string;
    versionNumber: number;
    sizeBytes: number;
    mimeType: string;
    checksumSha256: string;
    uploadedBy: { id: string; name: string };
    createdAt: string;
    downloadUrl: string;
  };
  sizeDifference: number;
  sameContent: boolean;
}
