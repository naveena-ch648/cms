import React from 'react';
import type { Citation } from '../../api/qa';
import { CitationLink } from './CitationLink';

interface MessageBubbleProps {
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations?: Citation[] | null;
  isLoading?: boolean;
  onCitationClick?: (citation: Citation) => void;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ role, content, citations, isLoading, onCitationClick }) => {
  const isUser = role === 'USER';

  const renderContentWithCitations = (text: string) => {
    if (!citations || citations.length === 0 || !onCitationClick) {
      return <span>{text}</span>;
    }

    // Replace [1], [2], etc. with CitationLink components
    const parts: React.ReactNode[] = [];
    const regex = /\[(\d+)\]/g;
    let lastIndex = 0;
    let match;

    while ((match = regex.exec(text)) !== null) {
      const index = parseInt(match[1] ?? '0', 10);
      const citation = citations.find(c => c.index === index);

      if (lastIndex < match.index) {
        parts.push(text.slice(lastIndex, match.index));
      }

      if (citation) {
        parts.push(
          <CitationLink key={`cite-${match.index}`} citation={citation} onClick={onCitationClick} />
        );
      } else {
        parts.push(match[0]);
      }

      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < text.length) {
      parts.push(text.slice(lastIndex));
    }

    return <>{parts}</>;
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      marginBottom: '12px',
    }}>
      <div style={{
        maxWidth: '75%',
        padding: '12px 16px',
        borderRadius: isUser ? '16px 16px 4px 16px' : '16px 16px 16px 4px',
        backgroundColor: isUser ? '#2563eb' : '#f3f4f6',
        color: isUser ? '#fff' : '#1f2937',
        fontSize: '14px',
        lineHeight: '1.5',
      }}>
        {isLoading ? (
          <div style={{ display: 'flex', gap: '4px', padding: '4px 0' }}>
            <span className="dot-animation">●</span>
            <span className="dot-animation" style={{ animationDelay: '0.2s' }}>●</span>
            <span className="dot-animation" style={{ animationDelay: '0.4s' }}>●</span>
          </div>
        ) : (
          <>
            <div style={{ whiteSpace: 'pre-wrap' }}>{renderContentWithCitations(content)}</div>
            {citations && citations.length > 0 && (
              <div style={{
                marginTop: '8px',
                paddingTop: '8px',
                borderTop: '1px solid #e5e7eb',
                fontSize: '12px',
                color: '#6b7280',
              }}>
                <div style={{ fontWeight: 600, marginBottom: '4px' }}>Sources:</div>
                {citations.map((citation) => (
                  <div key={citation.chunkId} style={{ marginBottom: '4px' }}>
                    [{citation.index}] {citation.documentName}
                    {citation.pageNumber > 0 && ` (p. ${citation.pageNumber})`}
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
