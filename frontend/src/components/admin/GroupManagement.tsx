import { useState, useEffect, useCallback, type CSSProperties } from 'react';
import { groupsApi } from '../../api/groups';
import { usersApi } from '../../api/users';
import type { Group, User } from '../../types/models';

export default function GroupManagement() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Dialogs
  const [showFormDialog, setShowFormDialog] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showMembersPanel, setShowMembersPanel] = useState(false);
  const [editingGroup, setEditingGroup] = useState<Group | null>(null);
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);
  const [formData, setFormData] = useState({ name: '', description: '' });

  // Members
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [currentMembers, setCurrentMembers] = useState<User[]>([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [memberSearch, setMemberSearch] = useState('');

  const showNotif = useCallback((type: 'success' | 'error', message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 4000);
  }, []);

  const fetchGroups = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await groupsApi.list();
      setGroups(res.data.data || []);
    } catch {
      setError('Failed to load groups');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchGroups(); }, [fetchGroups]);

  const openCreateDialog = () => {
    setEditingGroup(null);
    setFormData({ name: '', description: '' });
    setShowFormDialog(true);
  };

  const openEditDialog = (group: Group) => {
    setEditingGroup(group);
    setFormData({ name: group.name, description: group.description || '' });
    setShowFormDialog(true);
  };

  const openMembersPanel = async (group: Group) => {
    setSelectedGroup(group);
    setCurrentMembers([]);
    setMemberSearch('');
    setShowMembersPanel(true);
    setMembersLoading(true);
    try {
      const [membersRes, usersRes] = await Promise.all([
        groupsApi.getMembers(group.id),
        usersApi.list({ size: 200 }),
      ]);
      setCurrentMembers(membersRes.data.data || []);
      setAllUsers(usersRes.data.data || []);
    } catch {
      showNotif('error', 'Failed to load members');
    } finally {
      setMembersLoading(false);
    }
  };

  const refreshMembers = async (groupId: string) => {
    try {
      const res = await groupsApi.getMembers(groupId);
      setCurrentMembers(res.data.data || []);
    } catch { /* ignore */ }
  };

  const handleSave = async () => {
    try {
      if (editingGroup) {
        await groupsApi.update(editingGroup.id, formData);
        showNotif('success', 'Group updated successfully');
      } else {
        await groupsApi.create(formData);
        showNotif('success', 'Group created successfully');
      }
      setShowFormDialog(false);
      fetchGroups();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to save group');
    }
  };

  const handleDelete = async () => {
    if (!editingGroup) return;
    try {
      await groupsApi.delete(editingGroup.id);
      setShowDeleteConfirm(false);
      setEditingGroup(null);
      showNotif('success', 'Group deleted successfully');
      fetchGroups();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to delete group');
    }
  };

  const handleAddMember = async (user: User) => {
    if (!selectedGroup) return;
    try {
      await groupsApi.addMember(selectedGroup.id, user.id);
      showNotif('success', `Added ${user.firstName} ${user.lastName} to group`);
      setMemberSearch('');
      await refreshMembers(selectedGroup.id);
      fetchGroups();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to add member');
    }
  };

  const handleRemoveMember = async (user: User) => {
    if (!selectedGroup) return;
    try {
      await groupsApi.removeMember(selectedGroup.id, user.id);
      showNotif('success', `Removed ${user.firstName} ${user.lastName} from group`);
      await refreshMembers(selectedGroup.id);
      fetchGroups();
    } catch (err: any) {
      showNotif('error', err.response?.data?.error?.message || 'Failed to remove member');
    }
  };

  const currentMemberIds = new Set(currentMembers.map(u => u.id));

  const filteredGroups = groups.filter(g =>
    !searchTerm || g.name.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredUsers = allUsers.filter(u =>
    !currentMemberIds.has(u.id) &&
    (!memberSearch || `${u.firstName} ${u.lastName} ${u.email}`.toLowerCase().includes(memberSearch.toLowerCase()))
  );

  return (
    <div>
      <div style={styles.header}>
        <h2 style={styles.title}>Group Management</h2>
        <button onClick={openCreateDialog} style={styles.primaryBtn}>+ Create Group</button>
      </div>

      {notification && (
        <div style={{ ...styles.notification, background: notification.type === 'success' ? '#dcfce7' : '#fee2e2', color: notification.type === 'success' ? '#166534' : '#991b1b' }}>
          {notification.message}
        </div>
      )}

      <div style={{ marginBottom: '16px' }}>
        <input
          type="text"
          placeholder="Search groups..."
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
          style={styles.searchInput}
        />
      </div>

      {loading ? (
        <div style={styles.loading}>Loading groups...</div>
      ) : error ? (
        <div style={styles.error}>{error}<button onClick={fetchGroups} style={{ ...styles.primaryBtn, marginLeft: '12px' }}>Retry</button></div>
      ) : filteredGroups.length === 0 ? (
        <div style={styles.empty}>No groups found</div>
      ) : (
        <table style={styles.table}>
          <thead>
            <tr>
              <th style={styles.th}>Name</th>
              <th style={styles.th}>Description</th>
              <th style={styles.th}>Members</th>
              <th style={styles.th}>Created</th>
              <th style={styles.th}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredGroups.map(group => (
              <tr key={group.id} style={styles.tr}>
                <td style={styles.td}><span style={{ fontWeight: 500 }}>{group.name}</span></td>
                <td style={styles.td}>{group.description || '—'}</td>
                <td style={styles.td}>
                  <span style={{ padding: '2px 8px', borderRadius: '12px', fontSize: '12px', background: '#f1f5f9', color: '#475569' }}>
                    {group.memberCount}
                  </span>
                </td>
                <td style={styles.td}>{new Date(group.createdAt).toLocaleDateString()}</td>
                <td style={styles.td}>
                  <div style={{ display: 'flex', gap: '4px' }}>
                    <button onClick={() => openEditDialog(group)} style={styles.actionBtn}>Edit</button>
                    <button onClick={() => openMembersPanel(group)} style={styles.actionBtn}>Members</button>
                    <button onClick={() => { setEditingGroup(group); setShowDeleteConfirm(true); }} style={{ ...styles.actionBtn, color: '#dc2626' }}>Delete</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Create/Edit Group Dialog */}
      {showFormDialog && (
        <div style={styles.overlay} onClick={() => setShowFormDialog(false)}>
          <div style={styles.dialog} onClick={e => e.stopPropagation()}>
            <div style={styles.dialogHeader}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>{editingGroup ? 'Edit Group' : 'Create Group'}</h3>
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
              <div style={styles.dialogActions}>
                <button onClick={() => setShowFormDialog(false)} style={styles.cancelBtn}>Cancel</button>
                <button onClick={handleSave} style={styles.primaryBtn} disabled={!formData.name}>{editingGroup ? 'Save' : 'Create'}</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirm Dialog */}
      {showDeleteConfirm && editingGroup && (
        <div style={styles.overlay} onClick={() => setShowDeleteConfirm(false)}>
          <div style={styles.dialog} onClick={e => e.stopPropagation()}>
            <div style={styles.dialogHeader}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>Delete Group</h3>
              <button onClick={() => setShowDeleteConfirm(false)} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: '#94a3b8' }}>✕</button>
            </div>
            <div style={styles.dialogBody}>
              <p style={{ fontSize: '14px', color: '#475569', margin: '0 0 16px' }}>
                Are you sure you want to delete <strong>{editingGroup.name}</strong>?
              </p>
              <div style={styles.dialogActions}>
                <button onClick={() => setShowDeleteConfirm(false)} style={styles.cancelBtn}>Cancel</button>
                <button onClick={handleDelete} style={{ ...styles.primaryBtn, background: '#dc2626' }}>Delete</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Members Panel */}
      {showMembersPanel && selectedGroup && (
        <div style={styles.overlay} onClick={() => setShowMembersPanel(false)}>
          <div style={{ ...styles.dialog, width: '540px' }} onClick={e => e.stopPropagation()}>
            <div style={styles.dialogHeader}>
              <h3 style={{ margin: 0, fontSize: '16px', color: '#1e293b' }}>Members — {selectedGroup.name}</h3>
              <button onClick={() => setShowMembersPanel(false)} style={{ border: 'none', background: 'none', cursor: 'pointer', fontSize: '18px', color: '#94a3b8' }}>✕</button>
            </div>
            <div style={styles.dialogBody}>
              {/* Current members list */}
              <div style={{ marginBottom: '16px' }}>
                <label style={styles.label}>Current Members ({currentMembers.length})</label>
                {membersLoading ? (
                  <div style={{ fontSize: '13px', color: '#94a3b8', padding: '8px 0' }}>Loading...</div>
                ) : currentMembers.length === 0 ? (
                  <div style={{ fontSize: '13px', color: '#94a3b8', padding: '8px 0' }}>No members yet</div>
                ) : (
                  <div style={{ border: '1px solid #e2e8f0', borderRadius: '6px', maxHeight: '180px', overflow: 'auto' }}>
                    {currentMembers.map(user => (
                      <div key={user.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 12px', borderBottom: '1px solid #f1f5f9' }}>
                        <div>
                          <span style={{ fontSize: '13px', fontWeight: 500, color: '#1e293b' }}>{user.firstName} {user.lastName}</span>
                          <span style={{ fontSize: '12px', color: '#94a3b8', marginLeft: '8px' }}>{user.email}</span>
                        </div>
                        <button
                          onClick={() => handleRemoveMember(user)}
                          style={{ ...styles.actionBtn, color: '#dc2626', borderColor: '#fecaca' }}
                        >Remove</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Add member search */}
              <div style={styles.formGroup}>
                <label style={styles.label}>Add Member</label>
                <input
                  style={styles.input}
                  placeholder="Search users to add..."
                  value={memberSearch}
                  onChange={e => setMemberSearch(e.target.value)}
                />
              </div>
              {memberSearch && (
                filteredUsers.length === 0 ? (
                  <div style={{ fontSize: '13px', color: '#94a3b8', padding: '8px 0' }}>No users found (or all matching users are already members)</div>
                ) : (
                  <div style={{ maxHeight: '180px', overflow: 'auto', border: '1px solid #e2e8f0', borderRadius: '6px', marginBottom: '8px' }}>
                    {filteredUsers.slice(0, 10).map(user => (
                      <div key={user.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 12px', borderBottom: '1px solid #f1f5f9' }}>
                        <div>
                          <span style={{ fontSize: '13px', fontWeight: 500, color: '#1e293b' }}>{user.firstName} {user.lastName}</span>
                          <span style={{ fontSize: '12px', color: '#94a3b8', marginLeft: '8px' }}>{user.email}</span>
                        </div>
                        <button onClick={() => handleAddMember(user)} style={{ ...styles.actionBtn, color: '#2563eb', borderColor: '#bfdbfe' }}>Add</button>
                      </div>
                    ))}
                  </div>
                )
              )}
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
  searchInput: { width: '100%', padding: '8px 12px', border: '1px solid #e2e8f0', borderRadius: '6px', fontSize: '13px', outline: 'none', boxSizing: 'border-box' as const },
  table: { width: '100%', borderCollapse: 'collapse' as const, background: '#fff', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.06)' },
  th: { textAlign: 'left' as const, padding: '10px 12px', fontSize: '12px', fontWeight: 600, color: '#64748b', borderBottom: '1px solid #e2e8f0', background: '#f8fafc', textTransform: 'uppercase' as const, letterSpacing: '0.05em' },
  td: { padding: '10px 12px', fontSize: '13px', color: '#334155', borderBottom: '1px solid #f1f5f9' },
  tr: { transition: 'background 0.1s' },
  actionBtn: { padding: '4px 8px', border: '1px solid #e2e8f0', borderRadius: '4px', background: '#fff', cursor: 'pointer', fontSize: '12px', color: '#475569' },
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
