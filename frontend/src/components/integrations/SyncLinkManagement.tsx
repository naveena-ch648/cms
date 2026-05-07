import { useState, useEffect } from 'react';
import { integrationsApi, SyncLink, SyncJob } from '../../api/integrations';

export default function SyncLinkManagement() {
  const [syncLinks, setSyncLinks] = useState<SyncLink[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedLink, setSelectedLink] = useState<SyncLink | null>(null);
  const [jobs, setJobs] = useState<SyncJob[]>([]);

  useEffect(() => {
    loadSyncLinks();
  }, []);

  const loadSyncLinks = async () => {
    try {
      const data = await integrationsApi.getSyncLinks();
      setSyncLinks(data);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const handlePause = async (link: SyncLink) => {
    try {
      const updated = await integrationsApi.updateSyncLink(link.id, {
        status: link.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE',
      });
      setSyncLinks(prev => prev.map(l => l.id === link.id ? { ...l, status: updated.status } : l));
    } catch {
      // silent
    }
  };

  const handleDelete = async (linkId: string) => {
    try {
      await integrationsApi.deleteSyncLink(linkId);
      setSyncLinks(prev => prev.filter(l => l.id !== linkId));
      if (selectedLink?.id === linkId) setSelectedLink(null);
    } catch {
      // silent
    }
  };

  const handleViewJobs = async (link: SyncLink) => {
    setSelectedLink(link);
    try {
      const data = await integrationsApi.getSyncJobs(link.id);
      setJobs(data);
    } catch {
      setJobs([]);
    }
  };

  if (loading) {
    return <div style={{ padding: '16px', color: '#64748b' }}>Loading sync links...</div>;
  }

  return (
    <div>
      <h2 style={{ fontSize: '16px', fontWeight: 600, color: '#1e293b', marginBottom: '16px' }}>
        Sync Links
      </h2>

      {syncLinks.length === 0 ? (
        <div style={{
          padding: '32px',
          textAlign: 'center',
          color: '#64748b',
          background: '#f8fafc',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          fontSize: '13px',
        }}>
          No sync links configured. Set up a sync to keep folders in sync with Google Drive.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {syncLinks.map(link => (
            <div key={link.id} style={{
              padding: '16px 20px',
              background: '#fff',
              border: '1px solid #e2e8f0',
              borderRadius: '12px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ fontWeight: 600, fontSize: '14px', color: '#1e293b' }}>
                      {link.folderName || link.folderId}
                    </span>
                    <span style={{ color: '#94a3b8' }}>↔</span>
                    <span style={{ fontSize: '14px', color: '#475569' }}>
                      {link.externalFolderName || 'Drive Folder'}
                    </span>
                  </div>
                  <div style={{ display: 'flex', gap: '12px', marginTop: '6px', fontSize: '12px', color: '#64748b' }}>
                    <span>Direction: {link.direction.replace('_', ' ').toLowerCase()}</span>
                    <span>Every {link.syncIntervalMinutes} min</span>
                    {link.lastSyncAt && <span>Last sync: {new Date(link.lastSyncAt).toLocaleString()}</span>}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{
                    padding: '4px 8px',
                    borderRadius: '6px',
                    fontSize: '11px',
                    fontWeight: 600,
                    background: link.status === 'ACTIVE' ? '#ecfdf5' : link.status === 'PAUSED' ? '#fef3c7' : '#fef2f2',
                    color: link.status === 'ACTIVE' ? '#065f46' : link.status === 'PAUSED' ? '#92400e' : '#991b1b',
                  }}>
                    {link.status}
                  </span>
                  <button
                    onClick={() => handleViewJobs(link)}
                    style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '12px', cursor: 'pointer' }}
                  >
                    History
                  </button>
                  <button
                    onClick={() => handlePause(link)}
                    style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '12px', cursor: 'pointer' }}
                  >
                    {link.status === 'ACTIVE' ? 'Pause' : 'Resume'}
                  </button>
                  <button
                    onClick={() => handleDelete(link.id)}
                    style={{ padding: '6px 10px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', fontSize: '12px', color: '#dc2626', cursor: 'pointer' }}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Sync Jobs panel */}
      {selectedLink && (
        <div style={{ marginTop: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 600, color: '#1e293b' }}>
              Sync History: {selectedLink.folderName || selectedLink.folderId}
            </h3>
            <button
              onClick={() => setSelectedLink(null)}
              style={{ padding: '4px 8px', background: 'none', border: 'none', color: '#64748b', cursor: 'pointer', fontSize: '12px' }}
            >
              Close
            </button>
          </div>
          {jobs.length === 0 ? (
            <div style={{ padding: '20px', color: '#64748b', fontSize: '13px', background: '#f8fafc', borderRadius: '8px' }}>
              No sync jobs yet.
            </div>
          ) : (
            <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden' }}>
              {jobs.map((job, idx) => (
                <div key={job.id} style={{
                  padding: '12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  borderBottom: idx < jobs.length - 1 ? '1px solid #f1f5f9' : 'none',
                  fontSize: '13px',
                }}>
                  <span style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    background: job.status === 'COMPLETED' ? '#10b981' : job.status === 'FAILED' ? '#ef4444' : '#f59e0b',
                  }} />
                  <span style={{ fontWeight: 500, color: '#1e293b' }}>{job.direction}</span>
                  <span style={{ color: '#475569' }}>
                    {job.itemsSynced} synced
                    {job.itemsFailed > 0 && `, ${job.itemsFailed} failed`}
                    {job.itemsConflicted > 0 && `, ${job.itemsConflicted} conflicts`}
                  </span>
                  <span style={{ marginLeft: 'auto', color: '#94a3b8', fontSize: '11px' }}>
                    {new Date(job.startedAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
