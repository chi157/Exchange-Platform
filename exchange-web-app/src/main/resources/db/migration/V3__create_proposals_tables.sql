-- V3: 建立 proposals 與 proposal_items 資料表
CREATE TABLE proposals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposer_id BIGINT NOT NULL,
    proposee_listing_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    expires_at DATETIME,
    message TEXT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (proposer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (proposee_listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    INDEX idx_proposer (proposer_id),
    INDEX idx_proposee_listing (proposee_listing_id),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE proposal_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('OFFERED', 'REQUESTED')),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (proposal_id) REFERENCES proposals(id) ON DELETE CASCADE,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
    INDEX idx_proposal (proposal_id),
    INDEX idx_listing (listing_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
