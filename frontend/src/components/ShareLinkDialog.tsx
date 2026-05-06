import React, { useState } from 'react';
import { createShareLink } from '../api/sharing';
import type { CreateShareLinkRequest, ShareLink } from '../types/sharing';

interface ShareLinkDialogProps {
  workspaceId: string;
  resourceType: 'FILE' | 'FOLDER';
  resourceUuid: string;
  resourceName: string;
  open: boolean;
  onClose: () => void;
  onCreated?: (link: ShareLink) => void;
}

const ShareLinkDialog: React.FC<ShareLinkDialogProps> = ({
  workspaceId,
  resourceType,
  resourceUuid,
  resourceName,
  open,
  onClose,
  onCreated,
}) => {
  const [password, setPassword] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [allowDownload, setAllowDownload] = useState(true);
  const [watermarkEnabled, setWatermarkEnabled] = useState(false);
  const [loading, setLoading] = useState(false);
  const [createdLink, setCreatedLink] = useState<ShareLink | null>(null);
  const [error, setError] = useState('');

  const handleCreate = async () => {
    setLoading(true);
    setError('');
    try {
      const request: CreateShareLinkRequest = {
        resourceType,
        allowDownload,
        watermarkEnabled,
        ...(resourceType === 'FILE' ? { fileUuid: resourceUuid } : { folderUuid: resourceUuid }),
        ...(password ? { password } : {}),
        ...(expiresAt ? { expiresAt: new Date(expiresAt).toISOString() } : {}),
      };
      const link = await createShareLink(workspaceId, request);
      setCreatedLink(link);
      onCreated?.(link);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create share link');
    } finally {
      setLoading(false);
    }
  };

  const handleCopyUrl = () => {
    if (createdLink) {
      navigator.clipboard.writeText(createdLink.url);
    }
  };

  if (!open) return null;

  return (
    <div className="share-dialog-overlay" onClick={onClose}>
      <div className="share-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="share-dialog-header">
          <h3>Share: {resourceName}</h3>
          <button onClick={onClose} className="close-btn">&times;</button>
        </div>

        <div className="share-dialog-body">
          {createdLink ? (
            <div className="share-link-created">
              <p>Share link created successfully!</p>
              <div className="link-url">
                <input type="text" readOnly value={createdLink.url} />
                <button onClick={handleCopyUrl}>Copy</button>
              </div>
              <p className="link-info">
                {createdLink.hasPassword && <span>🔒 Password protected</span>}
                {createdLink.expiresAt && <span> • Expires: {new Date(createdLink.expiresAt).toLocaleDateString()}</span>}
              </p>
            </div>
          ) : (
            <div className="share-form">
              {error && <div className="error-message">{error}</div>}

              <div className="form-group">
                <label>Password (optional)</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Leave empty for no password"
                />
              </div>

              <div className="form-group">
                <label>Expiry date (optional)</label>
                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) => setExpiresAt(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={allowDownload}
                    onChange={(e) => setAllowDownload(e.target.checked)}
                  />
                  Allow download
                </label>
              </div>

              <div className="form-group">
                <label>
                  <input
                    type="checkbox"
                    checked={watermarkEnabled}
                    onChange={(e) => setWatermarkEnabled(e.target.checked)}
                  />
                  Enable watermark
                </label>
              </div>

              <button
                onClick={handleCreate}
                disabled={loading}
                className="create-btn"
              >
                {loading ? 'Creating...' : 'Create Share Link'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ShareLinkDialog;
