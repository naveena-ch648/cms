export interface Folder {
  id: string;
  name: string;
  parentId: string | null;
  workspaceId: string;
  sortOrder: number;
  status: 'ACTIVE' | 'DELETED';
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  breadcrumbs?: BreadcrumbItem[];
}

export interface FolderTreeNode {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  status: string;
  childCount: number;
  createdAt: string;
}

export interface BreadcrumbItem {
  id: string;
  name: string;
}

export interface FolderPermission {
  id: number;
  folderId: string;
  userId: string | null;
  userName: string | null;
  groupId: string | null;
  groupName: string | null;
  roleId: string;
  roleName: string;
  inherited: boolean;
  inheritedFrom: string | null;
  createdAt: string;
}

export interface FolderFavorite {
  id: string;
  name: string;
  parentId: string | null;
  favoritedAt: string;
}

export interface FolderRecent {
  id: string;
  name: string;
  parentId: string | null;
  accessedAt: string;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string | null;
  sortOrder?: number;
}

export interface UpdateFolderRequest {
  name?: string;
  sortOrder?: number;
}

export interface MoveFolderRequest {
  targetParentId: string | null;
  sortOrder?: number;
}

export interface FolderPermissionRequest {
  userId?: string;
  groupId?: string;
  roleId: string;
}
