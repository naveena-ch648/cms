import apiClient from './client';

export interface AskRequest {
  question: string;
  workspaceId: string;
  conversationId?: string;
  maxChunks?: number;
}

export interface Citation {
  index: number;
  documentId: string;
  documentName: string;
  pageNumber: number;
  excerpt: string;
  chunkId: string;
  charStart: number;
  charEnd: number;
}

export interface AskResponse {
  conversationId: string;
  messageId: string;
  answer: string;
  citations: Citation[];
  modelUsed: string | null;
  tokenCount: number;
  noRelevantInfo: boolean;
}

export interface ConversationSummary {
  id: string;
  title: string;
  status: 'ACTIVE' | 'ARCHIVED';
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations: Citation[] | null;
  modelUsed: string | null;
  tokenCount: number;
  createdAt: string;
}

export interface SummarizeRequest {
  documentId: string;
  workspaceId: string;
  length?: 'short' | 'medium' | 'long';
  maxSections?: number;
}

export interface SummarizeResponse {
  documentId: string;
  documentName: string;
  summary: string;
  citations: Citation[];
  modelUsed: string | null;
  tokenCount: number;
}

const API_BASE = '/qa';

export const qaApi = {
  ask: (request: AskRequest) =>
    apiClient.post<{ data: AskResponse }>(`${API_BASE}/ask`, request)
      .then(r => r.data.data),

  getConversations: (workspaceId: string, params?: { page?: number; size?: number; status?: string }) =>
    apiClient.get<{ data: { content: ConversationSummary[]; totalElements: number } }>(
      `${API_BASE}/conversations`, { params: { workspaceId, ...params } }
    ).then(r => r.data.data),

  getMessages: (conversationId: string, params?: { page?: number; size?: number }) =>
    apiClient.get<{ data: { content: ConversationMessage[]; totalElements: number } }>(
      `${API_BASE}/conversations/${conversationId}/messages`, { params }
    ).then(r => r.data.data),

  deleteConversation: (conversationId: string) =>
    apiClient.delete(`${API_BASE}/conversations/${conversationId}`),

  summarize: (request: SummarizeRequest) =>
    apiClient.post<{ data: SummarizeResponse }>(`${API_BASE}/summarize`, request)
      .then(r => r.data.data),
};
