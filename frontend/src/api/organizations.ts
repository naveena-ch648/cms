import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type { Organization } from '../types/models';

export const organizationApi = {
  get: (orgId: string) =>
    apiClient.get<ApiResponse<Organization>>(`/organizations/${orgId}`),

  update: (orgId: string, data: { name?: string; billingContactEmail?: string }) =>
    apiClient.put<ApiResponse<Organization>>(`/organizations/${orgId}`, data),

  updatePolicies: (orgId: string, policies: Record<string, unknown>) =>
    apiClient.put<ApiResponse<Record<string, unknown>>>(`/organizations/${orgId}/policies`, { policies }),

  deactivate: (orgId: string) =>
    apiClient.put<ApiResponse<Organization>>(`/organizations/${orgId}/deactivate`),
};
