import apiClient from './client';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export interface EmailDigestPreference {
  digestEnabled: boolean;
  digestFrequency: 'DAILY' | 'WEEKLY';
  includeSharedFiles: boolean;
  includePendingApprovals: boolean;
  includeStorageUsage: boolean;
  includeRecentActivity: boolean;
}

export interface UpdateDigestPreferenceRequest {
  digestEnabled: boolean;
  digestFrequency?: 'DAILY' | 'WEEKLY';
  includeSharedFiles?: boolean;
  includePendingApprovals?: boolean;
  includeStorageUsage?: boolean;
  includeRecentActivity?: boolean;
}

export const emailDigestApi = {
  getPreferences: () =>
    apiClient.get<ApiResponse<EmailDigestPreference>>('/me/email-preferences'),

  updatePreferences: (data: UpdateDigestPreferenceRequest) =>
    apiClient.put<ApiResponse<EmailDigestPreference>>('/me/email-preferences', data),
};
