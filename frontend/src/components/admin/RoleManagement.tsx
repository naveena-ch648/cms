import { useState, useEffect, useCallback, type CSSProperties } from 'react';
import { rolesApi } from '../../api/roles';
import type { Role, Permission } from '../../types/models';

type ViewMode = 'list' | 'matrix';

export default function RoleManagement() {
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Dialogs
  const [showFormDialog, setShowFormDialog] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '', permissionIds: [] as number[] });

  const showNotif = useCallback((type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const fetchRoles = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await rolesApi.list();
      setRoles(res.data.data || []);
    } catch {
      setError('Failed to load roles');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchPermissions = useCallback(async () => {
    try {
      const res = await rolesApi.listPermissions();
      setPermissions(res.data.data || []);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchRoles(); }, [fetchRoles]);
  useEffect(() => { fetchPermissions(); }, [fetchPermissions]);

  const openCreateDialog = () => {
    setEditingRole(null);
    setFormData({ name: '', description: '', permissionIds: [] });
    setShowFormDialog(true);
  };

  const openEditDialog = (role: Role) => {
    setEditingRole(role);
    const permIds = permissions
      .filter(p => role.directPermissions?.includes(p.name))
      .map(p => Number(p.id));
    setFormData({ name: role.name, description: role.description || '', permissionIds: permIds });
    setShowFormDialog(true);
  };

  const handleSave = async () => {
    try {
      if (editingRole) {
        await rolesApi.update(editingRole.id, { name: formData.name, description: formData.description, permissionIds: formData.permissionIds });
        showNotif('success', 'Role updated successfully');
      } else {
        await rolesApi.create({ name: formData.name, description: formData.description, permissionIds: formData.permissionIds });
        showNotif('success', 'Role created successfully');
      }
      setShowFormDialog(false);
      fetchRoles();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to save role');
    }
  };

  const handleDelete = async () => {
    if (!editingRole) return;
    try {
      await rolesApi.delete(editingRole.id);
      setShowDeleteConfirm(false);
      setEditingRole(null);
      showNotif('success', 'Role deleted successfully');
      fetchRoles();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to delete role');
    }
  };

  const togglePermission = (permId: number) => {
    setFormData(f => ({
      ...f,
      permissionIds: f.permissionIds.includes(permId)
        ? f.permissionIds.filter(id => id !== permId)
        : [...f.permissionIds, permId],
    }));
  };

  // Group permissions by category
  const permissionsByCategory = permissions.reduce<Record<string, Permission[]>>((acc, p) => {
    const cat = p.category || 'General';
    (acc[cat] = acc[cat] || []).push(p);
    return acc;
  }, {});

  return (
    <div>
      <div style={styles.header}>
        <h2 style={styles.title}>Role & Permission Management</h2>
        <div style={{ display: 'flex', gap: '8px' }}>
          <div style={styles.viewToggle}>
            <button onClick={() => setViewMode('list')} style={{ ...styles.toggleBtn, ...(viewMode === 'list' ? styles.toggleActive : {}) }}>List</button>
            <button onClick={() => setViewMode('matrix')} style={{ ...styles.toggleBtn, ...(viewMode === 'matrix' ? styles.toggleActive : {}) }}>Matrix</button>
          </div>
          <button onClick={openCreateDialog} style={styles.primaryBtn}>+ Create Role</button>
        </div>
      </div>

      {notification && (
        <div style={{ ...styles.notification, background: notification.type === 'success' ? '#dcfce7' : '#fee2e2', color: notification.type === 'success' ? '#166534' : '#991b1b' }}>
          {notification.message}
        </div>
      )}

      {loading ? (
        <div style={styles.loading}>Loading roles...</div>
      ) : error ? (
        <div style={styles.error}>{error}<button onClick={fetchRoles} style={{ ...styles.primaryBtn, marginLeft: '12px' }}>Retry</button></div>
      ) : viewMode === 'list' ? (
        /* List View */
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Description</th>
              <th style={styles.th}>Type</th>
              <th style={styles.th}>Permissions</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {roles.map(role => (
              <tr key={role.id} style={styles.tr}>
                <td style={styles.td}>
                  <span style={{ fontWeight: 500 }}>{role.name}</span>
                </td>
                <td style={styles.td}>{role.description || '—'}</td>
                <td style={styles.td}>
                  <span style={{
                    padding: '2px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 500,
                    background: role.isSystem ? '#dbeafe' : '#f0fdf4',
                    color: role.isSystem ? '#1e40af' : '#166534',
                  }}>
                    {role.isSystem ? 'System' : 'Custom'}
                  </span>
                </td>
                <td style={styles.td}>{role.effectivePermissions?.length || 0}</td>
                <td style={styles.td}>
                  <div style={{ display: 'flex', gap: '4px' }}>
                    {!role.isSystem && (
                      <>
                        <button onClick={() => openEditDialog(role)} style={styles.actionBtn}>Edit</button>
                        <button onClick={() => { setEditingRole(role); setShowDeleteConfirm(true); }} style={{ ...styles.actionBtn, color: '#dc2626' }}>Delete</button>
                      </>
                    )}
                    {role.isSystem && <span style={{ fontSize: '12px', color: '#94a3b8' }}>Read-only</span>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        /* Matrix View */
        <div style={{ overflowX: 'auto' }}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={{ ...styles.th, position: 'sticky' as const, left: 0, background: '#f8fafc', zIndex: 1 }}>Permission</th>
                {roles.map(r => <th key={r.id} style={{ ...styles.th, textAlign: 'center' as const, minWidth: '80px' }}>{r.name}</th>)}
              </tr>
            </thead>
            <tbody>
              {permissions.map(perm => (
                <tr key={perm.id}>
                  <td style={{ ...styles.td, position: 'sticky' as const, left: 0, background: '#fff', zIndex: 1, fontWeight: 500, fontSize: '12px' }}>
                    {perm.name}
                  </td>
                  {roles.map(role => (
                    <td key={role.id} style={{ ...styles.td, textAlign: 'center' as const }}>
                      {role.effectivePermissions?.includes(perm.name) ? (
                        <span style={{ color: '#16a34a', fontSize: '16px' }}>✓</span>
                      ) : (
                        <span style={{ color: '#e2e8f0' }}>—</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create/Edit Role Dialog */}
      {showFormDialog && (
        <div style={styles.overlay} onClick={() => setShowFormDialog(false)}>
          <div style={styles.dialog} onClick={e => e.stopPropagation()}>
            <div style={styles.dialogHeader}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>{editingRole ? 'Edit Role' : 'Create Role'}</h3>
              <button onClick={() => setShowFormDialog(false)} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: '#94a3b8' }}>✕</button>
            </div>
            <div style={styles.dialogBody}>
              <div style={styles.formGroup}>
                <label style={styles.label}>Name</label>
                <input style={styles.input} value={formData.name} onChange={e => setFormData(f => ({ ...f, name: e.target.value }))} />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Description</label>
                <input style={styles.input} value={formData.description} onChange={e => setFormData(f => ({ ...f, description: e.target.value }))} />
              </div>
              <div style={styles.formGroup}>
                <label style={styles.label}>Permissions</label>
                {Object.entries(permissionsByCategory).map(([cat, perms]) => (
                  <div key={cat} style={{ marginBottom: '12px' }}>
                    <div style={{ fontSize: '12px', fontWeight: 600, color: '#64748b', marginBottom: '4px', textTransform: 'uppercase' as const }}>{cat}</div>
                    {perms.map(p => (
                      <label key={p.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 0', fontSize: '13px', cursor: 'pointer' }}>
                        <input type="checkbox" checked={formData.permissionIds.includes(Number(p.id))} onChange={() => togglePermission(Number(p.id))} />
                        {p.name}
                        {p.description && <span style={{ color: '#94a3b8', fontSize: '11px' }}>— {p.description}</span>}
                      </label>
                    ))}
                  </div>
                ))}
              </div>
              <div style={styles.dialogActions}>
                <button onClick={() => setShowFormDialog(false)} style={styles.cancelBtn}>Cancel</button>
                <button onClick={handleSave} style={styles.primaryBtn} disabled={!formData.name}>{editingRole ? 'Save' : 'Create'}</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm Dialog */}
      {showDeleteConfirm && editingRole && (
        <div style={styles.overlay} onClick={() => setShowDeleteConfirm(false)}>
          <div style={styles.dialog} onClick={e => e.stopPropagation()}>
            <div style={styles.dialogHeader}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>Delete Role</h3>
              <button onClick={() => setShowDeleteConfirm(false)} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: '#94a3b8' }}>✕</button>
            </div>
            <div style={styles.dialogBody}>
              <p style={{ fontSize: '14px', color: '#475569', margin: '0 0 16px' }}>
                Are you sure you want to delete <strong>{editingRole.name}</strong>? This cannot be undone.
              </p>
              <div style={styles.dialogActions}>
                <button onClick={() => setShowDeleteConfirm(false)} style={styles.cancelBtn}>Cancel</button>
                <button onClick={handleDelete} style={{ ...styles.primaryBtn, background: '#dc2626' }}>Delete</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' },
  title: { margin: 0, fontSize: '20px', fontWeight: 700, color: '#1e293b' },
  primaryBtn: { padding: '8px 16px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: 500 },
  cancelBtn: { padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #e2e8f0', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' },
  viewToggle: { display: 'flex', border: '1px solid #e2e8f0', borderRadius: '6px', overflow: 'hidden' },
  toggleBtn: { padding: '8px 14px', border: 'none', background: '#fff', cursor: 'pointer', fontSize: '13px', color: '#475569' },
  toggleActive: { background: '#3b82f6', color: '#fff' },
  table: { width: '100%', borderCollapse: 'collapse' as const, background: '#fff', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  th: { textAlign: 'left' as const, padding: '10px 12px', fontSize: '12px', fontWeight: 600, color: '#64748b', borderBottom: '1px solid #e2e8f0', background: '#f8fafc', textTransform: 'uppercase' as const, letterSpacing: '0.05em' },
  td: { padding: '10px 12px', fontSize: '13px', color: '#334155', borderBottom: '1px solid #f1f5f9' },
  tr: { transition: 'background 0.1s' },
  actionBtn: { padding: '4px 8px', border: '1px solid #e2e8f0', borderRadius: '4px', background: '#fff', cursor: 'pointer', fontSize: '12px', color: '#475569' },
  notification: { padding: '10px 16px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: 500 },
  loading: { textAlign: 'center' as const, padding: '40px', color: '#64748b', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '40px', color: '#dc2626', fontSize: '14px' },
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  dialog: { background: '#fff', borderRadius: '12px', width: '500px', maxHeight: '80vh', overflow: 'auto', boxShadow: '0 20px 60px rgba(0,0,0,0.15)' },
  dialogHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px', borderBottom: '1px solid #e2e8f0' },
  dialogBody: { padding: '20px' },
  dialogActions: { display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '20px' },
  formGroup: { marginBottom: '14px' },
  label: { display: 'block', fontSize: '13px', fontWeight: 500, color: '#374151', marginBottom: '4px' },
  input: { width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' as const },
};
