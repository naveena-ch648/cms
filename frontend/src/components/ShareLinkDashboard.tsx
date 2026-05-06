import React, { useEffect, useState } from 'react';
import { listShareLinks, revokeShareLink, updateShareLink } from '../api/sharing';
import type { ShareLink } from '../types/sharing';

interface ShareLinkDashboardProps {
  workspaceId: string;
}

const ShareLinkDashboard: React.FC<ShareLinkDashboardProps> = ({ workspaceId }) => {
  const [links, setLinks] = useState<ShareLink[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadLinks();
  }, [workspaceId, statusFilter, page]);

  const loadLinks = async () => {
    setLoading(true);
    try {
      const result = await listShareLinks(workspaceId, statusFilter || undefined, page);
      setLinks(result.content);
      setTotalPages(result.totalPages);
    } catch (error) {
      console.error('Failed to load share links:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleRevoke = async (uuid: string) => {
    if (!confirm('Are you sure you want to revoke this share link?')) return;
    try {
      await revokeShareLink(uuid);
      await loadLinks();
    } catch (error) {
      console.error('Failed to revoke link:', error);
    }
  };

  const handleExtendExpiry = async (uuid: string) => {
    const newExpiry = new Date();
    newExpiry.setDate(newExpiry.getDate() + 7);
    try {
      await updateShareLink(uuid, { expiresAt: newExpiry.toISOString() });
      await loadLinks();
    } catch (error) {
      console.error('Failed to extend expiry:', error);
    }
  };

  const getStatusBadge = (link: ShareLink) => {
    const statusClasses: Record<string, string> = {
      ACTIVE: 'badge-active',
      REVOKED: 'badge-revoked',
      EXPIRED: 'badge-expired',
    };
    return (
      <span className={`badge ${statusClasses[link.status] || ''}`}>
        {link.status}
      </span>
    );
  };

  return (
    <div className="share-link-dashboard">
      <div className="dashboard-header">
        <h2>Share Links</h2>
        <div className="filters">
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          >
            <option value="">All</option>
            <option value="ACTIVE">Active</option>
            <option value="REVOKED">Revoked</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="loading">Loading...</div>
      ) : links.length === 0 ? (
        <div className="empty-state">
          <p>No share links found</p>
        </div>
      ) : (
        <>
          <table className="share-links-table">
            <thead>
              <tr>
                <th>Resource</th>
                <th>Type</th>
                <th>Status</th>
                <th>Views</th>
                <th>Password</th>
                <th>Expires</th>
                <th>Last Accessed</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {links.map((link) => (
                <tr key={link.uuid}>
                  <td className="resource-name">{link.resourceName}</td>
                  <td>{link.resourceType}</td>
                  <td>{getStatusBadge(link)}</td>
                  <td>{link.viewCount}</td>
                  <td>{link.hasPassword ? '🔒' : '—'}</td>
                  <td>
                    {link.expiresAt
                      ? new Date(link.expiresAt).toLocaleDateString()
                      : 'Never'}
                  </td>
                  <td>
                    {link.lastAccessedAt
                      ? new Date(link.lastAccessedAt).toLocaleDateString()
                      : 'Never'}
                  </td>
                  <td>{new Date(link.createdAt).toLocaleDateString()}</td>
                  <td className="actions">
                    {link.status === 'ACTIVE' && (
                      <>
                        <button
                          className="btn-sm btn-copy"
                          onClick={() => navigator.clipboard.writeText(link.url)}
                          title="Copy URL"
                        >
                          📋
                        </button>
                        <button
                          className="btn-sm btn-extend"
                          onClick={() => handleExtendExpiry(link.uuid)}
                          title="Extend 7 days"
                        >
                          ⏰
                        </button>
                        <button
                          className="btn-sm btn-revoke"
                          onClick={() => handleRevoke(link.uuid)}
                          title="Revoke"
                        >
                          ❌
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </button>
              <span>Page {page + 1} of {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default ShareLinkDashboard;
