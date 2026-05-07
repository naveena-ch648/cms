import React, { useEffect, useState } from 'react';
import { approvalsApi } from '../api/approvals';
import { ApprovalDecisionPanel } from '../components/ApprovalDecisionPanel';
import type { ApprovalRequest } from '../types/workflow';

const PendingApprovalsPage: React.FC = () => {
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchApprovals = () => {
    setLoading(true);
    approvalsApi.listPending(page, 20)
      .then(res => {
        setApprovals(res.data.data || []);
        setTotalPages(res.data.meta?.pagination?.totalPages || 1);
      })
      .catch(() => setApprovals([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchApprovals();
  }, [page]);

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '16px' }}>Pending Approvals</h2>

      {loading && <div style={{ color: '#6b7280' }}>Loading...</div>}

      {!loading && approvals.length === 0 && (
        <div style={{ padding: '24px', textAlign: 'center', color: '#6b7280' }}>
          No pending approvals
        </div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {approvals.map(approval => (
          <ApprovalDecisionPanel
            key={approval.id}
            approval={approval}
            onDecided={fetchApprovals}
          />
        ))}
      </div>

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '16px' }}>
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            style={{ padding: '6px 12px', fontSize: '13px', cursor: page === 0 ? 'not-allowed' : 'pointer' }}
          >
            Previous
          </button>
          <span style={{ fontSize: '13px', padding: '6px 12px' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => p + 1)}
            disabled={page >= totalPages - 1}
            style={{ padding: '6px 12px', fontSize: '13px', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer' }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default PendingApprovalsPage;
