export interface SearchResult {
  fileUuid: string;
  fileName: string;
  fileType: string;
  mimeType: string;
  fileSize: number;
  ownerUuid: string;
  ownerName: string;
  folderPath: string;
  folderUuid: string;
  createdAt: string;
  updatedAt: string;
  highlights: string[] | null;
  score: number;
}

export interface SearchPagination {
  page: number;
  size: number;
  totalResults: number;
  totalPages: number;
}

export interface SearchResponseData {
  results: SearchResult[];
  pagination: SearchPagination;
  query: string;
  filters: SearchFilters;
}

export interface SearchFilters {
  fileType: string[] | null;
  ownerUuid: string | null;
  dateFrom: string | null;
  dateTo: string | null;
}

export interface SearchParams {
  workspaceId: string;
  q?: string;
  fileType?: string[];
  ownerUuid?: string;
  dateFrom?: string;
  dateTo?: string;
  dateField?: string;
  sortBy?: SortOption;
  sortOrder?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export type SortOption = 'relevance' | 'name' | 'dateModified' | 'dateCreated' | 'fileSize' | 'owner';

export interface AutocompleteResponse {
  files: Pick<SearchResult, 'fileUuid' | 'fileName' | 'folderPath' | 'fileType'>[];
  recentSearches: string[];
}

export const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'relevance', label: 'Relevance' },
  { value: 'name', label: 'Name' },
  { value: 'dateModified', label: 'Date Modified' },
  { value: 'dateCreated', label: 'Date Created' },
  { value: 'fileSize', label: 'File Size' },
  { value: 'owner', label: 'Owner' },
];

export const FILE_TYPE_OPTIONS = [
  'pdf', 'image', 'document', 'spreadsheet', 'presentation', 'video', 'audio', 'archive', 'other'
] as const;

export type FileTypeOption = typeof FILE_TYPE_OPTIONS[number];
