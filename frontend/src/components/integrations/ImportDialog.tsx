import { useState } from 'react';
import { integrationsApi, DriveItem, IntegrationConnection } from '../../api/integrations';

interface ImportDialogProps {
  selectedFiles: DriveItem[];
  connection: IntegrationConnection;
  targetFolderId: string;
  onClose: () => void;
  onSuccess: (jobId: string) => void;
}

export default function ImportDialog({ selectedFiles, connection, targetFolderId, onClose, onSuccess }: ImportDialogProps) {
  const [preserveStructure, setPreserveStructure] = useState(false);
  const [importing, setImporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleImport = async () => {
    setImporting(true);
    setError(null);
    try {
      const result = await integrationsApi.importFromDrive({
        connectionId: connection.id,
        driveFileIds: selectedFiles.map(f => f.id),
        targetFolderId,
        preserveStructure,
      });
      onSuccess(result.jobId);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Import failed');
    } finally {
      setImporting(false);
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes === 0) return '—';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
    return `${size.toFixed(1)} ${units[i]}`;
  };

  const totalSize = selectedFiles.reduce((sum, f) => sum + (f.size || 0), 0);

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1001,
    }}>
      <div style={{
        background: '#fff',
        borderRadius: '16px',
        width: '480px',
        maxHeight: '70vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)',
      }}>
        {/* Header */}
        <div style={{ padding: '20px 24px', borderBottom: '1px solid #e2e8f0' }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>
            Import from Google Drive
          </h3>
          <p style={{ margin: '8px 0 0', fontSize: '13px', color: '#64748b' }}>
            {selectedFiles.length} file{selectedFiles.length !== 1 ? 's' : ''} • {formatSize(totalSize)} total
          </p>
        </div>

        {/* File list */}
        <div style={{ flex: 1, overflow: 'auto', padding: '12px 24px' }}>
          {selectedFiles.map(file => (
            <div key={file.id} style={{
              display: 'flex',
              alignItems: 'center',
              padding: '8px 0',
              borderBottom: '1px solid #f1f5f9',
            }}>
              <span style={{ marginRight: '8px' }}>📄</span>
              <span style={{ flex: 1, fontSize: '13px', color: '#1e293b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {file.name}
              </span>
              <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                {formatSize(file.size)}
              </span>
            </div>
          ))}
        </div>

        {/* Options */}
        <div style={{ padding: '12px 24px', borderTop: '1px solid #f1f5f9' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={preserveStructure}
              onChange={e => setPreserveStructure(e.target.checked)}
            />
            <span style={{ fontSize: '13px', color: '#475569' }}>Preserve folder structure</span>
          </label>
        </div>

        {/* Error */}
        {error && (
          <div style={{ padding: '0 24px 12px', color: '#dc2626', fontSize: '13px' }}>
            {error}
          </div>
        )}

        {/* Footer */}
        <div style={{
          padding: '16px 24px',
          borderTop: '1px solid #e2e8f0',
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '8px',
        }}>
          <button
            onClick={onClose}
            disabled={importing}
            style={{
              padding: '8px 16px',
              background: '#f1f5f9',
              border: '1px solid #e2e8f0',
              borderRadius: '8px',
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            Cancel
          </button>
          <button
            onClick={handleImport}
            disabled={importing}
            style={{
              padding: '8px 16px',
              background: importing ? '#94a3b8' : '#3b82f6',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: importing ? 'wait' : 'pointer',
            }}
          >
            {importing ? 'Importing...' : 'Import Files'}
          </button>
        </div>
      </div>
    </div>
  );
}
