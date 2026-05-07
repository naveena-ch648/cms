import React from 'react';
import type { Citation } from '../../api/qa';

interface CitationPanelProps {
  citation: Citation;
  onClose: () => void;
  onOpenDocument: (documentId: string, pageNumber: number) => void;
}

export const CitationPanel: React.FC<CitationPanelProps> = ({ citation, onClose, onOpenDocument }) => {
  return (
    <div style={{
      position: 'fixed',
      right: 0,
      top: 0,
      bottom: 0,
      width: '400px',
      backgroundColor: '#fff',
      borderLeft: '1px solid #e5e7eb',
      boxShadow: '-4px 0 12px rgba(0, 0, 0, 0.1)',
      zIndex: 1000,
      display: 'flex',
      flexDirection: 'column',
    }}>
      {/* Header */}
      <div style={{
        padding: '16px',
        borderBottom: '1px solid #e5e7eb',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
      }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600 }}>Source [{citation.index}]</h4>
          <p style={{ margin: '4px 0 0', fontSize: '12px', color: '#6b7280' }}>
            {citation.documentName}
            {citation.pageNumber > 0 && ` — Page ${citation.pageNumber}`}
          </p>
        </div>
        <button
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            fontSize: '20px',
            cursor: 'pointer',
            color: '#6b7280',
            padding: '4px',
          }}
        >
          ×
        </button>
      </div>

      {/* Excerpt */}
      <div style={{
        flex: 1,
        overflowY: 'auto',
        padding: '16px',
      }}>
        <div style={{ marginBottom: '12px' }}>
          <span style={{
            fontSize: '11px',
            textTransform: 'uppercase',
            fontWeight: 600,
            color: '#6b7280',
            letterSpacing: '0.5px',
          }}>
            Excerpt
          </span>
        </div>
        <div style={{
          padding: '12px',
          backgroundColor: '#fefce8',
          borderLeft: '3px solid #eab308',
          borderRadius: '4px',
          fontSize: '13px',
          lineHeight: '1.6',
          whiteSpace: 'pre-wrap',
        }}>
          {citation.excerpt}
        </div>
      </div>

      {/* Open Document Button */}
      <div style={{
        padding: '16px',
        borderTop: '1px solid #e5e7eb',
      }}>
        <button
          onClick={() => onOpenDocument(citation.documentId, citation.pageNumber)}
          style={{
            width: '100%',
            padding: '10px',
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontSize: '14px',
            fontWeight: 500,
            cursor: 'pointer',
          }}
        >
          Open Document {citation.pageNumber > 0 ? `at Page ${citation.pageNumber}` : ''}
        </button>
      </div>
    </div>
  );
};
