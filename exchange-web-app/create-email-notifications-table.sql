-- 創建 email_notifications 表
CREATE TABLE IF NOT EXISTS email_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content TEXT,
    related_entity_type VARCHAR(50),
    related_entity_id BIGINT,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_recipient_id (recipient_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_related_entity (related_entity_type, related_entity_id),
    INDEX idx_sent_status (sent),
    INDEX idx_created_at (created_at),
    
    CONSTRAINT fk_email_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- 插入一些測試通知（可選）
-- INSERT INTO email_notifications (recipient_id, email, notification_type, subject, content, related_entity_type, related_entity_id)
-- VALUES 
-- (1, 'test@example.com', 'PROPOSAL_RECEIVED', '您有新的交換提案！', '您收到了一個新的卡片交換提案，請登入查看。', 'Proposal', 1);