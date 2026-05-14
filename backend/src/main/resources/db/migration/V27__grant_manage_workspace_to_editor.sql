-- V24: Grant manage-workspace permission to the Editor role
-- Editors should be able to create, update, and archive workspaces.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.is_system = TRUE
  AND r.name = 'Editor'
  AND p.name = 'manage-workspace'
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
