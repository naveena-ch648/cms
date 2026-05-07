import React, { useEffect, useState } from 'react';
import { workflowApi } from '../api/workflow';
import { WorkflowStateBadge } from './WorkflowStateBadge';
import type { WorkflowTransition, WorkflowState } from '../types/workflow';

interface Props {
  fileId: string;
}

export const WorkflowHistoryPanel: React.FC<Props> = ({ fileId }) => {
  const [history, setHistory] = useState<WorkflowTransition[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    workflowApi.getHistory(fileId)
      .then(res => setHistory(res.data.data || []))
      .catch(() => setHistory([]))
      .finally(() => setLoading(false));
  }, [fileId]);

  if (loading) {
    return <div style={{ fontSize: '13px', color: '#6b7280' }}>Loading history...</div>;
  }

  if (history.length === 0) {
    return <div style={{ fontSize: '13px', color: '#6b7280' }}>No workflow history yet</div>;
  }

  return (
    <div>
      <h4 style={{ margin: '0 0 12px', fontSize: '14px' }}>Workflow History</h4>
      <div style={{ position: 'relative', paddingLeft: '20px' }}>
        {/* Timeline line */}
        <div style={{
          position: 'absolute', left: '8px', top: '4px', bottom: '4px',
          width: '2px', backgroundColor: '#e5e7eb',
        }} />

        {history.map((transition, index) => (
          <div key={transition.id} style={{ position: 'relative', marginBottom: '16px' }}>
            {/* Timeline dot */}
            <div style={{
              position: 'absolute', left: '-16px', top: '6px',
              width: '10px', height: '10px', borderRadius: '50%',
              backgroundColor: index === 0 ? '#3b82f6' : '#d1d5db',
              border: '2px solid #fff',
            }} />

            <div style={{ fontSize: '13px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                <WorkflowStateBadge state={transition.fromState as WorkflowState} />
                <span style={{ color: '#6b7280' }}>→</span>
                <WorkflowStateBadge state={transition.toState as WorkflowState} />
              </div>
              <div style={{ marginTop: '4px', color: '#374151' }}>
                <strong>{transition.actorName}</strong>
                <span style={{ color: '#6b7280', marginLeft: '6px', fontSize: '12px' }}>
                  {new Date(transition.createdAt).toLocaleString()}
                </span>
              </div>
              {transition.comment && (
                <div style={{ marginTop: '4px', fontSize: '12px', color: '#6b7280', fontStyle: 'italic' }}>
                  "{transition.comment}"
                </div>
              )}
              {transition.approvalRequestId && (
                <div style={{
                  marginTop: '4px', fontSize: '11px', padding: '4px 8px',
                  background: '#d1fae5', borderRadius: '4px', display: 'inline-block', color: '#065f46',
                }}>
                  via approval
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
