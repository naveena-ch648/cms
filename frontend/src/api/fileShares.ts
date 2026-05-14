import apiClient from './client';
import type {
  FileShare,
  SharedWithMeFile,
  CreateFileShareRequest,
  UpdateFileShareRequest,
  CmsUser,
} from '../types/fileShare';
import type { ApiResponse } from '../types/api';

// ─── Share a file ────────────────────────────────────────────────────────────

export async function shareFile(
  fileUuid: string,
  request: CreateFileShareRequest
): Promise<FileShare> {
  const res = await apiClient.post<ApiResponse<FileShare>>(
    `/files/${fileUuid}/shares`,
    request
  );
  return res.data.data;
}

// ─── List shares for a file ───────────────────────────────────────────────────

export async function listFileShares(fileUuid: string): Promise<FileShare[]> {
  const res = await apiClient.get<ApiResponse<FileShare[]>>(
    `/files/${fileUuid}/shares`
  );
  return res.data.data;
}

// ─── Update a share ───────────────────────────────────────────────────────────

export async function updateFileShare(
  shareUuid: string,
  request: UpdateFileShareRequest
): Promise<FileShare> {
  const res = await apiClient.patch<ApiResponse<FileShare>>(
    `/file-shares/${shareUuid}`,
    request
  );
  return res.data.data;
}

// ─── Revoke a share ───────────────────────────────────────────────────────────

export async function revokeFileShare(shareUuid: string): Promise<void> {
  await apiClient.delete(`/file-shares/${shareUuid}`);
}

// ─── Files shared with the current user ──────────────────────────────────────

export async function listSharedWithMe(
  page = 0,
  size = 20
): Promise<{ content: SharedWithMeFile[]; totalElements: number; totalPages: number }> {
  const res = await apiClient.get<
    ApiResponse<{ content: SharedWithMeFile[]; totalElements: number; totalPages: number }>
  >('/shared-with-me', { params: { page, size } });
  return res.data.data;
}

// ─── Search CMS users (for the share dialog) ─────────────────────────────────

export async function searchUsers(query: string): Promise<CmsUser[]> {
  const res = await apiClient.get<ApiResponse<{ content?: CmsUser[]; data?: CmsUser[] } | CmsUser[]>>(
    '/users',
    { params: { search: query, size: 10 } }
  );
  const data = res.data.data as { content?: CmsUser[] } | CmsUser[];
  if (Array.isArray(data)) return data;
  return (data as { content?: CmsUser[] }).content ?? [];
}
