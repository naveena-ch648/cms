export interface PreviewPage {
  page: number;
  url: string;
  width: number;
  height: number;
}

export interface PreviewData {
  id: string;
  fileId: string;
  type: 'FULL_PREVIEW' | 'THUMBNAIL';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  mimeType: string;
  pageCount: number;
  pages: PreviewPage[];
  directUrl: string | null;
  expiresAt: string | null;
}

export interface ThumbnailData {
  url: string;
  width: number;
  height: number;
  expiresAt: string;
}

export interface PreviewJobStatus {
  thumbnail: { status: string; generatedAt?: string };
  fullPreview: { status: string; attempts?: number; queuedAt?: string };
}

export interface CommentAuthor {
  id: string;
  name: string;
  email: string;
}

export interface Comment {
  id: string;
  content: string;
  author: CommentAuthor;
  parentId: string | null;
  replies: Comment[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateCommentRequest {
  content: string;
  parentId?: string | null;
}
