import React, { useState } from 'react';
import type { WorkflowState, WorkflowStateInfo } from '../types/workflow';
import { workflowApi } from '../api/workflow';
import { ApprovalSubmitDialog } from './ApprovalSubmitDialog';

interface Props {
  fileId: string;
  workspaceId: string;
  stateInfo: WorkflowStateInfo;
  onTransitioned?: () => void;
}

const STATE_LABELS: Record<WorkflowState, string> = {
  DRAFT: 'Draft',
  REVIEW: 'In Review',
  APPROVED: 'Approved',
  PUBLISHED: 'Published',
  ARCHIVED: 'Archived',
};

export const WorkflowTransitionMenu: React.FC<Props> = ({ fileId, workspaceId, stateInfo, onTransitioned }) => {
  const [open, setOpen] = useState(false);
  const [comment, setComment] = useState('');
  const [selectedState, setSelectedState] = useState<WorkflowState | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showApprovalDialog, setShowApprovalDialog] = useState(false);

  const handleTransition = async () => {
    if (!selectedState) return;

    setLoading(true);
    setError(null);
    try {
      await workflowApi.transition(fileId, selectedState, comment || undefined);
      setOpen(false);
      setComment('');
      setSelectedState(null);
      onTransitioned?.();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Transition failed');
    } finally {
      setLoading(false);
    }
  };

  if (stateInfo.allowedTransitions.length === 0) {
    return null;
  }

  return (
    <div style={{ position: 'relative' }}>
      <button
        onClick={() => setOpen(!open)}
        style={{
          padding: '6px 12px',
          fontSize: '13px',
          borderRadius: '4px',
          border: '1px solid #d1d5db',
          backgroundColor: '#fff',
          cursor: 'pointer',
        }}
      >
        Move to...
      </button>

      {open && (
        <div
          style={{
            position: 'absolute',
            top: '100%',
            right: 0,
            marginTop: '4px',
            backgroundColor: '#fff',
            border: '1px solid #e5e7eb',
            borderRadius: '6px',
            boxShadow: '0 4px 6px rgba(0,0,0,0.1)',
            padding: '12px',
            zIndex: 50,
            minWidth: '240px',
          }}
        >
          <div style={{ marginBottom: '8px', fontSize: '13px', fontWeight: 500 }}>
            Transition to:
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '8px' }}>
            {stateInfo.allowedTransitions.map((state) => {
              const needsApproval = stateInfo.requiresApproval.includes(state);
              return (
                <button
                  key={state}
                  onClick={() => {
                    if (needsApproval) {
                      setShowApprovalDialog(true);
                      setOpen(false);
                    } else {
                      setSelectedState(state);
                    }
                  }}
                  style={{
                    padding: '6px 10px',
                    fontSize: '13px',
                    textAlign: 'left',
                    borderRadius: '4px',
                    border: selectedState === state ? '2px solid #3b82f6' : '1px solid #e5e7eb',
                    backgroundColor: selectedState === state ? '#eff6ff' : '#fff',
                    cursor: 'pointer',
                  }}
                >
                  {STATE_LABELS[state] || state}
                  {needsApproval && ' (submit for approval)'}
                </button>
              );
            })}
          </div>

          {selectedState && (
            <>
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Add a comment (optional)"
                rows={2}
                style={{
                  width: '100%',
                  fontSize: '13px',
                  padding: '6px 8px',
                  borderRadius: '4px',
                  border: '1px solid #d1d5db',
                  resize: 'vertical',
                  marginBottom: '8px',
                }}
              />
              <button
                onClick={handleTransition}
                disabled={loading}
                style={{
                  width: '100%',
                  padding: '6px 12px',
                  fontSize: '13px',
                  borderRadius: '4px',
                  border: 'none',
                  backgroundColor: '#3b82f6',
                  color: '#fff',
                  cursor: loading ? 'not-allowed' : 'pointer',
                }}
              >
                {loading ? 'Transitioning...' : `Move to ${STATE_LABELS[selectedState]}`}
              </button>
            </>
          )}

          {error && (
            <div style={{ marginTop: '8px', fontSize: '12px', color: '#dc2626' }}>{error}</div>
          )}
        </div>
      )}

      {showApprovalDialog && (
        <ApprovalSubmitDialog
          fileId={fileId}
          workspaceId={workspaceId}
          onClose={() => setShowApprovalDialog(false)}
          onSubmitted={onTransitioned}
        />
      )}
    </div>
  );
};
