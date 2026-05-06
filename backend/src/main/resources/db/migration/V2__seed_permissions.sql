-- V2: Seed system permissions
INSERT INTO permissions (name, description, category) VALUES
    ('view-workspace', 'View workspace and its contents', 'workspace'),
    ('manage-workspace', 'Create, update, delete workspaces', 'workspace'),
    ('view-users', 'View user list', 'user'),
    ('manage-users', 'Create, update, deactivate users', 'user'),
    ('view-roles', 'View role definitions', 'role'),
    ('manage-roles', 'Create, update, delete roles', 'role'),
    ('view-groups', 'View groups', 'group'),
    ('manage-groups', 'Create, update, delete groups', 'group'),
    ('manage-policies', 'Configure organization policies', 'organization'),
    ('view-audit-log', 'View audit events', 'audit');
