import React, { useEffect, useState } from 'react';
import { approvalsApi } from '../api/approvals';
import type { ApprovalRequest } from '../types/workflow';

interface Props {
  onViewAll?: () => void;
}

export const PendingApprovalsWidget: React.FC<Props> = ({ onViewAll }) => {
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [count, setCount] = useState(0);

  useEffect(() => {
    approvalsApi.listPending(0, 5)
      .then(res => {
        setApprovals(res.data.data || []);
        setCount(res.data.meta?.pagination?.totalElements || res.data.data?.length || 0);
      })
      .catch(() => { setApprovals([]); setCount(0); });
  }, []);

  if (count === 0) return null;

  return (
    <div style={{ padding: '16px', background: '#fff', borderRadius: '8px', border: '1px solid #e5e7eb' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
        <h4 style={{ margin: 0, fontSize: '14px' }}>
          Pending Approvals
          <span style={{
            marginLeft: '8px', padding: '2px 8px', fontSize: '11px',
            background: '#fef3c7', color: '#92400e', borderRadius: '9999px',
          }}>
            {count}
          </span>
        </h4>
        {onViewAll && (
          <button
            onClick={onViewAll}
            style={{ fontSize: '12px', color: '#3b82f6', border: 'none', background: 'none', cursor: 'pointer' }}
          >
            View all
          </button>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {approvals.slice(0, 3).map(approval => (
          <div key={approval.id} style={{
            padding: '8px 12px', background: '#f9fafb', borderRadius: '4px', fontSize: '13px',
          }}>
            <div style={{ fontWeight: 500 }}>{approval.fileName}</div>
            <div style={{ color: '#6b7280', fontSize: '12px' }}>
              by {approval.submitterName} · {new Date(approval.createdAt).toLocaleDateString()}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
