import React, { useEffect, useState } from 'react';
import { listPermissions, assignPermission, removePermission } from '../api/permissions';
import type { Permission, AssignPermissionRequest } from '../types/permission';

interface PermissionDialogProps {
  workspaceId: string;
  folderId: string;
  folderName: string;
  open: boolean;
  onClose: () => void;
}

const PermissionDialog: React.FC<PermissionDialogProps> = ({
  workspaceId,
  folderId,
  folderName,
  open,
  onClose,
}) => {
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(false);
  const [userUuid, setUserUuid] = useState('');
  const [groupUuid, setGroupUuid] = useState('');
  const [roleUuid, setRoleUuid] = useState('');
  const [isOverride, setIsOverride] = useState(false);
  const [assignType, setAssignType] = useState<'user' | 'group'>('user');

  useEffect(() => {
    if (open) {
      loadPermissions();
    }
  }, [open, folderId]);

  const loadPermissions = async () => {
    setLoading(true);
    try {
      const perms = await listPermissions(workspaceId, folderId);
      setPermissions(perms);
    } catch (error) {
      console.error('Failed to load permissions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAssign = async () => {
    const request: AssignPermissionRequest = {
      roleUuid,
      isOverride,
      ...(assignType === 'user' ? { userUuid } : { groupUuid }),
    };
    try {
      await assignPermission(workspaceId, folderId, request);
      await loadPermissions();
      setUserUuid('');
      setGroupUuid('');
      setRoleUuid('');
      setIsOverride(false);
    } catch (error) {
      console.error('Failed to assign permission:', error);
    }
  };

  const handleRemove = async (permissionId: number) => {
    try {
      await removePermission(workspaceId, folderId, permissionId);
      await loadPermissions();
    } catch (error) {
      console.error('Failed to remove permission:', error);
    }
  };

  if (!open) return null;

  return (
    <div className="permission-dialog-overlay" onClick={onClose}>
      <div className="permission-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="permission-dialog-header">
          <h3>Permissions: {folderName}</h3>
          <button onClick={onClose} className="close-btn">&times;</button>
        </div>

        <div className="permission-dialog-body">
          {/* Assign new permission */}
          <div className="assign-section">
            <h4>Assign Permission</h4>
            <div className="assign-form">
              <div className="form-row">
                <label>
                  <input
                    type="radio"
                    value="user"
                    checked={assignType === 'user'}
                    onChange={() => setAssignType('user')}
                  />
                  User
                </label>
                <label>
                  <input
                    type="radio"
                    value="group"
                    checked={assignType === 'group'}
                    onChange={() => setAssignType('group')}
                  />
                  Group
                </label>
              </div>

              {assignType === 'user' ? (
                <input
                  type="text"
                  placeholder="User UUID"
                  value={userUuid}
                  onChange={(e) => setUserUuid(e.target.value)}
                />
              ) : (
                <input
                  type="text"
                  placeholder="Group UUID"
                  value={groupUuid}
                  onChange={(e) => setGroupUuid(e.target.value)}
                />
              )}

              <input
                type="text"
                placeholder="Role UUID"
                value={roleUuid}
                onChange={(e) => setRoleUuid(e.target.value)}
              />

              <label className="override-label">
                <input
                  type="checkbox"
                  checked={isOverride}
                  onChange={(e) => setIsOverride(e.target.checked)}
                />
                Override inherited permission
              </label>

              <button onClick={handleAssign} disabled={!roleUuid}>
                Assign
              </button>
            </div>
          </div>

          {/* Current permissions list */}
          <div className="permissions-list">
            <h4>Current Permissions</h4>
            {loading ? (
              <p>Loading...</p>
            ) : permissions.length === 0 ? (
              <p>No permissions assigned</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>User/Group</th>
                    <th>Role</th>
                    <th>Source</th>
                    <th>Override</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {permissions.map((perm) => (
                    <tr key={perm.id}>
                      <td>{perm.userName || perm.groupName || '—'}</td>
                      <td>{perm.role}</td>
                      <td>
                        {perm.inherited ? (
                          <span className="badge inherited">Inherited</span>
                        ) : (
                          <span className="badge direct">Direct</span>
                        )}
                      </td>
                      <td>{perm.isOverride ? '✓' : '—'}</td>
                      <td>
                        {!perm.inherited && (
                          <button
                            className="remove-btn"
                            onClick={() => handleRemove(perm.id)}
                          >
                            Remove
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PermissionDialog;
