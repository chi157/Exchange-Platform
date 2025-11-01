package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disputes", indexes = {
    @Index(name = "idx_swap_id", columnList = "swap_id"),
    @Index(name = "idx_complainant_id", columnList = "complainant_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute extends AuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_dispute_swap"))
    private Swap swap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dispute_complainant"))
    private User complainant;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_refs", columnDefinition = "json")
    @Builder.Default
    private List<String> evidenceRefs = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DisputeStatus status = DisputeStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", foreignKey = @ForeignKey(name = "fk_dispute_admin"))
    private User admin;

    @Column(name = "admin_resolution", columnDefinition = "TEXT")
    private String adminResolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Enum
    public enum DisputeStatus {
        OPEN, IN_REVIEW, RESOLVED_FAVOR_A, RESOLVED_FAVOR_B, RESOLVED_NO_FAULT, CLOSED
    }

    // Helper methods
    public void addEvidence(String evidenceUrl) {
        if (this.evidenceRefs == null) {
            this.evidenceRefs = new ArrayList<>();
        }
        this.evidenceRefs.add(evidenceUrl);
    }

    public void assignToAdmin(User admin) {
        this.admin = admin;
        this.status = DisputeStatus.IN_REVIEW;
    }

    public void resolve(DisputeStatus resolution, String resolutionText) {
        this.status = resolution;
        this.adminResolution = resolutionText;
        this.resolvedAt = LocalDateTime.now();
    }

    public boolean isResolved() {
        return this.status == DisputeStatus.RESOLVED_FAVOR_A ||
               this.status == DisputeStatus.RESOLVED_FAVOR_B ||
               this.status == DisputeStatus.RESOLVED_NO_FAULT ||
               this.status == DisputeStatus.CLOSED;
    }
}