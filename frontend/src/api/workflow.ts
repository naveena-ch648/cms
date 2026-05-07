import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { WorkflowTransition, WorkflowStateInfo } from '../types/workflow';

export const workflowApi = {
  transition: (fileId: string, targetState: string, comment?: string) =>
    apiClient.post<ApiResponse<WorkflowTransition>>(`/files/${fileId}/workflow/transition`, {
      targetState,
      comment,
    }),

  bulkTransition: (fileIds: string[], targetState: string, comment?: string) =>
    apiClient.post<ApiResponse<{ transitioned: number; fileIds: string[] }>>('/files/bulk-workflow/transition', {
      fileIds,
      targetState,
      comment,
    }),

  getHistory: (fileId: string, page = 0, size = 50) =>
    apiClient.get<ApiResponse<WorkflowTransition[]>>(`/files/${fileId}/workflow/history`, {
      params: { page, size },
    }),

  getState: (fileId: string) =>
    apiClient.get<ApiResponse<WorkflowStateInfo>>(`/files/${fileId}/workflow/state`),
};
