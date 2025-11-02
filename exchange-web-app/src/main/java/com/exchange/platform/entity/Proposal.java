package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposals", indexes = {
        @Index(name = "idx_proposals_listing", columnList = "listing_id"),
        @Index(name = "idx_proposals_proposer", columnList = "proposer_id"),
        @Index(name = "idx_proposals_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    // Legacy DB compatibility: some existing schemas require a non-null proposee_listing_id
    // For MVP (single-sided proposal), we mirror listingId into this legacy column at persist time
    @Column(name = "proposee_listing_id", nullable = false)
    private Long proposeeListingIdLegacy;

    // Legacy DB compatibility: some schemas require receiver_id (the listing owner user id)
    @Column(name = "receiver_id", nullable = false)
    private Long receiverIdLegacy;

    @Column(name = "proposer_id", nullable = false)
    private Long proposerId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProposalItem> proposalItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.proposeeListingIdLegacy == null) {
            this.proposeeListingIdLegacy = this.listingId;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum Status { PENDING, ACCEPTED, REJECTED }
}
