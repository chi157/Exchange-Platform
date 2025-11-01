package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposals", indexes = {
    @Index(name = "idx_proposer_id", columnList = "proposer_id"),
    @Index(name = "idx_receiver_id", columnList = "receiver_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposal extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proposal_proposer"))
    private User proposer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proposal_receiver"))
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProposalItem> proposalItems = new ArrayList<>();

    // Enum
    public enum ProposalStatus {
        PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
    }

    // Helper methods
    public void addProposalItem(ProposalItem item) {
        if (this.proposalItems == null) {
            this.proposalItems = new ArrayList<>();
        }
        this.proposalItems.add(item);
        item.setProposal(this);
    }

    public void removeProposalItem(ProposalItem item) {
        if (this.proposalItems != null) {
            this.proposalItems.remove(item);
            item.setProposal(null);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public void accept() {
        this.status = ProposalStatus.ACCEPTED;
    }

    public void reject() {
        this.status = ProposalStatus.REJECTED;
    }

    public void cancel() {
        this.status = ProposalStatus.CANCELLED;
    }

    public void markAsExpired() {
        this.status = ProposalStatus.EXPIRED;
    }

    public List<Listing> getProposerListings() {
        if (this.proposalItems == null) return new ArrayList<>();
        return this.proposalItems.stream()
            .filter(item -> item.getSide() == ProposalItem.Side.PROPOSER)
            .map(ProposalItem::getListing)
            .toList();
    }

    public List<Listing> getReceiverListings() {
        if (this.proposalItems == null) return new ArrayList<>();
        return this.proposalItems.stream()
            .filter(item -> item.getSide() == ProposalItem.Side.RECEIVER)
            .map(ProposalItem::getListing)
            .toList();
    }
}
