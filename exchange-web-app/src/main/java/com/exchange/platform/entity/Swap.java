package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "swaps", indexes = {
        @Index(name = "idx_swaps_listing", columnList = "listing_id"),
        @Index(name = "idx_swaps_users", columnList = "a_user_id,b_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Swap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "proposal_id", nullable = false)
    private Long proposalId;

    // 通常 a=listing owner, b=proposer
    @Column(name = "a_user_id", nullable = false)
    private Long aUserId;

    @Column(name = "b_user_id", nullable = false)
    private Long bUserId;

    // Legacy columns present in existing MySQL schema (NOT NULL). Keep them in sync on persist/update.
    @Column(name = "user_a_id", nullable = false)
    private Long userAIdLegacy;

    @Column(name = "user_b_id", nullable = false)
    private Long userBIdLegacy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.IN_PROGRESS;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // M5: Per-user delivery confirmations
    @Column(name = "a_confirmed_at")
    private LocalDateTime aConfirmedAt;

    @Column(name = "b_confirmed_at")
    private LocalDateTime bConfirmedAt;

    // 配送方式協商（面交或交貨便需要雙方同意）
    @Column(name = "delivery_method", length = 20)
    private String deliveryMethod; // "FACE_TO_FACE" 或 "SHIPNOW"

    @Column(name = "a_delivery_method_confirmed")
    private Boolean aDeliveryMethodConfirmed;

    @Column(name = "b_delivery_method_confirmed")
    private Boolean bDeliveryMethodConfirmed;

    // 面交資訊（僅當 deliveryMethod = FACE_TO_FACE 時使用）
    @Column(name = "meetup_location", length = 500)
    private String meetupLocation;

    @Column(name = "meetup_time")
    private LocalDateTime meetupTime;

    @Column(name = "meetup_notes", columnDefinition = "TEXT")
    private String meetupNotes;

    // 面交確認狀態
    @Column(name = "a_meetup_confirmed")
    private Boolean aMeetupConfirmed;

    @Column(name = "b_meetup_confirmed")
    private Boolean bMeetupConfirmed;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = Status.IN_PROGRESS;
        // Sync legacy columns
        if (this.userAIdLegacy == null) this.userAIdLegacy = this.aUserId;
        if (this.userBIdLegacy == null) this.userBIdLegacy = this.bUserId;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Keep legacy columns updated alongside primary columns
        if (this.aUserId != null) this.userAIdLegacy = this.aUserId;
        if (this.bUserId != null) this.userBIdLegacy = this.bUserId;
    }

    public enum Status { PENDING, IN_PROGRESS, COMPLETED, CANCELED }
}
