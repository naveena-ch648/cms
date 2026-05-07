import apiClient from './client';

export interface TagDto {
  name: string;
  createdAt: string;
  createdBy: string;
}

export interface BulkTagRequest {
  fileIds: string[];
  tags: string[];
}

export const tagsApi = {
  getFileTags: (fileId: string) =>
    apiClient.get<{ success: boolean; data: TagDto[] }>(`/files/${fileId}/tags`),

  addTags: (fileId: string, tags: string[]) =>
    apiClient.post<{ success: boolean; data: { added: number; tags: TagDto[] } }>(
      `/files/${fileId}/tags`,
      { tags }
    ),

  removeTag: (fileId: string, tagName: string) =>
    apiClient.delete(`/files/${fileId}/tags/${encodeURIComponent(tagName)}`),

  autocomplete: (workspaceId: string, prefix: string, limit = 10) =>
    apiClient.get<{ success: boolean; data: string[] }>(
      `/workspaces/${workspaceId}/tags/autocomplete`,
      { params: { prefix, limit } }
    ),

  bulkAddTags: (request: BulkTagRequest) =>
    apiClient.post<{ success: boolean; data: { totalFiles: number; updated: number; failed: number } }>(
      '/files/bulk-tags',
      request
    ),
};
