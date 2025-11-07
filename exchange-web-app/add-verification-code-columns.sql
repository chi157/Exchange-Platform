-- 添加驗證碼相關欄位到 users 表

ALTER TABLE users 
ADD COLUMN verification_code VARCHAR(10) NULL COMMENT '電子郵件驗證碼',
ADD COLUMN verification_code_expires_at TIMESTAMP NULL COMMENT '驗證碼過期時間';

-- 為驗證碼欄位添加索引（可選，提升查詢效率）
CREATE INDEX idx_users_verification_code ON users(verification_code);

-- 查看更新後的表結構
DESCRIBE users;
