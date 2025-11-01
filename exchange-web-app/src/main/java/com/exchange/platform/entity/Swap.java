package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "swaps", indexes = {
    @Index(name = "idx_proposal_id", columnList = "proposal_id"),
    @Index(name = "idx_user_a_id", columnList = "user_a_id"),
    @Index(name = "idx_user_b_id", columnList = "user_b_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Swap extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_swap_proposal"))
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_id", nullable = false, foreignKey = @ForeignKey(name = "fk_swap_user_a"))
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_id", nullable = false, foreignKey = @ForeignKey(name = "fk_swap_user_b"))
    private User userB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SwapStatus status = SwapStatus.SHIPPING;

    @Column(name = "received_a_confirmed", nullable = false)
    @Builder.Default
    private Boolean receivedAConfirmed = false;

    @Column(name = "received_b_confirmed", nullable = false)
    @Builder.Default
    private Boolean receivedBConfirmed = false;

    // Enum
    public enum SwapStatus {
        SHIPPING, COMPLETED, DISPUTED
    }

    // Helper methods
    public void confirmReceivedByUserA() {
        this.receivedAConfirmed = true;
        checkBothConfirmed();
    }

    public void confirmReceivedByUserB() {
        this.receivedBConfirmed = true;
        checkBothConfirmed();
    }

    private void checkBothConfirmed() {
        if (this.receivedAConfirmed && this.receivedBConfirmed) {
            this.status = SwapStatus.COMPLETED;
        }
    }

    public void markAsDisputed() {
        this.status = SwapStatus.DISPUTED;
    }

    public boolean isCompleted() {
        return this.status == SwapStatus.COMPLETED;
    }

    public boolean isDisputed() {
        return this.status == SwapStatus.DISPUTED;
    }
}
