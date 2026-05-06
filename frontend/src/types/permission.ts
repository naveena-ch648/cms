export interface Permission {
  id: number;
  folderUuid: string;
  userUuid: string | null;
  userName: string | null;
  groupUuid: string | null;
  groupName: string | null;
  role: string;
  isOverride: boolean;
  inherited: boolean;
  sourceFolderUuid: string | null;
  createdAt: string;
}

export interface EffectivePermission {
  folderUuid: string;
  effectiveRole: string | null;
  source: 'DIRECT' | 'INHERITED' | 'GROUP';
  sourceFolderUuid: string;
}

export interface AssignPermissionRequest {
  userUuid?: string;
  groupUuid?: string;
  roleUuid: string;
  isOverride?: boolean;
}
