import apiClient from './client';
import type { ApiResponse } from '../types/api';

export interface AdminAnalytics {
  summary: {
    totalUsers: number;
    activeUsers: number;
    inactiveUsers: number;
    lockedUsers: number;
    totalFiles: number;
    totalStorageUsedBytes: number;
    totalStorageMaxBytes: number;
    storageUsedPercent: number;
    totalWorkspaces: number;
    activeUsersLast30Days: number;
  };
  roleDistribution: { roleName: string; userCount: number }[];
  uploadTrend: { date: string; count: number }[];
  storageTrend: { date: string; totalBytes: number }[];
  topActiveUsers: { userId: string; name: string; actionCount: number }[];
}

export interface StorageQuotaDetail {
  maxStorageBytes: number;
  usedStorageBytes: number;
  maxFileSizeBytes: number;
  allowedExtensions: string[] | null;
  blockedExtensions: string[] | null;
  trashRetentionDays: number;
  usedPercent: number;
  warning: string | null;
}

export interface StorageQuotaUpdate {
  maxStorageBytes?: number;
  maxFileSizeBytes?: number;
  allowedExtensions?: string[] | null;
  blockedExtensions?: string[] | null;
  trashRetentionDays?: number;
}

export interface BulkUserActionRequest {
  userIds: string[];
  action: 'CHANGE_ROLE' | 'ACTIVATE' | 'DEACTIVATE';
  roleId?: string;
}

export interface BulkUserActionResponse {
  totalRequested: number;
  successful: number;
  failed: number;
  results: { userId: string; status: string; reason?: string }[];
}

export const adminApi = {
  getAnalytics: (days?: number) =>
    apiClient.get<ApiResponse<AdminAnalytics>>('/admin/analytics', { params: { days } }),

  getStorageQuota: () =>
    apiClient.get<ApiResponse<StorageQuotaDetail>>('/admin/storage-quota'),

  updateStorageQuota: (data: StorageQuotaUpdate) =>
    apiClient.put<ApiResponse<StorageQuotaDetail>>('/admin/storage-quota', data),

  bulkUserAction: (data: BulkUserActionRequest) =>
    apiClient.post<ApiResponse<BulkUserActionResponse>>('/admin/users/bulk-action', data),
};
