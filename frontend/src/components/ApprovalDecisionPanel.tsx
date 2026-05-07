import React, { useState } from 'react';
import { approvalsApi } from '../api/approvals';
import type { ApprovalRequest } from '../types/workflow';

interface Props {
  approval: ApprovalRequest;
  onDecided?: () => void;
}

export const ApprovalDecisionPanel: React.FC<Props> = ({ approval, onDecided }) => {
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDecision = async (decision: 'APPROVED' | 'REJECTED') => {
    setLoading(true);
    setError(null);
    try {
      await approvalsApi.decide(approval.id, decision, comment || undefined);
      onDecided?.();
    } catch (err: any) {
      setError(err.response?.data?.error?.message || 'Failed to submit decision');
    } finally {
      setLoading(false);
    }
  };

  if (approval.status !== 'PENDING') {
    return (
      <div style={{ padding: '12px', background: '#f9fafb', borderRadius: '6px', fontSize: '13px' }}>
        <strong>Status:</strong> {approval.status}
        {approval.completedAt && <span style={{ color: '#6b7280', marginLeft: '8px' }}>
          ({new Date(approval.completedAt).toLocaleString()})
        </span>}
      </div>
    );
  }

  return (
    <div style={{ padding: '12px', background: '#f9fafb', borderRadius: '6px' }}>
      <div style={{ marginBottom: '8px', fontSize: '13px' }}>
        <strong>{approval.submitterName}</strong> requested approval for <strong>{approval.fileName}</strong>
        {approval.comment && <div style={{ color: '#6b7280', marginTop: '4px' }}>"{approval.comment}"</div>}
      </div>

      <div style={{ marginBottom: '8px', fontSize: '12px', color: '#6b7280' }}>
        Reviewers: {approval.reviewers.map(r => (
          <span key={r.id} style={{
            display: 'inline-block', padding: '2px 6px', margin: '2px',
            background: r.decision === 'APPROVED' ? '#d1fae5' : r.decision === 'REJECTED' ? '#fee2e2' : '#e5e7eb',
            borderRadius: '4px',
          }}>
            {r.name} ({r.decision.toLowerCase()})
          </span>
        ))}
      </div>

      <textarea
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        placeholder="Add a comment (optional)"
        rows={2}
        style={{
          width: '100%', fontSize: '13px', padding: '8px',
          borderRadius: '4px', border: '1px solid #d1d5db', resize: 'vertical', marginBottom: '8px',
        }}
      />

      {error && (
        <div style={{ marginBottom: '8px', fontSize: '12px', color: '#dc2626' }}>{error}</div>
      )}

      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          onClick={() => handleDecision('APPROVED')}
          disabled={loading}
          style={{
            flex: 1, padding: '8px', fontSize: '13px', border: 'none', borderRadius: '4px',
            backgroundColor: '#10b981', color: '#fff', cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          Approve
        </button>
        <button
          onClick={() => handleDecision('REJECTED')}
          disabled={loading}
          style={{
            flex: 1, padding: '8px', fontSize: '13px', border: 'none', borderRadius: '4px',
            backgroundColor: '#ef4444', color: '#fff', cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          Reject
        </button>
      </div>
    </div>
  );
};
