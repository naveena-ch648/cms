import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { Workspace } from '../types/models';
import type { WorkspaceMember } from '../types/collaboration';

export const workspacesApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<Workspace[]>>('/workspaces', { params }),

  get: (workspaceId: string) =>
    apiClient.get<ApiResponse<Workspace>>(`/workspaces/${workspaceId}`),

  create: (data: { name: string; description?: string }) =>
    apiClient.post<ApiResponse<Workspace>>('/workspaces', data),

  update: (workspaceId: string, data: { name?: string; description?: string }) =>
    apiClient.put<ApiResponse<Workspace>>(`/workspaces/${workspaceId}`, data),

  archive: (workspaceId: string) =>
    apiClient.put<ApiResponse<Workspace>>(`/workspaces/${workspaceId}/archive`),

  delete: (workspaceId: string) =>
    apiClient.delete<ApiResponse<void>>(`/workspaces/${workspaceId}`),

  addMember: (workspaceId: string, userId: number, roleId: string) =>
    apiClient.post<ApiResponse<void>>(`/workspaces/${workspaceId}/members`, { userId, roleId }),

  removeMember: (workspaceId: string, userId: number) =>
    apiClient.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/members/${userId}`),

  getMembers: (workspaceId: string) =>
    apiClient.get<ApiResponse<WorkspaceMember[]>>(`/workspaces/${workspaceId}/members`),
};
