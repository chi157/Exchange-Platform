-- V7: 建立 reviews 與 disputes 資料表
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    swap_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewed_user_id BIGINT NOT NULL,
    scores JSON,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (swap_id) REFERENCES swaps(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_swap (swap_id),
    INDEX idx_reviewer (reviewer_id),
    INDEX idx_reviewed_user (reviewed_user_id),
    UNIQUE KEY unique_review_per_swap (swap_id, reviewer_id, reviewed_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE disputes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    swap_id BIGINT NOT NULL,
    claimant_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    evidence_refs JSON,
    status VARCHAR(20) DEFAULT 'OPEN',
    admin_resolution TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (swap_id) REFERENCES swaps(id) ON DELETE CASCADE,
    FOREIGN KEY (claimant_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_swap (swap_id),
    INDEX idx_claimant (claimant_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
