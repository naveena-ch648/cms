export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error: ApiError | null;
  meta: Meta;
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
}

export interface Meta {
  timestamp: string;
  requestId: string;
  pagination?: PagedMeta;
}

export interface PagedMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
