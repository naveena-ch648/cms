import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { Role, Permission } from '../types/models';

export const rolesApi = {
  list: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<Role[]>>('/roles', { params }),

  get: (roleId: string) =>
    apiClient.get<ApiResponse<Role>>(`/roles/${roleId}`),

  create: (data: { name: string; description?: string; parentRoleId?: string; permissionIds?: number[] }) =>
    apiClient.post<ApiResponse<Role>>('/roles', data),

  update: (roleId: string, data: { name?: string; description?: string; parentRoleId?: string; permissionIds?: number[] }) =>
    apiClient.put<ApiResponse<Role>>(`/roles/${roleId}`, data),

  delete: (roleId: string) =>
    apiClient.delete<ApiResponse<void>>(`/roles/${roleId}`),

  listPermissions: () =>
    apiClient.get<ApiResponse<Permission[]>>('/roles/permissions'),
};
