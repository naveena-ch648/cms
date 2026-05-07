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

// Audit types
export type AuditCategory = 'AUTHENTICATION' | 'FILE_OPERATION' | 'PERMISSION_CHANGE' | 'SHARING' | 'WORKFLOW' | 'SYSTEM';

export interface AuditEvent {
  id: number;
  userId?: number;
  actorName: string;
  eventType: string;
  category: AuditCategory;
  resourceType?: string;
  resourceId?: number;
  resourceName?: string;
  outcome: string;
  details?: string;
  ipAddress?: string;
  userAgent?: string;
  workspaceId?: number;
  createdAt: string;
}

export interface AuditSearchParams {
  query?: string;
  category?: AuditCategory;
  eventType?: string;
  userId?: number;
  outcome?: string;
  workspaceId?: number;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}

export interface AuditSearchResult {
  events: AuditEvent[];
  total: number;
  page: number;
  size: number;
}

export interface ComplianceReport {
  id: string;
  reportType: string;
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED';
  dateFrom: string;
  dateTo: string;
  totalEvents?: number;
  filePath?: string;
  fileSize?: number;
  errorMessage?: string;
  createdAt: string;
  completedAt?: string;
}

export interface AuditAlertRule {
  id: string;
  name: string;
  description?: string;
  eventType: string;
  thresholdCount: number;
  timeWindowMinutes: number;
  enabled: boolean;
  createdAt: string;
}

export interface AuditAlertInstance {
  id: string;
  ruleName: string;
  ruleId: string;
  triggeredByUser?: string;
  eventCount: number;
  windowStart: string;
  windowEnd: string;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  createdAt: string;
}

export interface AuditStats {
  totalEvents: number;
  byCategory: Record<AuditCategory, number>;
  byOutcome: Record<string, number>;
}
