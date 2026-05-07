import React, { useState } from 'react';
import { qaApi, type SummarizeResponse, type Citation } from '../../api/qa';
import { CitationLink } from './CitationLink';

interface SummarizeDialogProps {
  documentId: string;
  documentName: string;
  workspaceId: string;
  onClose: () => void;
  onCitationClick?: (citation: Citation) => void;
}

export const SummarizeDialog: React.FC<SummarizeDialogProps> = ({
  documentId,
  documentName,
  workspaceId,
  onClose,
  onCitationClick,
}) => {
  const [length, setLength] = useState<'short' | 'medium' | 'long'>('medium');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<SummarizeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSummarize = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await qaApi.summarize({
        documentId,
        workspaceId,
        length,
      });
      setResult(response);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to generate summary');
    } finally {
      setIsLoading(false);
    }
  };

  const renderSummaryWithCitations = (text: string, citations: Citation[]) => {
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

      if (citation && onCitationClick) {
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
      position: 'fixed',
      inset: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000,
    }}>
      <div style={{
        backgroundColor: '#fff',
        borderRadius: '12px',
        width: '600px',
        maxHeight: '80vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
      }}>
        {/* Header */}
        <div style={{
          padding: '20px 24px',
          borderBottom: '1px solid #e5e7eb',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>
          <div>
            <h3 style={{ margin: 0, fontSize: '18px' }}>Summarize Document</h3>
            <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#6b7280' }}>{documentName}</p>
          </div>
          <button onClick={onClose} style={{
            background: 'none', border: 'none', fontSize: '24px', cursor: 'pointer', color: '#6b7280',
          }}>×</button>
        </div>

        {/* Body */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '24px' }}>
          {!result && (
            <div>
              <label style={{ fontSize: '14px', fontWeight: 500, display: 'block', marginBottom: '8px' }}>
                Summary Length
              </label>
              <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
                {(['short', 'medium', 'long'] as const).map(opt => (
                  <button
                    key={opt}
                    onClick={() => setLength(opt)}
                    style={{
                      padding: '8px 16px',
                      borderRadius: '6px',
                      border: length === opt ? '2px solid #2563eb' : '1px solid #d1d5db',
                      backgroundColor: length === opt ? '#eff6ff' : '#fff',
                      color: length === opt ? '#2563eb' : '#374151',
                      cursor: 'pointer',
                      fontSize: '13px',
                      fontWeight: 500,
                      textTransform: 'capitalize',
                    }}
                  >
                    {opt}
                  </button>
                ))}
              </div>
              <button
                onClick={handleSummarize}
                disabled={isLoading}
                style={{
                  padding: '10px 20px',
                  backgroundColor: isLoading ? '#93c5fd' : '#2563eb',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '14px',
                  fontWeight: 500,
                  cursor: isLoading ? 'not-allowed' : 'pointer',
                }}
              >
                {isLoading ? 'Generating...' : 'Generate Summary'}
              </button>
            </div>
          )}

          {error && (
            <div style={{
              padding: '12px', backgroundColor: '#fef2f2', color: '#dc2626',
              borderRadius: '6px', fontSize: '13px', marginTop: '12px',
            }}>
              {error}
            </div>
          )}

          {result && (
            <div>
              <div style={{
                whiteSpace: 'pre-wrap',
                fontSize: '14px',
                lineHeight: '1.7',
                color: '#1f2937',
              }}>
                {renderSummaryWithCitations(result.summary, result.citations)}
              </div>
              {result.modelUsed && (
                <p style={{ marginTop: '16px', fontSize: '11px', color: '#9ca3af' }}>
                  Generated by {result.modelUsed} · ~{result.tokenCount} tokens
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
