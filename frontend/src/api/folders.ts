import apiClient from './client';
import type { ApiResponse } from '../types/api';
import type {
  Folder,
  FolderTreeNode,
  FolderPermission,
  FolderFavorite,
  FolderRecent,
  CreateFolderRequest,
  UpdateFolderRequest,
  MoveFolderRequest,
  FolderPermissionRequest,
} from '../types/folder';

export const foldersApi = {
  list: (workspaceId: string, lazy?: boolean) =>
    apiClient.get<ApiResponse<FolderTreeNode[]>>(
      `/workspaces/${workspaceId}/folders`,
      { params: lazy ? { lazy: true } : undefined }
    ),

  get: (workspaceId: string, folderId: string) =>
    apiClient.get<ApiResponse<Folder>>(
      `/workspaces/${workspaceId}/folders/${folderId}`
    ),

  getChildren: (workspaceId: string, folderId: string) =>
    apiClient.get<ApiResponse<FolderTreeNode[]>>(
      `/workspaces/${workspaceId}/folders/${folderId}/children`
    ),

  create: (workspaceId: string, data: CreateFolderRequest) =>
    apiClient.post<ApiResponse<Folder>>(
      `/workspaces/${workspaceId}/folders`,
      data
    ),

  update: (workspaceId: string, folderId: string, data: UpdateFolderRequest) =>
    apiClient.put<ApiResponse<Folder>>(
      `/workspaces/${workspaceId}/folders/${folderId}`,
      data
    ),

  delete: (workspaceId: string, folderId: string) =>
    apiClient.delete<ApiResponse<void>>(
      `/workspaces/${workspaceId}/folders/${folderId}`
    ),

  move: (workspaceId: string, folderId: string, data: MoveFolderRequest) =>
    apiClient.put<ApiResponse<Folder>>(
      `/workspaces/${workspaceId}/folders/${folderId}/move`,
      data
    ),

  // Favorites
  addFavorite: (workspaceId: string, folderId: string) =>
    apiClient.post<ApiResponse<{ folderId: string; favoritedAt: string }>>(
      `/workspaces/${workspaceId}/folders/${folderId}/favorite`
    ),

  removeFavorite: (workspaceId: string, folderId: string) =>
    apiClient.delete<ApiResponse<void>>(
      `/workspaces/${workspaceId}/folders/${folderId}/favorite`
    ),

  listFavorites: (workspaceId: string) =>
    apiClient.get<ApiResponse<FolderFavorite[]>>(
      `/workspaces/${workspaceId}/favorites`
    ),

  // Recents
  recordVisit: (workspaceId: string, folderId: string) =>
    apiClient.post<ApiResponse<void>>(
      `/workspaces/${workspaceId}/folders/${folderId}/visit`
    ),

  listRecents: (workspaceId: string) =>
    apiClient.get<ApiResponse<FolderRecent[]>>(
      `/workspaces/${workspaceId}/recents`
    ),

  // Permissions
  listPermissions: (workspaceId: string, folderId: string) =>
    apiClient.get<ApiResponse<FolderPermission[]>>(
      `/workspaces/${workspaceId}/folders/${folderId}/permissions`
    ),

  assignPermission: (workspaceId: string, folderId: string, data: FolderPermissionRequest) =>
    apiClient.post<ApiResponse<FolderPermission>>(
      `/workspaces/${workspaceId}/folders/${folderId}/permissions`,
      data
    ),

  removePermission: (workspaceId: string, folderId: string, permissionId: number) =>
    apiClient.delete<ApiResponse<void>>(
      `/workspaces/${workspaceId}/folders/${folderId}/permissions/${permissionId}`
    ),
};
