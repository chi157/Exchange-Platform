-- V4: 建立 swaps 資料表
CREATE TABLE swaps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT UNIQUE,
    a_user_id BIGINT NOT NULL,
    b_user_id BIGINT NOT NULL,
    status VARCHAR(30) DEFAULT 'INIT',
    received_a_confirmed BOOLEAN DEFAULT FALSE,
    received_b_confirmed BOOLEAN DEFAULT FALSE,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE SET NULL,
    FOREIGN KEY (a_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (b_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_a_user (a_user_id),
    INDEX idx_b_user (b_user_id),
    INDEX idx_status (status),
    INDEX idx_proposal (proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
