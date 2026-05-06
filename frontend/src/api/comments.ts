import apiClient from './client';
import type { Comment, CreateCommentRequest } from '../types/collaboration';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export async function getComments(fileId: string, page = 0, size = 50): Promise<PagedResponse<Comment>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<Comment>>>(`/files/${fileId}/comments`, {
    params: { page, size },
  });
  return response.data.data;
}

export async function getCommentCount(fileId: string): Promise<number> {
  const response = await apiClient.get<ApiResponse<{ count: number }>>(`/files/${fileId}/comments/count`);
  return response.data.data.count;
}

export async function createComment(fileId: string, request: CreateCommentRequest): Promise<Comment> {
  const response = await apiClient.post<ApiResponse<Comment>>(`/files/${fileId}/comments`, request);
  return response.data.data;
}

export async function deleteComment(fileId: string, commentId: string): Promise<void> {
  await apiClient.delete(`/files/${fileId}/comments/${commentId}`);
}

export async function getFolderComments(folderId: string, page = 0, size = 50): Promise<PagedResponse<Comment>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<Comment>>>(`/folders/${folderId}/comments`, {
    params: { page, size },
  });
  return response.data.data;
}

export async function getFolderCommentCount(folderId: string): Promise<number> {
  const response = await apiClient.get<ApiResponse<{ count: number }>>(`/folders/${folderId}/comments/count`);
  return response.data.data.count;
}

export async function createFolderComment(folderId: string, request: CreateCommentRequest): Promise<Comment> {
  const response = await apiClient.post<ApiResponse<Comment>>(`/folders/${folderId}/comments`, request);
  return response.data.data;
}

export async function deleteFolderComment(folderId: string, commentId: string): Promise<void> {
  await apiClient.delete(`/folders/${folderId}/comments/${commentId}`);
}
