import React from 'react';
import type { WorkflowState } from '../types/workflow';

interface Props {
  state: WorkflowState;
}

const STATE_STYLES: Record<WorkflowState, { bg: string; text: string; label: string }> = {
  DRAFT: { bg: '#f3f4f6', text: '#374151', label: 'Draft' },
  REVIEW: { bg: '#fef3c7', text: '#92400e', label: 'In Review' },
  APPROVED: { bg: '#d1fae5', text: '#065f46', label: 'Approved' },
  PUBLISHED: { bg: '#dbeafe', text: '#1e40af', label: 'Published' },
  ARCHIVED: { bg: '#e5e7eb', text: '#6b7280', label: 'Archived' },
};

export const WorkflowStateBadge: React.FC<Props> = ({ state }) => {
  const style = STATE_STYLES[state] || STATE_STYLES.DRAFT;

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: '2px 8px',
        borderRadius: '9999px',
        fontSize: '12px',
        fontWeight: 500,
        backgroundColor: style.bg,
        color: style.text,
      }}
    >
      {style.label}
    </span>
  );
};
