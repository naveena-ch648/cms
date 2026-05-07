import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { workspacesApi } from '../api/workspaces';
import type { Workspace } from '../types/models';

export default function WorkspaceListPage() {
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState<Workspace[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formName, setFormName] = useState('');
  const [formDesc, setFormDesc] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);

  const fetchWorkspaces = useCallback(async () => {
    try {
      setLoading(true);
      const res = await workspacesApi.list();
      setWorkspaces(res.data.data ?? []);
    } catch {
      setError('Failed to load workspaces');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchWorkspaces(); }, [fetchWorkspaces]);

  const handleCreate = async () => {
    if (!formName.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await workspacesApi.create({ name: formName.trim(), description: formDesc.trim() || undefined });
      setShowCreate(false);
      setFormName('');
      setFormDesc('');
      fetchWorkspaces();
    } catch {
      setError('Failed to create workspace');
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async (id: string) => {
    if (!formName.trim()) return;
    setSaving(true);
    setError(null);
    try {
      await workspacesApi.update(id, { name: formName.trim(), description: formDesc.trim() || undefined });
      setEditingId(null);
      setFormName('');
      setFormDesc('');
      fetchWorkspaces();
    } catch {
      setError('Failed to update workspace');
    } finally {
      setSaving(false);
    }
  };

  const handleArchive = async (id: string) => {
    try {
      await workspacesApi.archive(id);
      fetchWorkspaces();
    } catch {
      setError('Failed to archive workspace');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await workspacesApi.delete(id);
      setConfirmDelete(null);
      fetchWorkspaces();
    } catch {
      setError('Failed to delete workspace');
    }
  };

  const startEdit = (ws: Workspace) => {
    setEditingId(ws.id);
    setFormName(ws.name);
    setFormDesc(ws.description || '');
    setShowCreate(false);
  };

  const cancelForm = () => {
    setShowCreate(false);
    setEditingId(null);
    setFormName('');
    setFormDesc('');
  };

  const statusBadge = (status: string) => {
    const colors: Record<string, { bg: string; fg: string }> = {
      ACTIVE: { bg: '#dcfce7', fg: '#166534' },
      ARCHIVED: { bg: '#fef3c7', fg: '#92400e' },
      DELETED: { bg: '#fee2e2', fg: '#991b1b' },
    };
    const c = colors[status] || { bg: '#f3f4f6', fg: '#374151' };
    return (
      <span style={{ padding: '2px 10px', borderRadius: 12, fontSize: 12, fontWeight: 600, background: c.bg, color: c.fg }}>
        {status}
      </span>
    );
  };

  return (
    <div style={{ padding: 32, maxWidth: 1000, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ margin: 0, fontSize: 24, fontWeight: 700, color: '#1e293b' }}>Workspaces</h1>
        <button
          onClick={() => { setShowCreate(true); setEditingId(null); setFormName(''); setFormDesc(''); }}
          style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', fontWeight: 600, fontSize: 14 }}
        >
          + New Workspace
        </button>
      </div>

      {error && (
        <div style={{ padding: '10px 16px', background: '#fef2f2', color: '#dc2626', borderRadius: 8, marginBottom: 16, fontSize: 14 }}>
          {error}
          <button onClick={() => setError(null)} style={{ float: 'right', background: 'none', border: 'none', cursor: 'pointer', color: '#dc2626' }}>✕</button>
        </div>
      )}

      {(showCreate || editingId) && (
        <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 10, padding: 20, marginBottom: 20 }}>
          <h3 style={{ margin: '0 0 16px', fontSize: 16, fontWeight: 600 }}>
            {showCreate ? 'Create Workspace' : 'Edit Workspace'}
          </h3>
          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#475569', marginBottom: 4 }}>Name *</label>
            <input
              value={formName}
              onChange={e => setFormName(e.target.value)}
              placeholder="Workspace name"
              style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 14, boxSizing: 'border-box' }}
              autoFocus
            />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 500, color: '#475569', marginBottom: 4 }}>Description</label>
            <textarea
              value={formDesc}
              onChange={e => setFormDesc(e.target.value)}
              placeholder="Optional description"
              rows={2}
              style={{ width: '100%', padding: '8px 12px', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: 14, resize: 'vertical', boxSizing: 'border-box' }}
            />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={() => editingId ? handleUpdate(editingId) : handleCreate()}
              disabled={saving || !formName.trim()}
              style={{ padding: '8px 20px', background: '#2563eb', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontWeight: 600, fontSize: 14, opacity: saving || !formName.trim() ? 0.5 : 1 }}
            >
              {saving ? 'Saving...' : editingId ? 'Update' : 'Create'}
            </button>
            <button
              onClick={cancelForm}
              style={{ padding: '8px 20px', background: '#e2e8f0', color: '#475569', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Loading workspaces...</div>
      ) : workspaces.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 60, color: '#94a3b8' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>▤</div>
          <p style={{ fontSize: 16 }}>No workspaces yet. Create your first workspace to get started.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {workspaces.map(ws => (
            <div
              key={ws.id}
              style={{
                background: '#fff',
                border: '1px solid #e2e8f0',
                borderRadius: 10,
                padding: '16px 20px',
                display: 'flex',
                alignItems: 'center',
                gap: 16,
                transition: 'box-shadow 0.15s',
              }}
              onMouseEnter={e => (e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.08)')}
              onMouseLeave={e => (e.currentTarget.style.boxShadow = 'none')}
            >
              <div
                style={{ flex: 1, cursor: 'pointer' }}
                onClick={() => { localStorage.setItem('lastWorkspaceId', ws.id); navigate(`/workspaces/${ws.id}`); }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
                  <span style={{ fontSize: 16, fontWeight: 600, color: '#1e293b' }}>{ws.name}</span>
                  {statusBadge(ws.status)}
                </div>
                {ws.description && (
                  <p style={{ margin: 0, fontSize: 13, color: '#64748b', lineHeight: '1.4' }}>{ws.description}</p>
                )}
                <div style={{ display: 'flex', gap: 16, marginTop: 8, fontSize: 12, color: '#94a3b8' }}>
                  <span>👥 {ws.memberCount} member{ws.memberCount !== 1 ? 's' : ''}</span>
                  {ws.myRole && <span>Role: {ws.myRole.name}</span>}
                  <span>Created {new Date(ws.createdAt).toLocaleDateString()}</span>
                </div>
              </div>

              <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                <button
                  onClick={() => startEdit(ws)}
                  title="Edit"
                  style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}
                >
                  ✏️
                </button>
                {ws.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleArchive(ws.id)}
                    title="Archive"
                    style={{ padding: '6px 10px', background: '#fef3c7', border: '1px solid #fde68a', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}
                  >
                    📦
                  </button>
                )}
                {confirmDelete === ws.id ? (
                  <div style={{ display: 'flex', gap: 4 }}>
                    <button
                      onClick={() => handleDelete(ws.id)}
                      style={{ padding: '6px 10px', background: '#dc2626', color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 12, fontWeight: 600 }}
                    >
                      Confirm
                    </button>
                    <button
                      onClick={() => setConfirmDelete(null)}
                      style={{ padding: '6px 10px', background: '#f1f5f9', border: '1px solid #e2e8f0', borderRadius: 6, cursor: 'pointer', fontSize: 12 }}
                    >
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setConfirmDelete(ws.id)}
                    title="Delete"
                    style={{ padding: '6px 10px', background: '#fee2e2', border: '1px solid #fecaca', borderRadius: 6, cursor: 'pointer', fontSize: 13 }}
                  >
                    🗑️
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
