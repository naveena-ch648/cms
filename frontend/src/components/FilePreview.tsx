import React, { useEffect, useState } from 'react';
import { filesApi } from '../api/files';
import type { FileInfo } from '../types/file';

interface FilePreviewProps {
  file: FileInfo;
  onClose: () => void;
}

const FilePreview: React.FC<FilePreviewProps> = ({ file, onClose }) => {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    filesApi.getPreviewUrl(file.id)
      .then(data => { setPreviewUrl(data.previewUrl); setError(null); })
      .catch(() => setError('Failed to load preview'))
      .finally(() => setLoading(false));
  }, [file.id]);

  const renderContent = () => {
    if (loading) return <div style={{ padding: 40, textAlign: 'center' }}>Loading preview...</div>;
    if (error || !previewUrl) return <div style={{ padding: 40, textAlign: 'center', color: '#d32f2f' }}>{error || 'No preview available'}</div>;

    if (file.mimeType.startsWith('image/')) {
      return <img src={previewUrl} alt={file.name} style={{ maxWidth: '100%', maxHeight: '80vh', objectFit: 'contain' }} />;
    }
    if (file.mimeType === 'application/pdf') {
      return <iframe src={previewUrl} title={file.name} style={{ width: '100%', height: '80vh', border: 'none' }} />;
    }
    if (file.mimeType.startsWith('text/')) {
      return <iframe src={previewUrl} title={file.name} style={{ width: '100%', height: '80vh', border: 'none' }} />;
    }
    return <div style={{ padding: 40, textAlign: 'center' }}>Preview not available for this file type</div>;
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
      backgroundColor: 'rgba(0,0,0,0.7)', zIndex: 3000,
    }} onClick={onClose}>
      <div style={{ backgroundColor: '#fff', borderRadius: 8, padding: 16, maxWidth: '90vw', maxHeight: '90vh', overflow: 'auto' }}
        onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
          <h3 style={{ margin: 0 }}>{file.name}</h3>
          <button onClick={onClose} style={{ cursor: 'pointer', border: 'none', background: 'none', fontSize: 18 }}>✕</button>
        </div>
        {renderContent()}
      </div>
    </div>
  );
};

export default FilePreview;
