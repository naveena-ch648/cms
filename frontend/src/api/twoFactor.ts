import apiClient from './client';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export interface TwoFactorStatus {
  enabled: boolean;
  method: 'TOTP' | 'EMAIL' | 'NONE';
}

export const twoFactorApi = {
  getStatus: () =>
    apiClient.get<ApiResponse<TwoFactorStatus>>('/me/two-factor'),

  initTotpSetup: () =>
    apiClient.post<ApiResponse<{ otpauthUri: string }>>('/me/two-factor/totp/init'),

  confirmTotpSetup: (code: number) =>
    apiClient.post<ApiResponse<{ backupCodes: string[] }>>('/me/two-factor/totp/confirm', { code }),

  enableEmailOtp: () =>
    apiClient.post<ApiResponse<{ backupCodes: string[] }>>('/me/two-factor/email/enable'),

  disable: () =>
    apiClient.post<ApiResponse<{ message: string }>>('/me/two-factor/disable'),

  verifyLogin: (pendingToken: string, code: string) =>
    apiClient.post<ApiResponse<{ accessToken: string; refreshToken: string; expiresIn: number }>>(
      '/auth/two-factor/verify',
      { pendingToken, code },
    ),
};
