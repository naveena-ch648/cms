-- V4: Create folder system tables
-- Tables: folders, folder_permissions, folder_favorites, folder_recents

CREATE TABLE folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    parent_id BIGINT,
    name VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY idx_folder_uuid (uuid),
    KEY idx_folder_ws (workspace_id),
    KEY idx_folder_parent (parent_id),
    KEY idx_folder_ws_parent (workspace_id, parent_id),
    CONSTRAINT fk_folder_ws FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_parent FOREIGN KEY (parent_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_folder_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE folder_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    folder_id BIGINT NOT NULL,
    user_id BIGINT,
    group_id BIGINT,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY idx_fp_folder_user (folder_id, user_id),
    UNIQUE KEY idx_fp_folder_group (folder_id, group_id),
    KEY idx_fp_user (user_id),
    KEY idx_fp_role (role_id),
    CONSTRAINT fk_fp_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_group FOREIGN KEY (group_id) REFERENCES groups_table(id) ON DELETE CASCADE,
    CONSTRAINT fk_fp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE folder_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY idx_ff_user_folder (user_id, folder_id),
    KEY idx_ff_user (user_id),
    CONSTRAINT fk_ff_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ff_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE folder_recents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY idx_fr_user_folder (user_id, folder_id),
    KEY idx_fr_user_accessed (user_id, accessed_at),
    CONSTRAINT fk_fr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fr_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
