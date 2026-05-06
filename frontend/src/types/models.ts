export interface Organization {
  id: string;
  name: string;
  slug: string;
  billingContactEmail: string;
  status: 'ACTIVE' | 'DEACTIVATED';
  policies: OrganizationPolicies;
  createdAt: string;
  updatedAt?: string;
}

export interface OrganizationPolicies {
  passwordMinLength?: number;
  passwordRequireUppercase?: boolean;
  passwordRequireNumber?: boolean;
  passwordRequireSpecialChar?: boolean;
  sessionTimeoutMinutes?: number;
  maxWorkspaces?: number;
  maxFailedLoginAttempts?: number;
  accountLockoutMinutes?: number;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  organizationRole?: RoleSummary;
  lastLoginAt?: string;
  createdAt: string;
}

export interface RoleSummary {
  id: string;
  name: string;
}

export interface Role {
  id: string;
  name: string;
  description?: string;
  parentRole?: RoleSummary;
  isSystem: boolean;
  directPermissions: string[];
  effectivePermissions: string[];
  createdAt: string;
}

export interface Permission {
  id: string;
  name: string;
  description?: string;
  category: string;
}

export interface Group {
  id: string;
  name: string;
  description?: string;
  memberCount: number;
  workspaceRoles: WorkspaceRoleAssignment[];
  createdAt: string;
  updatedAt?: string;
}

export interface WorkspaceRoleAssignment {
  workspaceId: string;
  workspaceName: string;
  role: RoleSummary;
}

export interface Workspace {
  id: string;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'ARCHIVED' | 'DELETED';
  memberCount: number;
  myRole?: {
    id: string;
    name: string;
    source: 'direct' | 'group' | 'organization';
  };
  createdAt: string;
}

export interface WorkspaceMember {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  effectiveRole: RoleSummary;
  source: 'direct' | 'group' | 'organization';
  groups: { id: string; name: string }[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user?: User & {
    organizationId: string;
    organizationName: string;
  };
}

export interface AuthUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  organizationId: string;
  organizationName: string;
  organizationRole: string;
  lastLoginAt?: string;
}
