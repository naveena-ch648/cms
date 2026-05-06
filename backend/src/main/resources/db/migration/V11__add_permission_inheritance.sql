-- V11: Add permission inheritance support to folder_permissions
ALTER TABLE folder_permissions
    ADD COLUMN is_override BOOLEAN NOT NULL DEFAULT FALSE;
