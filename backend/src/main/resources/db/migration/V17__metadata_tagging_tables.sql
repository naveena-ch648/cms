-- Metadata & Tagging System tables

CREATE TABLE metadata_fields (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    field_type ENUM('TEXT','NUMBER','DATE','DROPDOWN') NOT NULL,
    description VARCHAR(500) NULL,
    options JSON NULL,
    required BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mf_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    INDEX idx_mf_workspace_active (workspace_id, deleted_at),
    INDEX idx_mf_workspace_order (workspace_id, display_order),
    UNIQUE INDEX idx_mf_workspace_name (workspace_id, name, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE metadata_values (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    field_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    text_value VARCHAR(1000) NULL,
    number_value DECIMAL(20,6) NULL,
    date_value DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mv_field FOREIGN KEY (field_id) REFERENCES metadata_fields(id),
    CONSTRAINT fk_mv_file FOREIGN KEY (file_id) REFERENCES files(id),
    UNIQUE INDEX idx_mv_file_field (file_id, field_id),
    INDEX idx_mv_field (field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE file_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    CONSTRAINT fk_ft_file FOREIGN KEY (file_id) REFERENCES files(id),
    CONSTRAINT fk_ft_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_ft_user FOREIGN KEY (created_by) REFERENCES users(id),
    UNIQUE INDEX idx_tag_file_name (file_id, name),
    INDEX idx_tag_workspace_name (workspace_id, name),
    INDEX idx_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
