import React from 'react';

interface DuplicateFileDialogProps {
  fileName: string;
  onChoice: (choice: 'rename' | 'replace' | 'skip') => void;
  onClose: () => void;
}

const DuplicateFileDialog: React.FC<DuplicateFileDialogProps> = ({ fileName, onChoice, onClose }) => {
  return (
    <div style={{
      position: 'fixed', inset: 0, display: 'flex', alignItems: 'center',
      justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.4)', zIndex: 2000,
    }} onClick={onClose}>
      <div style={{
        backgroundColor: '#fff', borderRadius: 8, padding: 24, minWidth: 360, maxWidth: 420,
      }} onClick={e => e.stopPropagation()}>
        <h3 style={{ margin: '0 0 12px' }}>Duplicate File</h3>
        <p style={{ margin: '0 0 16px', color: '#666', fontSize: '14px' }}>
          A file named <strong>{fileName}</strong> already exists in this folder.
        </p>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button onClick={() => onChoice('skip')} style={{ padding: '8px 16px', cursor: 'pointer' }}>
            Skip
          </button>
          <button onClick={() => onChoice('rename')} style={{ padding: '8px 16px', cursor: 'pointer' }}>
            Keep Both
          </button>
          <button onClick={() => onChoice('replace')} style={{
            padding: '8px 16px', cursor: 'pointer', backgroundColor: '#d32f2f', color: '#fff', border: 'none', borderRadius: 4,
          }}>
            Replace
          </button>
        </div>
      </div>
    </div>
  );
};

export default DuplicateFileDialog;
