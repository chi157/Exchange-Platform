-- V5: 建立 shipments 與 shipment_events 資料表
CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    swap_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    delivery_method VARCHAR(20) CHECK (delivery_method IN ('賣貨便', 'face_to_face')),
    tracking_number VARCHAR(100),
    tracking_url VARCHAR(500),
    last_status VARCHAR(20) DEFAULT 'PENDING',
    shipped_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (swap_id) REFERENCES swaps(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_swap (swap_id),
    INDEX idx_sender (sender_id),
    INDEX idx_last_status (last_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shipment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    actor_id BIGINT,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_shipment (shipment_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
