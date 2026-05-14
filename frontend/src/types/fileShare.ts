// Types for internal file sharing between CMS users

export interface FileShareUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface FileShare {
  uuid: string;
  fileUuid: string;
  fileName: string;
  sharedBy: FileShareUser;
  sharedWith: FileShareUser;
  permission: 'VIEWER' | 'EDITOR';
  allowDownload: boolean;
  watermarkEnabled: boolean;
  expiresAt: string | null;
  status: 'ACTIVE' | 'REVOKED';
  createdAt: string;
}

export interface SharedWithMeFile {
  shareUuid: string;
  permission: 'VIEWER' | 'EDITOR';
  allowDownload: boolean;
  watermarkEnabled: boolean;
  expiresAt: string | null;
  sharedAt: string;

  fileUuid: string;
  fileName: string;
  fileSizeBytes: number;
  fileMimeType: string;
  fileThumbnailUrl: string | null;
  fileWorkspaceId: string | null;
  fileFolderId: string | null;

  sharedByUserId: string;
  sharedByFirstName: string;
  sharedByLastName: string;
  sharedByEmail: string;
}

export interface CreateFileShareRequest {
  sharedWithUserUuid: string;
  permission: 'VIEWER' | 'EDITOR';
  allowDownload?: boolean;
  watermarkEnabled?: boolean;
  expiresAt?: string | null;
}

export interface UpdateFileShareRequest {
  permission?: 'VIEWER' | 'EDITOR';
  allowDownload?: boolean;
  watermarkEnabled?: boolean;
  expiresAt?: string | null;
  removeExpiry?: boolean;
}

export interface CmsUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
}
