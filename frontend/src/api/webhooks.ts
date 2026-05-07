import apiClient from './client';

export interface Webhook {
  id: string;
  name: string;
  url: string;
  eventTypes: string[];
  status: string;
  consecutiveFailures: number;
  createdBy: { id: string; name: string };
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDelivery {
  id: string;
  eventType: string;
  eventId: string;
  status: string;
  responseStatus: number | null;
  responseTimeMs: number | null;
  attemptNumber: number;
  deliveredAt: string | null;
  createdAt: string;
}

export interface WebhookTestResult {
  delivered: boolean;
  responseStatus: number | null;
  responseTimeMs: number | null;
  responseBody?: string;
  error?: string;
}

const BASE = '/webhooks';

export const webhooksApi = {
  create: (data: { name: string; url: string; secret?: string; eventTypes: string[] }) =>
    apiClient.post<{ data: Webhook }>(`${BASE}`, data).then(r => r.data.data),

  list: (params?: { status?: string; page?: number; size?: number }) =>
    apiClient.get<{ data: Webhook[] }>(`${BASE}`, { params }).then(r => r.data.data),

  get: (webhookId: string) =>
    apiClient.get<{ data: Webhook }>(`${BASE}/${webhookId}`).then(r => r.data.data),

  update: (webhookId: string, data: { name?: string; url?: string; secret?: string; eventTypes?: string[]; status?: string }) =>
    apiClient.put<{ data: Webhook }>(`${BASE}/${webhookId}`, data).then(r => r.data.data),

  delete: (webhookId: string) =>
    apiClient.delete(`${BASE}/${webhookId}`),

  test: (webhookId: string, data?: { eventType?: string }) =>
    apiClient.post<{ data: WebhookTestResult }>(`${BASE}/${webhookId}/test`, data || {}).then(r => r.data.data),

  getDeliveries: (webhookId: string, params?: { status?: string; eventType?: string; page?: number; size?: number }) =>
    apiClient.get<{ data: WebhookDelivery[] }>(`${BASE}/${webhookId}/deliveries`, { params }).then(r => r.data.data),

  retryDelivery: (webhookId: string, deliveryId: string) =>
    apiClient.post<{ data: { status: string; message: string } }>(`${BASE}/${webhookId}/deliveries/${deliveryId}/retry`).then(r => r.data.data),
};

export const WEBHOOK_EVENT_TYPES = [
  'file.uploaded',
  'file.deleted',
  'file.moved',
  'file.version_created',
  'folder.created',
  'folder.deleted',
  'workflow.status_changed',
  'user.created',
  'user.deactivated',
] as const;
