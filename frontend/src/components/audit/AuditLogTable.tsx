import React from 'react';
import { AuditEvent } from '../../types/models';

interface AuditLogTableProps {
  events: AuditEvent[];
  total: number;
  page: number;
  size: number;
  onPageChange: (page: number) => void;
  onEventClick: (event: AuditEvent) => void;
  loading?: boolean;
}

export const AuditLogTable: React.FC<AuditLogTableProps> = ({
  events,
  total,
  page,
  size,
  onPageChange,
  onEventClick,
  loading,
}) => {
  const totalPages = Math.ceil(total / size);

  const cellStyle: React.CSSProperties = {
    padding: '10px 12px',
    borderBottom: '1px solid #e5e7eb',
    fontSize: '13px',
  };

  const headerStyle: React.CSSProperties = {
    ...cellStyle,
    fontWeight: 600,
    background: '#f9fafb',
    color: '#374151',
  };

  if (loading) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>Loading...</div>;
  }

  if (events.length === 0) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#6b7280' }}>No audit events found.</div>;
  }

  return (
    <div>
      <table style={{ width: '100%', borderCollapse: 'collapse', border: '1px solid #e5e7eb', borderRadius: '8px' }}>
        <thead>
          <tr>
            <th style={headerStyle}>Time</th>
            <th style={headerStyle}>Actor</th>
            <th style={headerStyle}>Event</th>
            <th style={headerStyle}>Category</th>
            <th style={headerStyle}>Resource</th>
            <th style={headerStyle}>Outcome</th>
            <th style={headerStyle}>IP</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr
              key={event.id}
              onClick={() => onEventClick(event)}
              style={{ cursor: 'pointer' }}
              onMouseOver={(e) => (e.currentTarget.style.background = '#f3f4f6')}
              onMouseOut={(e) => (e.currentTarget.style.background = '')}
            >
              <td style={cellStyle}>{new Date(event.createdAt).toLocaleString()}</td>
              <td style={cellStyle}>{event.actorName || '—'}</td>
              <td style={cellStyle}><code style={{ fontSize: '12px' }}>{event.eventType}</code></td>
              <td style={cellStyle}>
                <span style={{
                  padding: '2px 8px',
                  borderRadius: '12px',
                  background: '#e0e7ff',
                  color: '#3730a3',
                  fontSize: '11px',
                  fontWeight: 500,
                }}>
                  {event.category}
                </span>
              </td>
              <td style={cellStyle}>{event.resourceName || event.resourceType || '—'}</td>
              <td style={cellStyle}>
                <span style={{
                  color: event.outcome === 'SUCCESS' ? '#059669' : '#dc2626',
                  fontWeight: 500,
                }}>
                  {event.outcome}
                </span>
              </td>
              <td style={cellStyle}>{event.ipAddress || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px' }}>
          <span style={{ fontSize: '13px', color: '#6b7280' }}>
            Showing {page * size + 1}–{Math.min((page + 1) * size, total)} of {total}
          </span>
          <div style={{ display: 'flex', gap: '4px' }}>
            <button
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              style={{ padding: '6px 12px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: page === 0 ? 'not-allowed' : 'pointer', opacity: page === 0 ? 0.5 : 1 }}
            >
              Previous
            </button>
            <button
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              style={{ padding: '6px 12px', border: '1px solid #d1d5db', borderRadius: '4px', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer', opacity: page >= totalPages - 1 ? 0.5 : 1 }}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
