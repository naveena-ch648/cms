import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { ApprovalRequest, ApprovalDecisionResponse } from '../types/workflow';

export const approvalsApi = {
  submitForApproval: (fileId: string, reviewerIds: string[], comment?: string) =>
    apiClient.post<ApiResponse<ApprovalRequest>>(`/files/${fileId}/approvals`, {
      reviewerIds,
      comment,
    }),

  listWorkspaceApprovals: (workspaceId: string, status?: string, page = 0, size = 20) =>
    apiClient.get<ApiResponse<ApprovalRequest[]>>(`/workspaces/${workspaceId}/approvals`, {
      params: { status, page, size },
    }),

  getApproval: (approvalId: string) =>
    apiClient.get<ApiResponse<ApprovalRequest>>(`/approvals/${approvalId}`),

  decide: (approvalId: string, decision: string, comment?: string) =>
    apiClient.post<ApiResponse<ApprovalDecisionResponse>>(`/approvals/${approvalId}/decisions`, {
      decision,
      comment,
    }),

  cancel: (approvalId: string) =>
    apiClient.post<ApiResponse<void>>(`/approvals/${approvalId}/cancel`),

  listPending: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<ApprovalRequest[]>>('/approvals/pending', {
      params: { page, size },
    }),
};
