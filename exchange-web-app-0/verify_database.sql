-- 查詢所有資料表
SHOW TABLES;

-- 查詢 Flyway 版本記錄
SELECT version, description, installed_on, success 
FROM flyway_schema_history 
ORDER BY installed_rank;

-- 查詢預設管理員帳號
SELECT id, email, display_name, verified, roles, risk_score, created_at 
FROM users;

-- 查詢各資料表的欄位結構
DESCRIBE users;
DESCRIBE listings;
DESCRIBE proposals;
DESCRIBE proposal_items;
DESCRIBE swaps;
DESCRIBE shipments;
DESCRIBE shipment_events;
DESCRIBE messages;
DESCRIBE reviews;
DESCRIBE disputes;