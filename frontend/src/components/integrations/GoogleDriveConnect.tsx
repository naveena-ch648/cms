import { useState, useEffect } from 'react';
import { integrationsApi, IntegrationConnection } from '../../api/integrations';

export default function GoogleDriveConnect() {
  const [connections, setConnections] = useState<IntegrationConnection[]>([]);
  const [loading, setLoading] = useState(true);
  const [connecting, setConnecting] = useState(false);

  useEffect(() => {
    loadConnections();
  }, []);

  const loadConnections = async () => {
    try {
      const data = await integrationsApi.getConnections();
      setConnections(data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const handleConnect = async () => {
    setConnecting(true);
    try {
      const result = await integrationsApi.connectGoogleDrive();
      window.location.href = result.authorizationUrl;
    } catch {
      setConnecting(false);
    }
  };

  const handleDisconnect = async (connectionId: string) => {
    try {
      await integrationsApi.disconnectConnection(connectionId);
      setConnections(prev => prev.filter(c => c.id !== connectionId));
    } catch {
      // silent
    }
  };

  const driveConnection = connections.find(c => c.provider === 'GOOGLE_DRIVE' && c.status === 'ACTIVE');

  if (loading) {
    return <div style={{ padding: '16px', color: '#64748b' }}>Loading connections...</div>;
  }

  return (
    <div style={{ marginBottom: '24px' }}>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '16px 20px',
        background: '#fff',
        border: '1px solid #e2e8f0',
        borderRadius: '12px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '8px',
            background: '#f1f5f9',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '20px',
          }}>
            📁
          </div>
          <div>
            <div style={{ fontWeight: 600, color: '#1e293b', fontSize: '14px' }}>Google Drive</div>
            {driveConnection ? (
              <div style={{ fontSize: '12px', color: '#10b981' }}>
                Connected as {driveConnection.providerAccountId}
              </div>
            ) : (
              <div style={{ fontSize: '12px', color: '#64748b' }}>Not connected</div>
            )}
          </div>
        </div>

        {driveConnection ? (
          <button
            onClick={() => handleDisconnect(driveConnection.id)}
            style={{
              padding: '8px 16px',
              background: '#fef2f2',
              color: '#dc2626',
              border: '1px solid #fecaca',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            Disconnect
          </button>
        ) : (
          <button
            onClick={handleConnect}
            disabled={connecting}
            style={{
              padding: '8px 16px',
              background: '#3b82f6',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: connecting ? 'wait' : 'pointer',
              opacity: connecting ? 0.7 : 1,
            }}
          >
            {connecting ? 'Connecting...' : 'Connect'}
          </button>
        )}
      </div>
    </div>
  );
}
