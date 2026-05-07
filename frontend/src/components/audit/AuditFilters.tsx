import React from 'react';
import { AuditCategory } from '../../types/models';

interface AuditFiltersProps {
  category: string;
  eventType: string;
  outcome: string;
  dateFrom: string;
  dateTo: string;
  onFilterChange: (filters: {
    category?: string;
    eventType?: string;
    outcome?: string;
    dateFrom?: string;
    dateTo?: string;
  }) => void;
}

const CATEGORIES: AuditCategory[] = [
  'AUTHENTICATION',
  'FILE_OPERATION',
  'PERMISSION_CHANGE',
  'SHARING',
  'WORKFLOW',
  'SYSTEM',
];

const OUTCOMES = ['SUCCESS', 'FAILURE'];

export const AuditFilters: React.FC<AuditFiltersProps> = ({
  category,
  eventType,
  outcome,
  dateFrom,
  dateTo,
  onFilterChange,
}) => {
  const filterStyle: React.CSSProperties = {
    padding: '6px 10px',
    border: '1px solid #d1d5db',
    borderRadius: '6px',
    fontSize: '13px',
    minWidth: '140px',
  };

  return (
    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginBottom: '16px', alignItems: 'center' }}>
      <select
        value={category}
        onChange={(e) => onFilterChange({ category: e.target.value })}
        style={filterStyle}
      >
        <option value="">All Categories</option>
        {CATEGORIES.map((c) => (
          <option key={c} value={c}>{c.replace('_', ' ')}</option>
        ))}
      </select>

      <input
        type="text"
        value={eventType}
        onChange={(e) => onFilterChange({ eventType: e.target.value })}
        placeholder="Event type..."
        style={filterStyle}
      />

      <select
        value={outcome}
        onChange={(e) => onFilterChange({ outcome: e.target.value })}
        style={filterStyle}
      >
        <option value="">All Outcomes</option>
        {OUTCOMES.map((o) => (
          <option key={o} value={o}>{o}</option>
        ))}
      </select>

      <input
        type="date"
        value={dateFrom}
        onChange={(e) => onFilterChange({ dateFrom: e.target.value })}
        style={filterStyle}
      />
      <span style={{ color: '#6b7280' }}>to</span>
      <input
        type="date"
        value={dateTo}
        onChange={(e) => onFilterChange({ dateTo: e.target.value })}
        style={filterStyle}
      />
    </div>
  );
};
