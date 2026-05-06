import apiClient from './client';
import type { ShareLink, CreateShareLinkRequest, UpdateShareLinkRequest, ShareLinkAccess } from '../types/sharing';
import type { ApiResponse } from '../types/api';

export async function createShareLink(workspaceId: string, request: CreateShareLinkRequest): Promise<ShareLink> {
  const response = await apiClient.post<ApiResponse<ShareLink>>(
    `/workspaces/${workspaceId}/share-links`,
    request
  );
  return response.data.data;
}

export async function listShareLinks(
  workspaceId: string,
  status?: string,
  page = 0,
  size = 20
): Promise<{ content: ShareLink[]; totalElements: number; totalPages: number }> {
  const params: Record<string, string | number> = { page, size };
  if (status) params.status = status;
  const response = await apiClient.get<ApiResponse<{ content: ShareLink[]; totalElements: number; totalPages: number }>>(
    `/workspaces/${workspaceId}/share-links`,
    { params }
  );
  return response.data.data;
}

export async function updateShareLink(uuid: string, request: UpdateShareLinkRequest): Promise<ShareLink> {
  const response = await apiClient.patch<ApiResponse<ShareLink>>(
    `/share-links/${uuid}`,
    request
  );
  return response.data.data;
}

export async function revokeShareLink(uuid: string): Promise<void> {
  await apiClient.delete(`/share-links/${uuid}`);
}

export async function getShareLinkAccesses(
  uuid: string,
  page = 0,
  size = 20
): Promise<{ content: ShareLinkAccess[]; totalElements: number; totalPages: number }> {
  const response = await apiClient.get<ApiResponse<{ content: ShareLinkAccess[]; totalElements: number; totalPages: number }>>(
    `/share-links/${uuid}/accesses`,
    { params: { page, size } }
  );
  return response.data.data;
}
