package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listings_owner", columnList = "owner_id"),
        @Index(name = "idx_listings_created", columnList = "created_at")
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

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // === 新增卡片屬性欄位 ===
    @Column(name = "card_name", length = 200)
    private String cardName;

    @Column(name = "group_name", length = 100)
    private String groupName;

    @Column(name = "artist_name", length = 100)
    private String artistName;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_source", length = 20)
    private CardSource cardSource;

    @Column(name = "condition_rating")
    private Integer conditionRating; // 1-10成新

    @Column(name = "has_protection")
    @Builder.Default
    private Boolean hasProtection = false;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "image_paths", columnDefinition = "TEXT")
    private String imagePaths; // JSON array of image file paths

    // 與現有資料表相容：DB 同時存在 user_id 與 owner_id 皆為 NOT NULL
    // 我們以 ownerId 對應 user_id，並新增 legacy 欄位對應 owner_id，以避免插入失敗
    @Column(name = "user_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerIdLegacy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    @Column(name = "status_rank")
    private Integer statusRank = 0;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.ownerIdLegacy == null) {
            this.ownerIdLegacy = this.ownerId;
        }
        if (this.status == null) this.status = Status.ACTIVE;
        updateStatusRank();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        updateStatusRank();
    }
    
    @PostLoad
    public void postLoad() {
        // 處理舊數據中可能為 null 的 statusRank
        if (this.statusRank == null) {
            updateStatusRank();
        }
    }
    
    private void updateStatusRank() {
        this.statusRank = (this.status == Status.COMPLETED) ? 1 : 0;
    }

    // 包含 AVAILABLE 作為舊資料相容值，行為視同 ACTIVE
    public enum Status { ACTIVE, LOCKED, COMPLETED, AVAILABLE }

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
