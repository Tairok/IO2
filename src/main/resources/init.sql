-- Cloud Storage Database Initialization Script
-- This script creates tables if they don't exist, clears existing data, and loads initial data on every app launch.

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    login VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

-- Create files table
CREATE TABLE IF NOT EXISTS files (
    id INT PRIMARY KEY AUTO_INCREMENT,
    owner_id INT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create settings table
CREATE TABLE IF NOT EXISTS settings (
    user_id INT PRIMARY KEY,
    display_name VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Clear existing data (but keep tables)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE files;
TRUNCATE TABLE settings;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- Insert initial admin user
-- Password: password (BCrypt hash)
INSERT INTO users (login, password_hash, email, full_name, role)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@capp.com', 'Administrator', 'ADMIN');

-- Insert sample user
-- Password: password (BCrypt hash)
INSERT INTO users (login, password_hash, email, full_name, role)
VALUES ('user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'user@capp.com', 'Sample User', 'USER');

-- Insert another sample user
-- Password: password (BCrypt hash)
INSERT INTO users (login, password_hash, email, full_name, role)
VALUES ('testuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'test@capp.com', 'Test User', 'USER');

-- Insert settings for admin
INSERT INTO settings (user_id, display_name)
VALUES (1, 'Admin');

-- Insert settings for sample user
INSERT INTO settings (user_id, display_name)
VALUES (2, 'User');

-- Insert settings for test user
INSERT INTO settings (user_id, display_name)
VALUES (3, 'Test');