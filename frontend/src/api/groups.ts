import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { Group } from '../types/models';

export const groupsApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<Group[]>>('/groups', { params }),

  get: (groupId: string) =>
    apiClient.get<ApiResponse<Group>>(`/groups/${groupId}`),

  create: (data: { name: string; description?: string }) =>
    apiClient.post<ApiResponse<Group>>('/groups', data),

  update: (groupId: string, data: { name?: string; description?: string }) =>
    apiClient.put<ApiResponse<Group>>(`/groups/${groupId}`, data),

  delete: (groupId: string) =>
    apiClient.delete<ApiResponse<void>>(`/groups/${groupId}`),

  addMember: (groupId: string, userId: number) =>
    apiClient.post<ApiResponse<void>>(`/groups/${groupId}/members`, { userId }),

  removeMember: (groupId: string, userId: number) =>
    apiClient.delete<ApiResponse<void>>(`/groups/${groupId}/members/${userId}`),
};
