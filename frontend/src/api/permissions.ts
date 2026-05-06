import apiClient from './client';
import type { Permission, EffectivePermission, AssignPermissionRequest } from '../types/permission';
import type { ApiResponse } from '../types/api';

export async function listPermissions(workspaceId: string, folderId: string): Promise<Permission[]> {
  const response = await apiClient.get<ApiResponse<Permission[]>>(
    `/workspaces/${workspaceId}/folders/${folderId}/permissions`
  );
  return response.data.data;
}

export async function assignPermission(
  workspaceId: string,
  folderId: string,
  request: AssignPermissionRequest
): Promise<Permission> {
  const response = await apiClient.post<ApiResponse<Permission>>(
    `/workspaces/${workspaceId}/folders/${folderId}/permissions`,
    request
  );
  return response.data.data;
}

export async function removePermission(
  workspaceId: string,
  folderId: string,
  permissionId: number
): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/folders/${folderId}/permissions/${permissionId}`);
}

export async function getEffectivePermission(
  workspaceId: string,
  folderId: string
): Promise<EffectivePermission> {
  const response = await apiClient.get<ApiResponse<EffectivePermission>>(
    `/workspaces/${workspaceId}/folders/${folderId}/effective-permission`
  );
  return response.data.data;
}
