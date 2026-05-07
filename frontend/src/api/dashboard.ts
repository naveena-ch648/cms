import apiClient from './client';
import type { DashboardSummary, RecentFile, ActivityEvent, SharedItem, Alert } from '../types/dashboard';
import type { ApiResponse } from '../types/api';

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const response = await apiClient.get<ApiResponse<DashboardSummary>>('/dashboard/summary');
  return response.data.data;
}

export async function getRecentFiles(limit = 10): Promise<RecentFile[]> {
  const response = await apiClient.get<ApiResponse<RecentFile[]>>('/dashboard/recent-files', {
    params: { limit },
  });
  return response.data.data;
}

export async function getActivity(page = 0, size = 20): Promise<{ content: ActivityEvent[]; totalElements: number }> {
  const response = await apiClient.get<ApiResponse<{ content: ActivityEvent[]; totalElements: number }>>('/dashboard/activity', {
    params: { page, size },
  });
  return response.data.data;
}

export async function getSharedItems(direction = 'WITH_ME', limit = 10): Promise<SharedItem[]> {
  const response = await apiClient.get<ApiResponse<SharedItem[]>>('/dashboard/shared', {
    params: { direction, limit },
  });
  return response.data.data;
}

export async function getAlerts(): Promise<Alert[]> {
  const response = await apiClient.get<ApiResponse<Alert[]>>('/dashboard/alerts');
  return response.data.data;
}

export async function dismissAlert(alertId: string): Promise<void> {
  await apiClient.post(`/dashboard/alerts/${alertId}/dismiss`);
}
