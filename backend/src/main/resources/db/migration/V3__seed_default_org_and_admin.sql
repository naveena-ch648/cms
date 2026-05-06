-- V3: Seed default platform organization and super admin user
-- Password: Admin123! (bcrypt encoded with cost factor 12)

INSERT INTO organizations (uuid, name, slug, billing_contact_email, status, policies)
VALUES ('00000000-0000-0000-0000-000000000001', 'Platform', 'platform', 'admin@cms-platform.com', 'ACTIVE', '{}');

-- Create default roles for platform org
INSERT INTO roles (uuid, organization_id, name, description, parent_role_id, is_system)
VALUES ('00000000-0000-0000-0000-000000000010', 1, 'Viewer', 'Can view workspaces, users, roles, and groups', NULL, TRUE);

INSERT INTO roles (uuid, organization_id, name, description, parent_role_id, is_system)
VALUES ('00000000-0000-0000-0000-000000000011', 1, 'Editor', 'Inherits all Viewer permissions', 1, TRUE);

INSERT INTO roles (uuid, organization_id, name, description, parent_role_id, is_system)
VALUES ('00000000-0000-0000-0000-000000000012', 1, 'Admin', 'Full administrative access', 2, TRUE);

-- Assign view permissions to Viewer role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE name LIKE 'view-%';

-- Assign manage permissions + view-audit-log to Admin role
INSERT INTO role_permissions (role_id, permission_id)
SELECT 3, id FROM permissions WHERE name LIKE 'manage-%' OR name = 'view-audit-log';

-- Create super admin user (password: Admin123!)
INSERT INTO users (uuid, organization_id, email, password_hash, first_name, last_name, status)
VALUES ('00000000-0000-0000-0000-000000000002', 1, 'admin@cms-platform.com',
        '$2a$12$wEZyKNPLks29x8If/Gg9Z.QImihdREmMfYwrZiLj8mKdE9iLN09BK',
        'Platform', 'Admin', 'ACTIVE');

-- Assign Admin role to super admin user
INSERT INTO user_organization_roles (user_id, organization_id, role_id)
VALUES (1, 1, 3);
