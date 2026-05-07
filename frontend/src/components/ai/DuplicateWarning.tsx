import React from 'react';
import type { DuplicateSuggestion } from '../../api/ai';

interface DuplicateWarningProps {
  duplicates: DuplicateSuggestion;
  onNavigateToFile?: (fileId: string) => void;
}

const DuplicateWarning: React.FC<DuplicateWarningProps> = ({ duplicates, onNavigateToFile }) => {
  if (!duplicates || duplicates.status !== 'COMPLETED') return null;
  if (!duplicates.exactMatch && (!duplicates.nearDuplicates || duplicates.nearDuplicates.length === 0)) return null;

  return (
    <div style={{ padding: 10, background: '#fff3cd', borderRadius: 6, border: '1px solid #ffc107', fontSize: 12 }}>
      <div style={{ fontWeight: 600, marginBottom: 6, color: '#856404' }}>
        ⚠️ Duplicate Detection
      </div>

      {duplicates.exactMatch && (
        <div style={{ marginBottom: 8, padding: 8, background: '#f8d7da', borderRadius: 4 }}>
          <div style={{ fontWeight: 600, color: '#721c24', marginBottom: 4 }}>Exact Duplicate Found</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>{duplicates.exactMatch.fileName}</span>
            <span style={{ fontSize: 10, color: '#666' }}>(100% match)</span>
            {onNavigateToFile && (
              <button
                onClick={() => onNavigateToFile(duplicates.exactMatch!.fileId)}
                style={{ fontSize: 10, padding: '2px 6px', cursor: 'pointer', border: '1px solid #007bff', borderRadius: 3, background: '#fff', color: '#007bff' }}
              >
                View Original
              </button>
            )}
          </div>
        </div>
      )}

      {duplicates.nearDuplicates && duplicates.nearDuplicates.length > 0 && (
        <div>
          <div style={{ fontWeight: 600, color: '#856404', marginBottom: 4 }}>Near Duplicates</div>
          {duplicates.nearDuplicates.map((dup, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4, padding: '4px 8px', background: '#fff8e1', borderRadius: 3 }}>
              <span>{dup.fileName}</span>
              <span style={{ fontSize: 10, color: '#666', fontWeight: 600 }}>
                {dup.similarity.toFixed(1)}% similar
              </span>
              {onNavigateToFile && (
                <button
                  onClick={() => onNavigateToFile(dup.fileId)}
                  style={{ fontSize: 10, padding: '1px 5px', cursor: 'pointer', border: '1px solid #6c757d', borderRadius: 3, background: '#fff', color: '#6c757d' }}
                >
                  View
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default DuplicateWarning;
