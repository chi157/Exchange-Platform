-- V1: 建立 users 資料表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    verified BOOLEAN DEFAULT FALSE NOT NULL,
    roles VARCHAR(100) DEFAULT 'ROLE_USER',
    risk_score INT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_risk_score (risk_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入預設管理員帳號 (密碼: admin123，需用 BCrypt 加密)
-- $2a$10$... 為 BCrypt 加密後的 "admin123"
INSERT INTO users (email, password_hash, display_name, verified, roles, risk_score)
VALUES ('admin@exchange-platform.com', 
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 
        'System Admin', 
        TRUE, 
        'ROLE_USER,ROLE_ADMIN', 
        0);
