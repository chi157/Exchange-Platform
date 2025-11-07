-- 修復 chat_rooms 表的 status 欄位長度問題
-- 確保可以存儲 'READ_ONLY'（9個字符）

-- 使用資料庫
USE exchange_platform;

-- 查看當前欄位定義
DESCRIBE chat_rooms;

-- 修改 status 欄位長度
ALTER TABLE chat_rooms 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- 驗證修改
DESCRIBE chat_rooms;
