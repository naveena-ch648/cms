import React from 'react';
import type { UploadProgress } from '../types/file';

interface UploadProgressPanelProps {
  uploads: UploadProgress[];
  onPause?: (fileId: string) => void;
  onResume?: (fileId: string) => void;
  onCancel?: (fileId: string) => void;
  onDismiss?: (fileId: string) => void;
}

const UploadProgressPanel: React.FC<UploadProgressPanelProps> = ({
  uploads, onPause, onResume, onCancel, onDismiss,
}) => {
  if (uploads.length === 0) return null;

  const activeCount = uploads.filter(u => u.status === 'uploading' || u.status === 'queued').length;
  const completedCount = uploads.filter(u => u.status === 'completed').length;

  return (
    <div style={{
      position: 'fixed', bottom: 0, right: 16, width: 360,
      maxHeight: 300, backgroundColor: '#fff', borderRadius: '8px 8px 0 0',
      boxShadow: '0 -2px 12px rgba(0,0,0,0.15)', overflow: 'hidden', zIndex: 1000,
    }}>
      <div style={{ padding: '12px 16px', backgroundColor: '#1976d2', color: '#fff', fontSize: '14px' }}>
        {activeCount > 0 ? `Uploading ${activeCount} file(s)...` : `${completedCount} upload(s) complete`}
      </div>
      <div style={{ maxHeight: 250, overflowY: 'auto' }}>
        {uploads.map(upload => (
          <div key={upload.fileId} style={{ padding: '8px 16px', borderBottom: '1px solid #eee' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '13px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 200 }}>
                {upload.fileName}
              </span>
              <span style={{ fontSize: '12px', color: '#666' }}>
                {upload.status === 'completed' ? '✓' : upload.status === 'failed' ? '✗' : `${upload.percentage}%`}
              </span>
            </div>
            {(upload.status === 'uploading' || upload.status === 'queued') && (
              <div style={{ marginTop: 4, height: 4, backgroundColor: '#e0e0e0', borderRadius: 2 }}>
                <div style={{ height: '100%', width: `${upload.percentage}%`, backgroundColor: '#1976d2', borderRadius: 2, transition: 'width 0.3s' }} />
              </div>
            )}
            {upload.status === 'failed' && (
              <div style={{ fontSize: '11px', color: '#d32f2f', marginTop: 2 }}>{upload.error}</div>
            )}
            <div style={{ marginTop: 4, display: 'flex', gap: 8 }}>
              {upload.status === 'uploading' && onPause && (
                <button onClick={() => onPause(upload.fileId)} style={{ fontSize: '11px', cursor: 'pointer' }}>Pause</button>
              )}
              {upload.status === 'paused' && onResume && (
                <button onClick={() => onResume(upload.fileId)} style={{ fontSize: '11px', cursor: 'pointer' }}>Resume</button>
              )}
              {(upload.status === 'uploading' || upload.status === 'paused' || upload.status === 'queued') && onCancel && (
                <button onClick={() => onCancel(upload.fileId)} style={{ fontSize: '11px', cursor: 'pointer' }}>Cancel</button>
              )}
              {(upload.status === 'completed' || upload.status === 'failed') && onDismiss && (
                <button onClick={() => onDismiss(upload.fileId)} style={{ fontSize: '11px', cursor: 'pointer' }}>Dismiss</button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default UploadProgressPanel;
