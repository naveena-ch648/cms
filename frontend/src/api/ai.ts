import apiClient from './client';

export interface TagSuggestions {
  status: string;
  suggestions: string[];
  confidence: Record<string, number>;
  acceptedTags: string[];
  rejectedTags: string[];
}

export interface ClassificationSuggestion {
  status: string;
  category: string;
  confidence: number;
  alternatives: { category: string; confidence: number }[];
}

export interface SummarySuggestion {
  status: string;
  text: string;
  wordCount: number;
  keyTopics: string[];
}

export interface DuplicateInfo {
  fileId: string;
  fileName: string;
  similarity: number;
}

export interface DuplicateSuggestion {
  status: string;
  exactMatch: DuplicateInfo | null;
  nearDuplicates: DuplicateInfo[];
}

export interface SensitivityDetection {
  type: string;
  count: number;
  severity: string;
}

export interface SensitivitySuggestion {
  status: string;
  hasSensitiveData: boolean;
  severity: string;
  detections: SensitivityDetection[];
}

export interface WorkflowRecommendation {
  status: string;
  recommendedWorkflow: string;
  workflowId: string;
  reason: string;
}

export interface AISuggestions {
  fileId: string;
  processingStatus: string;
  tags: TagSuggestions | null;
  classification: ClassificationSuggestion | null;
  summary: SummarySuggestion | null;
  duplicates: DuplicateSuggestion | null;
  sensitivity: SensitivitySuggestion | null;
  workflowRecommendation: WorkflowRecommendation | null;
}

export interface AIJobInfo {
  id: string;
  type: string;
  status: string;
  confidence: number | null;
  triggeredBy: string;
  createdAt: string;
  completedAt: string | null;
}

export interface AIConfig {
  enabledFeatures: string[];
  confidenceThreshold: number;
  sensitivityPatterns: {
    customPatterns: { name: string; pattern: string }[];
  };
  workflowMappings: Record<string, string>;
}

export const aiApi = {
  getSuggestions(fileId: string) {
    return apiClient.get<{ success: boolean; data: AISuggestions }>(`/ai/files/${fileId}/suggestions`);
  },

  acceptTags(fileId: string, acceptedTags: string[], rejectedTags: string[]) {
    return apiClient.post<{ success: boolean; data: { appliedTags: string[]; rejectedTags: string[] } }>(
      `/ai/files/${fileId}/accept-tags`,
      { acceptedTags, rejectedTags }
    );
  },

  acceptClassification(fileId: string, category: string) {
    return apiClient.post<{ success: boolean; data: { category: string; applied: boolean } }>(
      `/ai/files/${fileId}/accept-classification`,
      { category }
    );
  },

  regenerate(fileId: string, types?: string[]) {
    return apiClient.post<{ success: boolean; data: { jobIds: string[]; message: string } }>(
      `/ai/files/${fileId}/regenerate`,
      types ? { types } : {}
    );
  },

  getJobs(fileId: string, page = 0, size = 20) {
    return apiClient.get<{ success: boolean; data: { content: AIJobInfo[]; totalElements: number; totalPages: number } }>(
      `/ai/files/${fileId}/jobs`,
      { params: { page, size } }
    );
  },

  applyWorkflow(fileId: string, workflowId: string) {
    return apiClient.post<{ success: boolean; data: { fileId: string; workflowId: string; workflowName: string; applied: boolean } }>(
      `/ai/files/${fileId}/apply-workflow`,
      { workflowId }
    );
  },

  getConfig() {
    return apiClient.get<{ success: boolean; data: AIConfig }>('/ai/config');
  },

  updateConfig(config: Partial<AIConfig>) {
    return apiClient.put<{ success: boolean; data: AIConfig }>('/ai/config', config);
  },
};
