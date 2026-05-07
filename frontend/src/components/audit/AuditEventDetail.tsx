import React from 'react';
import { AuditEvent } from '../../types/models';

interface AuditEventDetailProps {
  event: AuditEvent | null;
  onClose: () => void;
}

export const AuditEventDetail: React.FC<AuditEventDetailProps> = ({ event, onClose }) => {
  if (!event) return null;

  const rowStyle: React.CSSProperties = {
    display: 'flex',
    padding: '8px 0',
    borderBottom: '1px solid #f3f4f6',
  };

  const labelStyle: React.CSSProperties = {
    width: '140px',
    fontWeight: 500,
    color: '#374151',
    fontSize: '13px',
    flexShrink: 0,
  };

  const valueStyle: React.CSSProperties = {
    color: '#6b7280',
    fontSize: '13px',
    wordBreak: 'break-all',
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      right: 0,
      width: '420px',
      height: '100vh',
      background: 'white',
      boxShadow: '-4px 0 12px rgba(0,0,0,0.1)',
      padding: '24px',
      overflowY: 'auto',
      zIndex: 1000,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h3 style={{ margin: 0, fontSize: '16px' }}>Event Details</h3>
        <button
          onClick={onClose}
          style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: '#6b7280' }}
        >
          ×
        </button>
      </div>

      <div>
        <div style={rowStyle}><span style={labelStyle}>ID</span><span style={valueStyle}>{event.id}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Time</span><span style={valueStyle}>{new Date(event.createdAt).toLocaleString()}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Actor</span><span style={valueStyle}>{event.actorName || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Event Type</span><span style={valueStyle}>{event.eventType}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Category</span><span style={valueStyle}>{event.category}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Outcome</span><span style={valueStyle}>{event.outcome}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Resource Type</span><span style={valueStyle}>{event.resourceType || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Resource ID</span><span style={valueStyle}>{event.resourceId || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Resource Name</span><span style={valueStyle}>{event.resourceName || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>IP Address</span><span style={valueStyle}>{event.ipAddress || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>User Agent</span><span style={valueStyle}>{event.userAgent || '—'}</span></div>
        <div style={rowStyle}><span style={labelStyle}>Workspace</span><span style={valueStyle}>{event.workspaceId || '—'}</span></div>
        {event.details && (
          <div style={rowStyle}>
            <span style={labelStyle}>Details</span>
            <span style={valueStyle}>{event.details}</span>
          </div>
        )}
      </div>
    </div>
  );
};
