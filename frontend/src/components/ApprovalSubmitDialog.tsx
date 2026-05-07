import React, { useState, useEffect } from 'react';
import { workspacesApi } from '../api/workspaces';
import { approvalsApi } from '../api/approvals';
import type { WorkspaceMember } from '../types/collaboration';

interface Props {
  fileId: string;
  workspaceId: string;
  onClose: () => void;
  onSubmitted?: () => void;
}

export const ApprovalSubmitDialog: React.FC<Props> = ({ fileId, workspaceId, onClose, onSubmitted }) => {
  const [members, setMembers] = useState<WorkspaceMember[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    workspacesApi.getMembers(workspaceId)
      .then(res => setMembers(res.data.data || []))
      .catch(() => setMembers([]));
  }, [workspaceId]);

  const toggleReviewer = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleSubmit = async () => {
    if (selectedIds.size === 0) {
      setError('Select at least one reviewer');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await approvalsApi.submitForApproval(fileId, Array.from(selectedIds), comment || undefined);
      onSubmitted?.();
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to submit for approval');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
    }}>
      <div style={{
        backgroundColor: '#fff', borderRadius: '8px', padding: '24px',
        width: '400px', maxHeight: '80vh', overflow: 'auto',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0 }}>Submit for Approval</h3>
          <button onClick={onClose} style={{ border: 'none', background: 'none', fontSize: '18px', cursor: 'pointer' }}>✕</button>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <label style={{ fontSize: '13px', fontWeight: 500, display: 'block', marginBottom: '6px' }}>
            Select Reviewers
          </label>
          <div style={{ maxHeight: '200px', overflow: 'auto', border: '1px solid #e5e7eb', borderRadius: '4px' }}>
            {members.map(member => (
              <label
                key={member.id}
                style={{
                  display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px',
                  cursor: 'pointer', borderBottom: '1px solid #f3f4f6',
                  backgroundColor: selectedIds.has(member.id) ? '#eff6ff' : '#fff',
                }}
              >
                <input
                  type="checkbox"
                  checked={selectedIds.has(member.id)}
                  onChange={() => toggleReviewer(member.id)}
                />
                <span style={{ fontSize: '13px' }}>
                  {member.firstName} {member.lastName}
                  <span style={{ color: '#6b7280', marginLeft: '4px' }}>{member.email}</span>
                </span>
              </label>
            ))}
            {members.length === 0 && (
              <div style={{ padding: '12px', fontSize: '13px', color: '#6b7280', textAlign: 'center' }}>
                No workspace members found
              </div>
            )}
          </div>
        </div>

        <div style={{ marginBottom: '16px' }}>
          <label style={{ fontSize: '13px', fontWeight: 500, display: 'block', marginBottom: '6px' }}>
            Comment (optional)
          </label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Add a comment for reviewers..."
            rows={3}
            style={{
              width: '100%', fontSize: '13px', padding: '8px',
              borderRadius: '4px', border: '1px solid #d1d5db', resize: 'vertical',
            }}
          />
        </div>

        {error && (
          <div style={{ marginBottom: '12px', fontSize: '12px', color: '#dc2626', padding: '8px', backgroundColor: '#fef2f2', borderRadius: '4px' }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
          <button
            onClick={onClose}
            style={{ padding: '8px 16px', fontSize: '13px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: 'pointer', backgroundColor: '#fff' }}
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading || selectedIds.size === 0}
            style={{
              padding: '8px 16px', fontSize: '13px', border: 'none', borderRadius: '4px',
              cursor: loading ? 'not-allowed' : 'pointer',
              backgroundColor: '#3b82f6', color: '#fff', opacity: selectedIds.size === 0 ? 0.5 : 1,
            }}
          >
            {loading ? 'Submitting...' : 'Submit for Approval'}
          </button>
        </div>
      </div>
    </div>
  );
};
