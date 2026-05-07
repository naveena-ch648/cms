export type WorkflowState = 'DRAFT' | 'REVIEW' | 'APPROVED' | 'PUBLISHED' | 'ARCHIVED';

export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export type DecisionType = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface WorkflowTransition {
  id: string;
  fileId: string;
  fromState: WorkflowState;
  toState: WorkflowState;
  actorId: string;
  actorName: string;
  comment: string | null;
  approvalRequestId: string | null;
  createdAt: string;
}

export interface WorkflowStateInfo {
  currentState: WorkflowState;
  allowedTransitions: WorkflowState[];
  requiresApproval: WorkflowState[];
  hasActiveApproval: boolean;
  activeApprovalId: string | null;
}

export interface ApprovalReviewer {
  id: string;
  name: string;
  decision: DecisionType;
  comment: string | null;
  decidedAt: string | null;
}

export interface ApprovalRequest {
  id: string;
  fileId: string;
  fileName: string;
  submitterId: string;
  submitterName: string;
  status: ApprovalStatus;
  fromState: WorkflowState;
  toState: WorkflowState;
  comment: string | null;
  reviewers: ApprovalReviewer[];
  createdAt: string;
  completedAt: string | null;
}

export interface ApprovalDecisionResponse {
  id: string;
  approvalRequestId: string;
  reviewerId: string;
  reviewerName: string;
  decision: DecisionType;
  comment: string | null;
  decidedAt: string;
  approvalStatus: ApprovalStatus;
  approvedCount: number;
  totalReviewers: number;
}

export interface WorkflowTrigger {
  id: string;
  name: string;
  triggerState: WorkflowState;
  triggerType: 'NOTIFICATION' | 'PREREQUISITE';
  config: Record<string, unknown>;
  enabled: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}
