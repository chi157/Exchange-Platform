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

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.ownerIdLegacy == null) {
            this.ownerIdLegacy = this.ownerId;
        }
        if (this.status == null) this.status = Status.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 包含 AVAILABLE 作為舊資料相容值，行為視同 ACTIVE
    public enum Status { ACTIVE, LOCKED, COMPLETED, AVAILABLE }
}
