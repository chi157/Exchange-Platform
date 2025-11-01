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
