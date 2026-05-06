import React from 'react';
import type { FileInfo } from '../types/file';
import FileVersionHistory from './FileVersionHistory';

interface FileDetailPanelProps {
  file: FileInfo;
  onClose: () => void;
  onDownload: (file: FileInfo) => void;
  onPreview: (file: FileInfo) => void;
  onFileUpdated?: () => void;
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]!}`;
}

const FileDetailPanel: React.FC<FileDetailPanelProps> = ({ file, onClose, onDownload, onPreview, onFileUpdated }) => {
  return (
    <div style={{
      width: 320, borderLeft: '1px solid #e0e0e0', padding: 16, overflowY: 'auto',
      display: 'flex', flexDirection: 'column', gap: 12,
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ margin: 0, fontSize: 16 }}>{file.name}</h3>
        <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: 18 }}>✕</button>
      </div>

      <div style={{ fontSize: 13, color: '#666', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div><strong>Size:</strong> {formatSize(file.sizeBytes)}</div>
        <div><strong>Type:</strong> {file.mimeType}</div>
        <div><strong>Uploaded by:</strong> {file.uploadedBy.name}</div>
        <div><strong>Date:</strong> {new Date(file.createdAt).toLocaleString()}</div>
        {file.description && <div><strong>Description:</strong> {file.description}</div>}
        {file.tags && file.tags.length > 0 && (
          <div><strong>Tags:</strong> {file.tags.join(', ')}</div>
        )}
        <div><strong>Downloads:</strong> {file.downloadCount}</div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button onClick={() => onDownload(file)} style={{ flex: 1, padding: '8px 12px', cursor: 'pointer' }}>
          ⬇️ Download
        </button>
        {file.previewable && (
          <button onClick={() => onPreview(file)} style={{ flex: 1, padding: '8px 12px', cursor: 'pointer' }}>
            👁️ Preview
          </button>
        )}
      </div>

      <div style={{ marginTop: 16, borderTop: '1px solid #e0e0e0', paddingTop: 12 }}>
        <FileVersionHistory fileId={file.id} onVersionRestored={onFileUpdated} />
      </div>
    </div>
  );
};

export default FileDetailPanel;
