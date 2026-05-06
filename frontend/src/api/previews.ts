import apiClient from './client';
import type { PreviewData, ThumbnailData, PreviewJobStatus } from '../types/preview';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export async function getPreview(fileId: string): Promise<PreviewData> {
  const response = await apiClient.get<ApiResponse<PreviewData>>(`/files/${fileId}/preview`);
  return response.data.data;
}

export async function getThumbnail(fileId: string): Promise<ThumbnailData> {
  const response = await apiClient.get<ApiResponse<ThumbnailData>>(`/files/${fileId}/thumbnail`);
  return response.data.data;
}

export async function regeneratePreview(fileId: string): Promise<{ jobId: string; status: string }> {
  const response = await apiClient.post<ApiResponse<{ jobId: string; status: string }>>(`/files/${fileId}/preview/regenerate`);
  return response.data.data;
}

export async function getPreviewStatus(fileId: string): Promise<PreviewJobStatus> {
  const response = await apiClient.get<ApiResponse<PreviewJobStatus>>(`/files/${fileId}/preview/status`);
  return response.data.data;
}
