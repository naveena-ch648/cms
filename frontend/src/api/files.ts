import apiClient from "./client";
import type {
  FileInfo,
  UploadSession,
  UploadSessionStatus,
  StorageQuota,
  ChunkStatus,
  FileUpdateRequest,
  FileMoveRequest,
  PagedResponse,
} from "../types/file";

const API_BASE = "/files";

export const filesApi = {
  // List files in a folder
  listFiles: (
    folderId: string,
    params?: {
      page?: number;
      size?: number;
      sort?: string;
      direction?: string;
    },
  ) =>
    apiClient
      .get<{
        data: PagedResponse<FileInfo>;
      }>(API_BASE, { params: { folderId, ...params } })
      .then((r) => r.data.data),

  // Get file details (also records a view in the user's recent files)
  getFile: (fileId: string) =>
    apiClient
      .get<{ data: FileInfo }>(`${API_BASE}/${fileId}`)
      .then((r) => r.data.data),

  // Explicitly record a file view in the user's recent-files history (lightweight, fire-and-forget)
  recordView: (fileId: string) =>
    apiClient.post(`${API_BASE}/${fileId}/view`).catch(() => {
      /* best-effort */
    }),

  // Upload single file
  uploadFile: (
    file: File,
    folderId: string,
    options?: { description?: string; tags?: string[]; onDuplicate?: string },
    onProgress?: (progress: number) => void,
  ) => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("folderId", folderId);
    if (options?.description)
      formData.append("description", options.description);
    if (options?.tags) formData.append("tags", JSON.stringify(options.tags));
    if (options?.onDuplicate)
      formData.append("onDuplicate", options.onDuplicate);

    return apiClient
      .post<{ data: FileInfo }>(`${API_BASE}/upload`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress: (e) => {
          if (onProgress && e.total) {
            onProgress(Math.round((e.loaded / e.total) * 100));
          }
        },
      })
      .then((r) => r.data.data);
  },

  // Initiate chunked upload
  initiateChunkedUpload: (params: {
    fileName: string;
    fileSize: number;
    mimeType: string;
    folderId: string;
    chunkSize?: number;
    description?: string;
    tags?: string[];
    onDuplicate?: string;
  }) =>
    apiClient
      .post<{ data: UploadSession }>(`${API_BASE}/upload/initiate`, params)
      .then((r) => r.data.data),

  // Upload a chunk
  uploadChunk: (sessionId: string, chunkNumber: number, data: ArrayBuffer) =>
    apiClient
      .put<{ data: ChunkStatus }>(
        `${API_BASE}/upload/${sessionId}/chunks/${chunkNumber}`,
        data,
        {
          headers: { "Content-Type": "application/octet-stream" },
        },
      )
      .then((r) => r.data.data),

  // Complete chunked upload
  completeChunkedUpload: (sessionId: string, checksumSha256?: string) =>
    apiClient
      .post<{
        data: FileInfo;
      }>(`${API_BASE}/upload/${sessionId}/complete`, { checksumSha256 })
      .then((r) => r.data.data),

  // Abort chunked upload
  abortUpload: (sessionId: string) =>
    apiClient.delete(`${API_BASE}/upload/${sessionId}`),

  // Get upload session status
  getUploadStatus: (sessionId: string) =>
    apiClient
      .get<{
        data: UploadSessionStatus;
      }>(`${API_BASE}/upload/${sessionId}/status`)
      .then((r) => r.data.data),

  // Download file (returns redirect URL)
  downloadFile: (fileId: string) =>
    apiClient
      .get(`${API_BASE}/${fileId}/download`, {
        maxRedirects: 0,
        validateStatus: (s) => s === 302,
      })
      .then((r) => r.headers.location as string),

  // Get preview URL
  getPreviewUrl: (fileId: string) =>
    apiClient
      .get<{
        data: { previewUrl: string; mimeType: string; expiresAt: string };
      }>(`${API_BASE}/${fileId}/preview`)
      .then((r) => r.data.data),

  // Update file metadata
  updateFile: (fileId: string, update: FileUpdateRequest) =>
    apiClient
      .patch<{ data: FileInfo }>(`${API_BASE}/${fileId}`, update)
      .then((r) => r.data.data),

  // Move file
  moveFile: (fileId: string, req: FileMoveRequest) =>
    apiClient
      .post<{ data: FileInfo }>(`${API_BASE}/${fileId}/move`, req)
      .then((r) => r.data.data),

  // Copy file
  copyFile: (fileId: string, req: FileMoveRequest) =>
    apiClient
      .post<{ data: FileInfo }>(`${API_BASE}/${fileId}/copy`, req)
      .then((r) => r.data.data),

  // Trash file
  trashFile: (fileId: string) =>
    apiClient
      .delete<{ data: FileInfo }>(`${API_BASE}/${fileId}`)
      .then((r) => r.data.data),

  // Restore from trash
  restoreFile: (fileId: string) =>
    apiClient
      .post<{ data: FileInfo }>(`${API_BASE}/${fileId}/restore`)
      .then((r) => r.data.data),

  // Permanent delete
  permanentDelete: (fileId: string) =>
    apiClient.delete(`${API_BASE}/${fileId}/permanent`),

  // List trash
  listTrash: (workspaceId: string, params?: { page?: number; size?: number }) =>
    apiClient
      .get<{
        data: PagedResponse<FileInfo>;
      }>(`${API_BASE}/trash`, { params: { workspaceId, ...params } })
      .then((r) => r.data.data),

  // Get storage quota
  getQuota: () =>
    apiClient
      .get<{ data: StorageQuota }>(`${API_BASE}/quota`)
      .then((r) => r.data.data),

  // Filter files by metadata and tags
  filterFiles: (params: {
    workspaceId: string;
    query?: string;
    tags?: string[];
    fileType?: string[];
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortOrder?: string;
    metadataFilters?: Record<string, string>;
  }) => {
    const queryParams: Record<string, unknown> = { ...params };
    delete queryParams.metadataFilters;
    if (params.metadataFilters) {
      for (const [key, value] of Object.entries(params.metadataFilters)) {
        (queryParams as Record<string, string>)[`meta.${key}`] = value;
      }
    }
    return apiClient
      .get<{ data: unknown }>(`${API_BASE}/filter`, { params: queryParams })
      .then((r) => r.data.data);
  },
};
