import { useState, useEffect, useCallback, type CSSProperties } from 'react';
import { usersApi } from '../../api/users';
import { rolesApi } from '../../api/roles';
import { adminApi } from '../../api/admin';
import type { User, Role } from '../../types/models';
import type { PagedMeta } from '../../types/api';

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [pagination, setPagination] = useState<PagedMeta | null>(null);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Dialogs
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showPasswordDialog, setShowPasswordDialog] = useState(false);
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  const [showDeactivateConfirm, setShowDeactivateConfirm] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  // Bulk
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkAction, setBulkAction] = useState('');
  const [bulkRoleId, setBulkRoleId] = useState('');

  // Forms
  const [createForm, setCreateForm] = useState({ firstName: '', lastName: '', email: '', password: '', roleId: '' });
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '' });
  const [newPassword, setNewPassword] = useState('');
  const [newRoleId, setNewRoleId] = useState('');

  const showNotification = useCallback((type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await usersApi.list({ search: search || undefined, status: statusFilter || undefined, page, size: 10 });
      setUsers(res.data.data || []);
      setPagination(res.data.meta?.pagination || null);
    } catch {
      setError('Failed to load users');
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, page]);

  const fetchRoles = useCallback(async () => {
    try {
      const res = await rolesApi.list();
      setRoles(res.data.data || []);
    } catch { /* ignore */ }
  }, []);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);
  useEffect(() => { fetchRoles(); }, [fetchRoles]);

  const handleCreate = async () => {
    try {
      await usersApi.create(createForm);
      setShowCreateDialog(false);
      setCreateForm({ firstName: '', lastName: '', email: '', password: '', roleId: '' });
      showNotification('success', 'User created successfully');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to create user');
    }
  };

  const handleEdit = async () => {
    if (!selectedUser) return;
    try {
      await usersApi.update(selectedUser.id, editForm);
      setShowEditDialog(false);
      showNotification('success', 'User updated successfully');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to update user');
    }
  };

  const handleChangeRole = async () => {
    if (!selectedUser) return;
    try {
      await usersApi.changeRole(selectedUser.id, newRoleId);
      setShowRoleDialog(false);
      showNotification('success', 'Role changed successfully');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to change role');
    }
  };

  const handleResetPassword = async () => {
    if (!selectedUser) return;
    try {
      await usersApi.changePassword(selectedUser.id, newPassword);
      setShowPasswordDialog(false);
      setNewPassword('');
      showNotification('success', 'Password reset successfully');
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to reset password');
    }
  };

  const handleDeactivate = async () => {
    if (!selectedUser) return;
    try {
      await usersApi.update(selectedUser.id, { status: 'INACTIVE' });
      setShowDeactivateConfirm(false);
      showNotification('success', 'User deactivated');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to deactivate user');
    }
  };

  const handleActivate = async (user: User) => {
    try {
      await usersApi.update(user.id, { status: 'ACTIVE' });
      showNotification('success', 'User activated');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Failed to activate user');
    }
  };

  const handleBulkAction = async () => {
    if (selectedIds.size === 0 || !bulkAction) return;
    try {
      const res = await adminApi.bulkUserAction({
        userIds: Array.from(selectedIds),
        action: bulkAction as 'CHANGE_ROLE' | 'ACTIVATE' | 'DEACTIVATE',
        roleId: bulkAction === 'CHANGE_ROLE' ? bulkRoleId : undefined,
      });
      const data = res.data.data!;
      showNotification('success', `Bulk action: ${data.successful} succeeded, ${data.failed} failed`);
      setSelectedIds(new Set());
      setBulkAction('');
      fetchUsers();
    } catch (err: any) {
      showNotification('error', err.response?.data?.error?.message || 'Bulk action failed');
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === users.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(users.map(u => u.id)));
    }
  };

  const statusBadge = (status: string) => {
    const colors: Record<string, { bg: string; color: string }> = {
      ACTIVE: { bg: '#dcfce7', color: '#166534' },
      INACTIVE: { bg: '#fef3c7', color: '#92400e' },
      LOCKED: { bg: '#fee2e2', color: '#991b1b' },
    };
    const c = colors[status] || { bg: '#f1f5f9', color: '#475569' };
    return (
      <span style={{ padding: '2px 8px', borderRadius: '12px', fontSize: '12px', fontWeight: 500, background: c.bg, color: c.color }}>
        {status}
      </span>
    );
  };

  return (
    <div>
      <div style={styles.header}>
        <h2 style={styles.title}>User Management</h2>
        <button onClick={() => { setCreateForm({ firstName: '', lastName: '', email: '', password: '', roleId: roles[0]?.id || '' }); setShowCreateDialog(true); }} style={styles.primaryBtn}>
          + Create User
        </button>
      </div>

      {notification && (
        <div style={{ ...styles.notification, background: notification.type === 'success' ? '#dcfce7' : '#fee2e2', color: notification.type === 'success' ? '#166534' : '#991b1b' }}>
          {notification.message}
        </div>
      )}

      {/* Filters */}
      <div style={styles.filters}>
        <input
          type="text"
          placeholder="Search by name or email..."
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(0); }}
          style={styles.searchInput}
        />
        <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0); }} style={styles.select}>
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="LOCKED">Locked</option>
        </select>
      </div>

      {/* Bulk actions */}
      {selectedIds.size > 0 && (
        <div style={styles.bulkBar}>
          <span style={{ fontSize: '13px', color: '#475569' }}>{selectedIds.size} selected</span>
          <select value={bulkAction} onChange={e => setBulkAction(e.target.value)} style={styles.select}>
            <option value="">Bulk action...</option>
            <option value="CHANGE_ROLE">Change Role</option>
            <option value="ACTIVATE">Activate</option>
            <option value="DEACTIVATE">Deactivate</option>
          </select>
          {bulkAction === 'CHANGE_ROLE' && (
            <select value={bulkRoleId} onChange={e => setBulkRoleId(e.target.value)} style={styles.select}>
              <option value="">Select role...</option>
              {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          )}
          <button onClick={handleBulkAction} style={styles.primaryBtn} disabled={!bulkAction || (bulkAction === 'CHANGE_ROLE' && !bulkRoleId)}>
            Apply
          </button>
        </div>
      )}

      {/* Table */}
      {loading ? (
        <div style={styles.loading}>Loading users...</div>
      ) : error ? (
        <div style={styles.error}>
          {error}
          <button onClick={fetchUsers} style={{ ...styles.primaryBtn, marginLeft: '12px' }}>Retry</button>
        </div>
      ) : users.length === 0 ? (
        <div style={styles.empty}>No users found</div>
      ) : (
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>
                <input type="checkbox" checked={selectedIds.size === users.length && users.length > 0} onChange={toggleSelectAll} />
              </th>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Email</th>
              <th style={styles.th}>Role</th>
              <th style={styles.th}>Status</th>
              <th style={styles.th}>Created</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id} style={styles.tr}>
                <td style={styles.td}>
                  <input type="checkbox" checked={selectedIds.has(user.id)} onChange={() => toggleSelect(user.id)} />
                </td>
                <td style={styles.td}>{user.firstName} {user.lastName}</td>
                <td style={styles.td}>{user.email}</td>
                <td style={styles.td}>{user.organizationRole?.name || '—'}</td>
                <td style={styles.td}>{statusBadge(user.status)}</td>
                <td style={styles.td}>{new Date(user.createdAt).toLocaleDateString()}</td>
                <td style={styles.td}>
                  <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                    <button onClick={() => { setSelectedUser(user); setEditForm({ firstName: user.firstName, lastName: user.lastName }); setShowEditDialog(true); }} style={styles.actionBtn}>Edit</button>
                    <button onClick={() => { setSelectedUser(user); setNewRoleId(user.organizationRole?.id || ''); setShowRoleDialog(true); }} style={styles.actionBtn}>Role</button>
                    <button onClick={() => { setSelectedUser(user); setNewPassword(''); setShowPasswordDialog(true); }} style={styles.actionBtn}>Password</button>
                    {user.status === 'ACTIVE' ? (
                      <button onClick={() => { setSelectedUser(user); setShowDeactivateConfirm(true); }} style={{ ...styles.actionBtn, color: '#dc2626' }}>Deactivate</button>
                    ) : (
                      <button onClick={() => handleActivate(user)} style={{ ...styles.actionBtn, color: '#16a34a' }}>Activate</button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Pagination */}
      {pagination && pagination.totalPages > 1 && (
        <div style={styles.pagination}>
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} style={styles.pageBtn}>← Prev</button>
          <span style={{ fontSize: '13px', color: '#475569' }}>Page {page + 1} of {pagination.totalPages} ({pagination.totalElements} total)</span>
          <button onClick={() => setPage(p => p + 1)} disabled={page >= pagination.totalPages - 1} style={styles.pageBtn}>Next →</button>
        </div>
      )}

      {/* Create User Dialog */}
      {showCreateDialog && (
        <Dialog title="Create User" onClose={() => setShowCreateDialog(false)}>
          <div style={styles.formGroup}>
            <label style={styles.label}>First Name</label>
            <input style={styles.input} value={createForm.firstName} onChange={e => setCreateForm(f => ({ ...f, firstName: e.target.value }))} />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>Last Name</label>
            <input style={styles.input} value={createForm.lastName} onChange={e => setCreateForm(f => ({ ...f, lastName: e.target.value }))} />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>Email</label>
            <input style={styles.input} type="email" value={createForm.email} onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>Password</label>
            <input style={styles.input} type="password" value={createForm.password} onChange={e => setCreateForm(f => ({ ...f, password: e.target.value }))} />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>Role</label>
            <select style={styles.input} value={createForm.roleId} onChange={e => setCreateForm(f => ({ ...f, roleId: e.target.value }))}>
              <option value="">Select role...</option>
              {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          </div>
          <div style={styles.dialogActions}>
            <button onClick={() => setShowCreateDialog(false)} style={styles.cancelBtn}>Cancel</button>
            <button onClick={handleCreate} style={styles.primaryBtn} disabled={!createForm.email || !createForm.password || !createForm.roleId}>Create</button>
          </div>
        </Dialog>
      )}

      {/* Edit User Dialog */}
      {showEditDialog && selectedUser && (
        <Dialog title={`Edit ${selectedUser.firstName} ${selectedUser.lastName}`} onClose={() => setShowEditDialog(false)}>
          <div style={styles.formGroup}>
            <label style={styles.label}>First Name</label>
            <input style={styles.input} value={editForm.firstName} onChange={e => setEditForm(f => ({ ...f, firstName: e.target.value }))} />
          </div>
          <div style={styles.formGroup}>
            <label style={styles.label}>Last Name</label>
            <input style={styles.input} value={editForm.lastName} onChange={e => setEditForm(f => ({ ...f, lastName: e.target.value }))} />
          </div>
          <div style={styles.dialogActions}>
            <button onClick={() => setShowEditDialog(false)} style={styles.cancelBtn}>Cancel</button>
            <button onClick={handleEdit} style={styles.primaryBtn}>Save</button>
          </div>
        </Dialog>
      )}

      {/* Change Role Dialog */}
      {showRoleDialog && selectedUser && (
        <Dialog title={`Change Role — ${selectedUser.firstName} ${selectedUser.lastName}`} onClose={() => setShowRoleDialog(false)}>
          <div style={styles.formGroup}>
            <label style={styles.label}>New Role</label>
            <select style={styles.input} value={newRoleId} onChange={e => setNewRoleId(e.target.value)}>
              <option value="">Select role...</option>
              {roles.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          </div>
          <div style={styles.dialogActions}>
            <button onClick={() => setShowRoleDialog(false)} style={styles.cancelBtn}>Cancel</button>
            <button onClick={handleChangeRole} style={styles.primaryBtn} disabled={!newRoleId}>Change Role</button>
          </div>
        </Dialog>
      )}

      {/* Password Reset Dialog */}
      {showPasswordDialog && selectedUser && (
        <Dialog title={`Reset Password — ${selectedUser.firstName} ${selectedUser.lastName}`} onClose={() => setShowPasswordDialog(false)}>
          <div style={styles.formGroup}>
            <label style={styles.label}>New Password</label>
            <input style={styles.input} type="password" value={newPassword} onChange={e => setNewPassword(e.target.value)} />
          </div>
          <div style={styles.dialogActions}>
            <button onClick={() => setShowPasswordDialog(false)} style={styles.cancelBtn}>Cancel</button>
            <button onClick={handleResetPassword} style={styles.primaryBtn} disabled={!newPassword}>Reset Password</button>
          </div>
        </Dialog>
      )}

      {/* Deactivate Confirm Dialog */}
      {showDeactivateConfirm && selectedUser && (
        <Dialog title="Confirm Deactivation" onClose={() => setShowDeactivateConfirm(false)}>
          <p style={{ fontSize: '14px', color: '#475569', margin: '0 0 16px' }}>
            Are you sure you want to deactivate <strong>{selectedUser.firstName} {selectedUser.lastName}</strong>? They will lose access to the system.
          </p>
          <div style={styles.dialogActions}>
            <button onClick={() => setShowDeactivateConfirm(false)} style={styles.cancelBtn}>Cancel</button>
            <button onClick={handleDeactivate} style={{ ...styles.primaryBtn, background: '#dc2626' }}>Deactivate</button>
          </div>
        </Dialog>
      )}
    </div>
  );
}

function Dialog({ title, onClose, children }: { title: string; onClose: () => void; children: React.ReactNode }) {
  return (
    <div style={styles.overlay} onClick={onClose}>
      <div style={styles.dialog} onClick={e => e.stopPropagation()}>
        <div style={styles.dialogHeader}>
          <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>{title}</h3>
          <button onClick={onClose} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: '#94a3b8' }}>✕</button>
        </div>
        <div style={styles.dialogBody}>{children}</div>
      </div>
    </div>
  );
}

const styles: Record<string, CSSProperties> = {
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' },
  title: { margin: 0, fontSize: '20px', fontWeight: 700, color: '#1e293b' },
  primaryBtn: { padding: '8px 16px', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: 500 },
  cancelBtn: { padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #e2e8f0', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' },
  filters: { display: 'flex', gap: '12px', marginBottom: '16px' },
  searchInput: { flex: 1, padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', outline: 'none' },
  select: { padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', background: '#fff' },
  bulkBar: { display: 'flex', alignItems: 'center', gap: '12px', padding: '10px 16px', background: '#eff6ff', borderRadius: '8px', marginBottom: '16px' },
  table: { width: '100%', borderCollapse: 'collapse' as const, background: '#fff', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  th: { textAlign: 'left' as const, padding: '10px 12px', fontSize: '12px', fontWeight: 600, color: '#64748b', borderBottom: '1px solid #e2e8f0', background: '#f8fafc', textTransform: 'uppercase' as const, letterSpacing: '0.05em' },
  td: { padding: '10px 12px', fontSize: '13px', color: '#334155', borderBottom: '1px solid #f1f5f9' },
  tr: { transition: 'background 0.1s' },
  actionBtn: { padding: '4px 8px', border: '1px solid #e2e8f0', borderRadius: '4px', background: '#fff', cursor: 'pointer', fontSize: '12px', color: '#475569' },
  pagination: { display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '16px', marginTop: '16px' },
  pageBtn: { padding: '6px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', background: '#fff', cursor: 'pointer', fontSize: '13px' },
  notification: { padding: '10px 16px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px', fontWeight: 500 },
  loading: { textAlign: 'center' as const, padding: '40px', color: '#64748b', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '40px', color: '#dc2626', fontSize: '14px' },
  empty: { textAlign: 'center' as const, padding: '40px', color: '#94a3b8', fontSize: '14px' },
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  dialog: { background: '#fff', borderRadius: '12px', width: '440px', maxHeight: '80vh', overflow: 'auto', boxShadow: '0 20px 60px rgba(0,0,0,0.15)' },
  dialogHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 20px', borderBottom: '1px solid #e2e8f0' },
  dialogBody: { padding: '20px' },
  dialogActions: { display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '20px' },
  formGroup: { marginBottom: '14px' },
  label: { display: 'block', fontSize: '13px', fontWeight: 500, color: '#374151', marginBottom: '4px' },
  input: { width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' as const },
};
