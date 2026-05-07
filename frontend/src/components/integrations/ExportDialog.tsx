import { useState } from 'react';
import { integrationsApi, IntegrationConnection } from '../../api/integrations';

interface ExportDialogProps {
  fileIds: string[];
  connection: IntegrationConnection;
  onClose: () => void;
  onSuccess: (jobId: string) => void;
}

export default function ExportDialog({ fileIds, connection, onClose, onSuccess }: ExportDialogProps) {
  const [targetDriveFolderId, setTargetDriveFolderId] = useState('root');
  const [conflictStrategy, setConflictStrategy] = useState('SKIP');
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleExport = async () => {
    setExporting(true);
    setError(null);
    try {
      const result = await integrationsApi.exportToDrive({
        connectionId: connection.id,
        fileIds,
        targetDriveFolderId,
        conflictStrategy,
      });
      onSuccess(result.jobId);
    } catch (e: any) {
      setError(e.response?.data?.message || 'Export failed');
    } finally {
      setExporting(false);
    }
  };

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
        width: '440px',
        padding: '24px',
        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.25)',
      }}>
        <h3 style={{ margin: '0 0 8px', fontSize: '18px', fontWeight: 600, color: '#1e293b' }}>
          Export to Google Drive
        </h3>
        <p style={{ margin: '0 0 20px', fontSize: '13px', color: '#64748b' }}>
          {fileIds.length} file{fileIds.length !== 1 ? 's' : ''} will be exported
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '4px' }}>
              Target Drive Folder ID
            </label>
            <input
              value={targetDriveFolderId}
              onChange={e => setTargetDriveFolderId(e.target.value)}
              placeholder="root"
              style={{
                width: '100%',
                padding: '8px 12px',
                border: '1px solid #e2e8f0',
                borderRadius: '8px',
                fontSize: '13px',
                outline: 'none',
                boxSizing: 'border-box',
              }}
            />
            <span style={{ fontSize: '11px', color: '#94a3b8' }}>Use "root" for My Drive root</span>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, color: '#475569', marginBottom: '8px' }}>
              If file already exists
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              {(['SKIP', 'REPLACE', 'RENAME'] as const).map(strategy => (
                <button
                  key={strategy}
                  onClick={() => setConflictStrategy(strategy)}
                  style={{
                    padding: '6px 12px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    border: conflictStrategy === strategy ? '1px solid #3b82f6' : '1px solid #e2e8f0',
                    background: conflictStrategy === strategy ? '#eff6ff' : '#fff',
                    color: conflictStrategy === strategy ? '#1d4ed8' : '#475569',
                    cursor: 'pointer',
                  }}
                >
                  {strategy.charAt(0) + strategy.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
          </div>
        </div>

        {error && (
          <div style={{ marginTop: '12px', color: '#dc2626', fontSize: '13px' }}>{error}</div>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '24px' }}>
          <button
            onClick={onClose}
            disabled={exporting}
            style={{ padding: '8px 16px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '8px', fontSize: '13px', cursor: 'pointer' }}
          >
            Cancel
          </button>
          <button
            onClick={handleExport}
            disabled={exporting}
            style={{
              padding: '8px 16px',
              background: exporting ? '#94a3b8' : '#3b82f6',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: exporting ? 'wait' : 'pointer',
            }}
          >
            {exporting ? 'Exporting...' : 'Export'}
          </button>
        </div>
      </div>
    </div>
  );
}
