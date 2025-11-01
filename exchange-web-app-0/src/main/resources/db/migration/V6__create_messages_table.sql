-- V6: 建立 messages 資料表
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT,
    swap_id BIGINT,
    from_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    attachments JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    FOREIGN KEY (swap_id) REFERENCES swaps(id) ON DELETE CASCADE,
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_proposal (proposal_id),
    INDEX idx_swap (swap_id),
    INDEX idx_from_user (from_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
