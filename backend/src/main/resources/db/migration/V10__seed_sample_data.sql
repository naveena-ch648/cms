-- V10: Seed sample data for development/testing
-- Creates: additional users, a workspace, groups, folders, and sample files

-- Additional users (password: User123!)
INSERT INTO users (uuid, organization_id, email, password_hash, first_name, last_name, status)
VALUES ('00000000-0000-0000-0000-000000000003', 1, 'editor@cms-platform.com',
        '$2a$12$O/WPxaOwTNXH5PF/eqo/8eIWftdK06CZgLbv5W/ysxvuG5BTK0tw6',
        'Jane', 'Editor', 'ACTIVE');

INSERT INTO users (uuid, organization_id, email, password_hash, first_name, last_name, status)
VALUES ('00000000-0000-0000-0000-000000000004', 1, 'viewer@cms-platform.com',
        '$2a$12$O/WPxaOwTNXH5PF/eqo/8eIWftdK06CZgLbv5W/ysxvuG5BTK0tw6',
        'Bob', 'Viewer', 'ACTIVE');

INSERT INTO users (uuid, organization_id, email, password_hash, first_name, last_name, status)
VALUES ('00000000-0000-0000-0000-000000000005', 1, 'manager@cms-platform.com',
        '$2a$12$O/WPxaOwTNXH5PF/eqo/8eIWftdK06CZgLbv5W/ysxvuG5BTK0tw6',
        'Alice', 'Manager', 'ACTIVE');

-- Assign roles to users
INSERT INTO user_organization_roles (user_id, organization_id, role_id)
VALUES (2, 1, 2),  -- editor@cms-platform.com → Editor role
       (3, 1, 1),  -- viewer@cms-platform.com → Viewer role
       (4, 1, 3);  -- manager@cms-platform.com → Admin role

-- Create groups
INSERT INTO groups_table (uuid, organization_id, name, description)
VALUES ('00000000-0000-0000-0000-000000000020', 1, 'Engineering', 'Engineering team members');

INSERT INTO groups_table (uuid, organization_id, name, description)
VALUES ('00000000-0000-0000-0000-000000000021', 1, 'Marketing', 'Marketing team members');

-- Add users to groups
INSERT INTO user_groups (user_id, group_id) VALUES (1, 1);  -- admin → Engineering
INSERT INTO user_groups (user_id, group_id) VALUES (2, 1);  -- editor → Engineering
INSERT INTO user_groups (user_id, group_id) VALUES (3, 2);  -- viewer → Marketing
INSERT INTO user_groups (user_id, group_id) VALUES (4, 1);  -- manager → Engineering
INSERT INTO user_groups (user_id, group_id) VALUES (4, 2);  -- manager → Marketing

-- Create a workspace
INSERT INTO workspaces (uuid, organization_id, name, description, status)
VALUES ('00000000-0000-0000-0000-000000000100', 1, 'Main Workspace', 'Primary content workspace', 'ACTIVE');

-- Assign users to workspace (all users get workspace-level role)
INSERT INTO user_workspace_roles (user_id, workspace_id, role_id)
VALUES (1, 1, 3),  -- admin → Admin on workspace
       (2, 1, 2),  -- editor → Editor on workspace
       (3, 1, 1),  -- viewer → Viewer on workspace
       (4, 1, 3);  -- manager → Admin on workspace

-- Create folder hierarchy
-- Root folders
INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000200', 1, NULL, 'Documents', 0, 'ACTIVE', 1);

INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000201', 1, NULL, 'Media', 1, 'ACTIVE', 1);

INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000202', 1, NULL, 'Projects', 2, 'ACTIVE', 1);

-- Subfolders under Documents
INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000210', 1, 1, 'Policies', 0, 'ACTIVE', 1);

INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000211', 1, 1, 'Reports', 1, 'ACTIVE', 2);

-- Subfolders under Projects
INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000220', 1, 3, 'Project Alpha', 0, 'ACTIVE', 4);

INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000221', 1, 3, 'Project Beta', 1, 'ACTIVE', 4);

-- Nested subfolder under Project Alpha
INSERT INTO folders (uuid, workspace_id, parent_id, name, sort_order, status, created_by)
VALUES ('00000000-0000-0000-0000-000000000230', 1, 6, 'Designs', 0, 'ACTIVE', 2);

-- Sample files in Documents folder
INSERT INTO files (uuid, folder_id, organization_id, workspace_id, name, original_name, size_bytes, mime_type, storage_key, storage_bucket, status, uploaded_by, upload_completed_at)
VALUES ('00000000-0000-0000-0000-000000000300', 1, 1, 1, 'welcome.pdf', 'welcome.pdf', 102400, 'application/pdf', 'files/1/welcome.pdf', 'cms-files', 'ACTIVE', 1, CURRENT_TIMESTAMP);

INSERT INTO files (uuid, folder_id, organization_id, workspace_id, name, original_name, size_bytes, mime_type, storage_key, storage_bucket, status, uploaded_by, upload_completed_at)
VALUES ('00000000-0000-0000-0000-000000000301', 1, 1, 1, 'readme.md', 'readme.md', 2048, 'text/markdown', 'files/1/readme.md', 'cms-files', 'ACTIVE', 1, CURRENT_TIMESTAMP);

-- Sample file in Reports folder
INSERT INTO files (uuid, folder_id, organization_id, workspace_id, name, original_name, size_bytes, mime_type, storage_key, storage_bucket, status, uploaded_by, upload_completed_at)
VALUES ('00000000-0000-0000-0000-000000000302', 5, 1, 1, 'q1-report.pdf', 'Q1 Report 2026.pdf', 524288, 'application/pdf', 'files/5/q1-report.pdf', 'cms-files', 'ACTIVE', 2, CURRENT_TIMESTAMP);

-- Sample image in Media folder
INSERT INTO files (uuid, folder_id, organization_id, workspace_id, name, original_name, size_bytes, mime_type, storage_key, storage_bucket, status, uploaded_by, upload_completed_at)
VALUES ('00000000-0000-0000-0000-000000000303', 2, 1, 1, 'logo.png', 'company-logo.png', 45056, 'image/png', 'files/2/logo.png', 'cms-files', 'ACTIVE', 1, CURRENT_TIMESTAMP);

-- Sample file in Project Alpha / Designs
INSERT INTO files (uuid, folder_id, organization_id, workspace_id, name, original_name, size_bytes, mime_type, storage_key, storage_bucket, status, uploaded_by, upload_completed_at)
VALUES ('00000000-0000-0000-0000-000000000304', 8, 1, 1, 'mockup-v1.png', 'mockup-v1.png', 1048576, 'image/png', 'files/8/mockup-v1.png', 'cms-files', 'ACTIVE', 2, CURRENT_TIMESTAMP);

-- Folder permissions (assign access)
-- Engineering group gets Editor on Documents folder
INSERT INTO folder_permissions (folder_id, user_id, group_id, role_id)
VALUES (1, NULL, 1, 2);

-- Marketing group gets Viewer on Documents folder
INSERT INTO folder_permissions (folder_id, user_id, group_id, role_id)
VALUES (1, NULL, 2, 1);

-- Admin gets Admin permission on all root folders
INSERT INTO folder_permissions (folder_id, user_id, group_id, role_id)
VALUES (1, 1, NULL, 3),
       (2, 1, NULL, 3),
       (3, 1, NULL, 3);

-- Engineering group gets Editor on Projects folder
INSERT INTO folder_permissions (folder_id, user_id, group_id, role_id)
VALUES (3, NULL, 1, 2);
