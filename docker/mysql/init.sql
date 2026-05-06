-- CMS Platform Database Initialization
-- This script runs on first MySQL container start

CREATE DATABASE IF NOT EXISTS cms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'cmsuser'@'%' IDENTIFIED BY 'cmspassword';
GRANT ALL PRIVILEGES ON cms.* TO 'cmsuser'@'%';
FLUSH PRIVILEGES;
