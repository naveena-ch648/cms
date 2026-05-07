-- Feature 007: Collaboration System
-- Extend comments for folder discussions, create mentions, tasks, notifications tables

-- Extend comments table for folder discussions
ALTER TABLE comments
    MODIFY COLUMN file_id BIGINT NULL,
    ADD COLUMN folder_id BIGINT NULL AFTER file_id,
    ADD CONSTRAINT fk_comment_folder FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE CASCADE,
    ADD INDEX idx_comment_folder (folder_id, created_at);

-- Mentions table
CREATE TABLE mentions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mention_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_mention_user FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_mention_comment_user (comment_id, mentioned_user_id),
    INDEX idx_mention_user (mentioned_user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tasks table
CREATE TABLE tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    file_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    assignee_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status ENUM('OPEN', 'DONE') NOT NULL DEFAULT 'OPEN',
    due_date DATE,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_task_file_status (file_id, status),
    INDEX idx_task_assignee (assignee_id, status, due_date),
    INDEX idx_task_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid CHAR(36) NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL,
    type ENUM('MENTION', 'TASK_ASSIGNED', 'TASK_COMPLETED') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500),
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    actor_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notification_recipient (recipient_id, is_read, created_at DESC),
    INDEX idx_notification_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
