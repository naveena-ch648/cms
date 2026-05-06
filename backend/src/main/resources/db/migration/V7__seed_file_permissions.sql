-- V7: Seed file-related permissions
INSERT INTO permissions (name, description, category) VALUES
('FILE_UPLOAD', 'Upload files to folders', 'FILE'),
('FILE_DOWNLOAD', 'Download and preview files', 'FILE'),
('FILE_MANAGE', 'Rename, move, copy, delete files', 'FILE'),
('FILE_TRASH_RESTORE', 'Restore files from trash', 'FILE'),
('FILE_TRASH_DELETE', 'Permanently delete files from trash', 'FILE');

-- Assign to Viewer role: FILE_DOWNLOAD only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Viewer' AND p.name = 'FILE_DOWNLOAD';

-- Assign to Editor role: FILE_UPLOAD, FILE_DOWNLOAD, FILE_MANAGE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Editor' AND p.name IN ('FILE_UPLOAD', 'FILE_DOWNLOAD', 'FILE_MANAGE');

-- Assign to Admin role: all file permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'Admin' AND p.name IN ('FILE_UPLOAD', 'FILE_DOWNLOAD', 'FILE_MANAGE', 'FILE_TRASH_RESTORE', 'FILE_TRASH_DELETE');
