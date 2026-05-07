import apiClient from './client';
import {
  AuditAlertInstance,
  AuditAlertRule,
  AuditSearchParams,
  AuditSearchResult,
  AuditStats,
  ComplianceReport,
} from '../types/models';

export const auditApi = {
  // Search events
  searchEvents: (params: AuditSearchParams) =>
    apiClient.get<AuditSearchResult>('/audit/events/search', { params }),

  // Get event by ID
  getEvent: (id: number) =>
    apiClient.get<{ id: number }>(`/audit/events/${id}`),

  // Get stats
  getStats: (dateFrom?: string, dateTo?: string) =>
    apiClient.get<AuditStats>('/audit/stats', { params: { dateFrom, dateTo } }),

  // Compliance reports
  listReports: () =>
    apiClient.get<ComplianceReport[]>('/audit/reports'),

  generateReport: (reportType: string, dateFrom: string, dateTo: string) =>
    apiClient.post<ComplianceReport>('/audit/reports', { reportType, dateFrom, dateTo }),

  downloadReport: (reportId: string) =>
    apiClient.get(`/audit/reports/${reportId}/download`, { responseType: 'blob' }),

  // Alert rules
  listAlertRules: () =>
    apiClient.get<AuditAlertRule[]>('/audit/alerts/rules'),

  createAlertRule: (rule: Omit<AuditAlertRule, 'id' | 'createdAt'>) =>
    apiClient.post<AuditAlertRule>('/audit/alerts/rules', rule),

  updateAlertRule: (ruleId: string, rule: Partial<AuditAlertRule>) =>
    apiClient.put<AuditAlertRule>(`/audit/alerts/rules/${ruleId}`, rule),

  deleteAlertRule: (ruleId: string) =>
    apiClient.delete(`/audit/alerts/rules/${ruleId}`),

  // Alert instances
  listAlertInstances: (page = 0, size = 20, acknowledged?: boolean) =>
    apiClient.get<{ content: AuditAlertInstance[]; totalElements: number }>(
      '/audit/alerts/instances',
      { params: { page, size, acknowledged } }
    ),

  acknowledgeAlert: (instanceId: string) =>
    apiClient.post(`/audit/alerts/instances/${instanceId}/acknowledge`),
};
