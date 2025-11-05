package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_user", columnList = "user_id"),
        @Index(name = "idx_listings_created", columnList = "created_at"),
        @Index(name = "idx_listings_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 卡片名稱 (必填)
    @Column(name = "card_name", nullable = false, length = 200)
    private String cardName;

    // 2. 團體名稱 (選填)
    @Column(name = "group_name", length = 100)
    private String groupName;

    // 3. 藝人名稱 (必填)
    @Column(name = "artist_name", nullable = false, length = 100)
    private String artistName;

    // 4. 描述 (選填)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // 5. 卡片來源 (必填)
    @Enumerated(EnumType.STRING)
    @Column(name = "card_source", nullable = false, length = 20)
    private CardSource cardSource;

    // 6. 品相等級 (必填, 1-10)
    @Column(name = "condition_rating", nullable = false)
    private Integer conditionRating;

    // 7. 是否有保護措施 (必填)
    @Column(name = "has_protection", nullable = false)
    @Builder.Default
    private Boolean hasProtection = false;

    // 8. 備註 (選填)
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // 9. 圖片 (必填, JSON array of image file paths)
    @Column(name = "image_paths", nullable = false, columnDefinition = "TEXT")
    private String imagePaths;

    // 擁有者ID
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 10. 上架時間 (系統自動帶入)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 更新時間
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 11. 卡片狀態 (必填)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = Status.AVAILABLE;
        }
        if (this.hasProtection == null) {
            this.hasProtection = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 卡片狀態枚舉
    public enum Status {
        AVAILABLE("可交換"),
        LOCKED("已鎖定"),
        PENDING("交換中"),
        COMPLETED("已完成");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 卡片來源枚舉
    public enum CardSource {
        ALBUM("專輯"),
        CONCERT("演唱會"),
        FAN_MEETING("粉絲見面會"),
        EVENT_CARD("活動卡"),
        SPECIAL_CARD("特典卡"),
        UNOFFICIAL("非官方卡");

        private final String displayName;

        CardSource(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
