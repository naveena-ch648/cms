import axios from 'axios';
import type { FileVersion, VersionComparison, PagedResponse } from '../types/file';

const API_BASE = '/api/files';

export const fileVersionsApi = {
  // Upload a new version of a file
  uploadVersion: (fileId: string, file: File, changeNote?: string, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    if (changeNote) formData.append('changeNote', changeNote);

    return axios.post<{ data: FileVersion }>(`${API_BASE}/${fileId}/versions`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      },
    }).then(r => r.data.data);
  },

  // List version history for a file
  listVersions: (fileId: string, params?: { page?: number; size?: number }) =>
    axios.get<{ data: PagedResponse<FileVersion> }>(`${API_BASE}/${fileId}/versions`, { params })
      .then(r => r.data.data),

  // Get a specific version's details
  getVersion: (fileId: string, versionId: string) =>
    axios.get<{ data: FileVersion }>(`${API_BASE}/${fileId}/versions/${versionId}`)
      .then(r => r.data.data),

  // Download a specific version
  downloadVersion: (fileId: string, versionId: string) =>
    axios.get(`${API_BASE}/${fileId}/versions/${versionId}/download`, {
      maxRedirects: 0,
      validateStatus: s => s === 302,
    }).then(r => r.headers.location as string),

  // Restore a previous version
  restoreVersion: (fileId: string, versionId: string) =>
    axios.post<{ data: FileVersion }>(`${API_BASE}/${fileId}/versions/${versionId}/restore`)
      .then(r => r.data.data),

  // Compare two versions
  compareVersions: (fileId: string, versionId1: string, versionId2: string) =>
    axios.get<{ data: VersionComparison }>(`${API_BASE}/${fileId}/versions/compare`, {
      params: { v1: versionId1, v2: versionId2 },
    }).then(r => r.data.data),
};
