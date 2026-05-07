import React from 'react';
import type { Citation } from '../../api/qa';

interface CitationLinkProps {
  citation: Citation;
  onClick: (citation: Citation) => void;
}

export const CitationLink: React.FC<CitationLinkProps> = ({ citation, onClick }) => {
  return (
    <span
      onClick={() => onClick(citation)}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: '18px',
        height: '18px',
        backgroundColor: '#dbeafe',
        color: '#2563eb',
        borderRadius: '4px',
        fontSize: '11px',
        fontWeight: 600,
        cursor: 'pointer',
        verticalAlign: 'super',
        marginLeft: '1px',
        marginRight: '1px',
        lineHeight: 1,
      }}
      title={`${citation.documentName}${citation.pageNumber > 0 ? ` (p. ${citation.pageNumber})` : ''}`}
    >
      {citation.index}
    </span>
  );
};
