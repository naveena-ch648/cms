import { useState, useEffect } from 'react';
import { integrationsApi, IntegrationConnection, DriveItem } from '../api/integrations';
import GoogleDriveConnect from '../components/integrations/GoogleDriveConnect';
import DriveFileBrowser from '../components/integrations/DriveFileBrowser';
import ImportDialog from '../components/integrations/ImportDialog';
import WebhookManagement from '../components/integrations/WebhookManagement';
import SyncLinkManagement from '../components/integrations/SyncLinkManagement';

export default function IntegrationsPage() {
  const [connections, setConnections] = useState<IntegrationConnection[]>([]);
  const [showBrowser, setShowBrowser] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<DriveItem[]>([]);
  const [jobMessage, setJobMessage] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'connections' | 'webhooks' | 'sync'>('connections');

  useEffect(() => {
    integrationsApi.getConnections()
      .then(data => setConnections(data))
      .catch(() => {});
  }, []);

  const driveConnection = connections.find(c => c.provider === 'GOOGLE_DRIVE' && c.status === 'ACTIVE');

  const handleFilesSelected = (items: DriveItem[]) => {
    setSelectedFiles(items);
    setShowBrowser(false);
    setShowImport(true);
  };

  const handleImportSuccess = (jobId: string) => {
    setShowImport(false);
    setSelectedFiles([]);
    setJobMessage(`Import started (Job: ${jobId.substring(0, 8)}...)`);
    setTimeout(() => setJobMessage(null), 5000);
  };

  // Handle OAuth callback (check URL params on mount)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    const state = params.get('state');
    if (code && state) {
      // Clean URL
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  const tabs = [
    { key: 'connections' as const, label: 'Connections' },
    { key: 'webhooks' as const, label: 'Webhooks' },
    { key: 'sync' as const, label: 'Sync' },
  ];

  return (
    <div style={{ padding: '32px', maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{
          fontSize: '24px',
          fontWeight: 700,
          color: '#1e293b',
          margin: 0,
        }}>
          Integrations
        </h1>
        <p style={{ color: '#64748b', margin: '8px 0 0', fontSize: '14px' }}>
          Connect external services to import, export, and sync your files.
        </p>
      </div>

      {/* Success message */}
      {jobMessage && (
        <div style={{
          padding: '12px 16px',
          background: '#ecfdf5',
          border: '1px solid #a7f3d0',
          borderRadius: '8px',
          color: '#065f46',
          fontSize: '13px',
          marginBottom: '16px',
        }}>
          {jobMessage}
        </div>
      )}

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '4px', marginBottom: '24px', borderBottom: '1px solid #e2e8f0', paddingBottom: '0' }}>
        {tabs.map(tab => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            style={{
              padding: '10px 16px',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab.key ? '2px solid #3b82f6' : '2px solid transparent',
              color: activeTab === tab.key ? '#1e293b' : '#64748b',
              fontWeight: activeTab === tab.key ? 600 : 400,
              fontSize: '14px',
              cursor: 'pointer',
              marginBottom: '-1px',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'connections' && (
        <>
          <section style={{ marginBottom: '32px' }}>
            <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#1e293b', marginBottom: '16px' }}>
              Connected Services
            </h2>
            <GoogleDriveConnect />
          </section>

          {driveConnection && (
            <section style={{ marginBottom: '32px' }}>
              <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#1e293b', marginBottom: '16px' }}>
                Quick Actions
              </h2>
              <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                <button
                  onClick={() => setShowBrowser(true)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '12px 20px',
                    background: '#fff',
                    border: '1px solid #e2e8f0',
                    borderRadius: '10px',
                    fontSize: '14px',
                    fontWeight: 500,
                    color: '#1e293b',
                    cursor: 'pointer',
                  }}
                >
                  <span style={{ fontSize: '18px' }}>⬇️</span>
                  Import from Drive
                </button>
              </div>
            </section>
          )}
        </>
      )}

      {activeTab === 'webhooks' && <WebhookManagement />}
      {activeTab === 'sync' && <SyncLinkManagement />}

      {/* Modals */}
      {showBrowser && (
        <DriveFileBrowser
          onSelect={handleFilesSelected}
          onCancel={() => setShowBrowser(false)}
        />
      )}

      {showImport && driveConnection && (
        <ImportDialog
          selectedFiles={selectedFiles}
          connection={driveConnection}
          targetFolderId=""
          onClose={() => setShowImport(false)}
          onSuccess={handleImportSuccess}
        />
      )}
    </div>
  );
}
