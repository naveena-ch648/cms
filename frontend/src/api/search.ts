import apiClient from './client';
import type { SearchResponseData, SearchParams, AutocompleteResponse } from '../types/search';
import type { ApiResponse } from '../types/api';

export async function search(params: SearchParams): Promise<SearchResponseData> {
  const response = await apiClient.get<ApiResponse<SearchResponseData>>('/search', {
    params: {
      workspaceId: params.workspaceId,
      q: params.q || undefined,
      fileType: params.fileType,
      ownerUuid: params.ownerUuid || undefined,
      dateFrom: params.dateFrom || undefined,
      dateTo: params.dateTo || undefined,
      dateField: params.dateField || undefined,
      sortBy: params.sortBy || undefined,
      sortOrder: params.sortOrder || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20,
    },
    paramsSerializer: {
      indexes: null,
    },
  });
  return response.data.data;
}

export async function autocomplete(
  q: string,
  workspaceId: string,
  limit = 5
): Promise<AutocompleteResponse> {
  const response = await apiClient.get<ApiResponse<AutocompleteResponse>>('/search/autocomplete', {
    params: { q, workspaceId, limit },
  });
  return response.data.data;
}

export async function saveRecentSearch(query: string, workspaceId: string): Promise<void> {
  await apiClient.post('/search/recent', { query, workspaceId });
}

export async function clearRecentSearches(): Promise<void> {
  await apiClient.delete('/search/recent');
}
