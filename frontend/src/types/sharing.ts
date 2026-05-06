export interface ShareLink {
  uuid: string;
  token: string;
  url: string;
  resourceType: 'FILE' | 'FOLDER';
  resourceName: string;
  hasPassword: boolean;
  expiresAt: string | null;
  allowDownload: boolean;
  watermarkEnabled: boolean;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  viewCount: number;
  lastAccessedAt: string | null;
  createdAt: string;
}

export interface CreateShareLinkRequest {
  resourceType: 'FILE' | 'FOLDER';
  fileUuid?: string;
  folderUuid?: string;
  password?: string;
  expiresAt?: string;
  allowDownload?: boolean;
  watermarkEnabled?: boolean;
}

export interface UpdateShareLinkRequest {
  password?: string;
  expiresAt?: string;
  allowDownload?: boolean;
  watermarkEnabled?: boolean;
}

export interface ShareLinkAccess {
  accessedAt: string;
  ipAddress: string;
  userAgent: string;
}

export interface PublicShareLinkData {
  resourceType: string;
  resourceName: string;
  mimeType: string;
  size: number;
  allowDownload: boolean;
  watermarkEnabled: boolean;
  previewUrl: string | null;
  downloadUrl: string | null;
  requiresPassword: boolean;
}
