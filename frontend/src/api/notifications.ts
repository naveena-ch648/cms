import apiClient from './client';
import type { NotificationItem, UnreadCountResponse } from '../types/collaboration';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export const notificationsApi = {
  getNotifications: (params?: { page?: number; size?: number }) =>
    apiClient.get<ApiResponse<NotificationItem[]>>('/notifications', { params }),

  getUnreadCount: () =>
    apiClient.get<ApiResponse<UnreadCountResponse>>('/notifications/count'),

  markAsRead: (notificationId: string) =>
    apiClient.patch<ApiResponse<NotificationItem>>(`/notifications/${notificationId}/read`),

  markAllAsRead: () =>
    apiClient.post<ApiResponse<void>>('/notifications/read-all'),
};
