import apiClient from './client';
import type { ActivityEvent, PagedResponse } from '../types/collaboration';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export const activityApi = {
  getFileActivity: (fileId: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<PagedResponse<ActivityEvent>>>(`/files/${fileId}/activity`, { params: { page, size } }),

  getFolderActivity: (folderId: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<PagedResponse<ActivityEvent>>>(`/folders/${folderId}/activity`, { params: { page, size } }),
};
