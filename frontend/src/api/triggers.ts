import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { WorkflowTrigger } from '../types/workflow';

export const triggersApi = {
  create: (workspaceId: string, data: {
    name: string;
    triggerState: string;
    triggerType: string;
    config?: Record<string, unknown>;
    enabled?: boolean;
  }) =>
    apiClient.post<ApiResponse<WorkflowTrigger>>(`/workspaces/${workspaceId}/workflow-triggers`, data),

  list: (workspaceId: string) =>
    apiClient.get<ApiResponse<WorkflowTrigger[]>>(`/workspaces/${workspaceId}/workflow-triggers`),

  update: (workspaceId: string, triggerId: string, data: {
    name?: string;
    triggerState?: string;
    triggerType?: string;
    config?: Record<string, unknown>;
    enabled?: boolean;
  }) =>
    apiClient.put<ApiResponse<WorkflowTrigger>>(`/workspaces/${workspaceId}/workflow-triggers/${triggerId}`, data),

  delete: (workspaceId: string, triggerId: string) =>
    apiClient.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/workflow-triggers/${triggerId}`),

  toggle: (workspaceId: string, triggerId: string, enabled: boolean) =>
    apiClient.patch<ApiResponse<WorkflowTrigger>>(`/workspaces/${workspaceId}/workflow-triggers/${triggerId}`, { enabled }),
};
