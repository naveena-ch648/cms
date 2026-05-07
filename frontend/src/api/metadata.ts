import apiClient from './client';

export interface MetadataFieldDto {
  id: string;
  name: string;
  fieldType: 'TEXT' | 'NUMBER' | 'DATE' | 'DROPDOWN';
  description?: string;
  options?: string[];
  required: boolean;
  displayOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface MetadataFieldRequest {
  name: string;
  fieldType: string;
  description?: string;
  options?: string[];
  required?: boolean;
  displayOrder?: number;
}

export interface MetadataValueDto {
  fieldId: string;
  fieldName: string;
  fieldType: string;
  value: string | number | null;
  updatedAt: string;
}

export interface MetadataValueUpdate {
  fieldId: string;
  value: string | number | null;
}

export interface BulkMetadataRequest {
  fileIds: string[];
  values: MetadataValueUpdate[];
}

export const metadataApi = {
  // Metadata Fields
  listFields: (workspaceId: string, includeDeleted = false) =>
    apiClient.get<{ success: boolean; data: MetadataFieldDto[] }>(
      `/workspaces/${workspaceId}/metadata-fields`,
      { params: { includeDeleted } }
    ),

  createField: (workspaceId: string, request: MetadataFieldRequest) =>
    apiClient.post<{ success: boolean; data: MetadataFieldDto }>(
      `/workspaces/${workspaceId}/metadata-fields`,
      request
    ),

  updateField: (workspaceId: string, fieldId: string, request: MetadataFieldRequest) =>
    apiClient.put<{ success: boolean; data: MetadataFieldDto }>(
      `/workspaces/${workspaceId}/metadata-fields/${fieldId}`,
      request
    ),

  deleteField: (workspaceId: string, fieldId: string) =>
    apiClient.delete(`/workspaces/${workspaceId}/metadata-fields/${fieldId}`),

  // Metadata Values
  getFileMetadata: (fileId: string) =>
    apiClient.get<{ success: boolean; data: MetadataValueDto[] }>(
      `/files/${fileId}/metadata`
    ),

  updateFileMetadata: (fileId: string, values: MetadataValueUpdate[]) =>
    apiClient.put<{ success: boolean; data: { updated: number; values: MetadataValueDto[] } }>(
      `/files/${fileId}/metadata`,
      { values }
    ),

  deleteFieldValue: (fileId: string, fieldId: string) =>
    apiClient.delete(`/files/${fileId}/metadata/${fieldId}`),

  // Bulk Metadata
  bulkUpdateMetadata: (request: BulkMetadataRequest) =>
    apiClient.put<{ success: boolean; data: { totalFiles: number; updated: number; failed: number } }>(
      '/files/bulk-metadata',
      request
    ),
};
