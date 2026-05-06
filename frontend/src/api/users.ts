import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { User } from '../types/models';

export const usersApi = {
  list: (params?: { search?: string; status?: string; page?: number; size?: number }) =>
    apiClient.get<ApiResponse<User[]>>('/users', { params }),

  get: (userId: string) =>
    apiClient.get<ApiResponse<User>>(`/users/${userId}`),

  create: (data: { email: string; firstName: string; lastName: string; password: string; roleId: string }) =>
    apiClient.post<ApiResponse<User>>('/users', data),

  update: (userId: string, data: { firstName?: string; lastName?: string; status?: string }) =>
    apiClient.put<ApiResponse<User>>(`/users/${userId}`, data),

  changeRole: (userId: string, roleId: string) =>
    apiClient.put<ApiResponse<User>>(`/users/${userId}/role`, { roleId }),

  changePassword: (userId: string, newPassword: string) =>
    apiClient.put<ApiResponse<void>>(`/users/${userId}/password`, { newPassword }),

  deactivate: (userId: string) =>
    apiClient.delete<ApiResponse<User>>(`/users/${userId}`),
};
