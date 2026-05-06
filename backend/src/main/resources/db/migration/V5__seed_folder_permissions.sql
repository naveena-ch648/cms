-- V5: Seed folder-related permissions

INSERT INTO permissions (name, description, category) VALUES
    ('manage-folders', 'Create, rename, move, and delete folders', 'FOLDER'),
    ('view-folders', 'View folder contents and navigate folder tree', 'FOLDER');

-- Grant folder permissions to existing system roles
-- Admin gets manage-folders, Editor gets manage-folders, Viewer gets view-folders
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.is_system = TRUE
  AND r.name = 'Admin'
  AND p.name IN ('manage-folders', 'view-folders');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.is_system = TRUE
  AND r.name = 'Editor'
  AND p.name IN ('manage-folders', 'view-folders');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.is_system = TRUE
  AND r.name = 'Viewer'
  AND p.name = 'view-folders';
