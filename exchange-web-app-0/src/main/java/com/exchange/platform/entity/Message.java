package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_proposal_id", columnList = "proposal_id"),
    @Index(name = "idx_swap_id", columnList = "swap_id"),
    @Index(name = "idx_sender_id", columnList = "sender_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", foreignKey = @ForeignKey(name = "fk_message_proposal"))
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_id", foreignKey = @ForeignKey(name = "fk_message_swap"))
    private Swap swap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_message_sender"))
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    // Helper methods
    public void markAsRead() {
        this.isRead = true;
    }

    public boolean belongsToProposal() {
        return this.proposal != null;
    }

    public boolean belongsToSwap() {
        return this.swap != null;
    }
}