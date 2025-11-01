package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_swap_id", columnList = "swap_id"),
    @Index(name = "idx_reviewer_id", columnList = "reviewer_id"),
    @Index(name = "idx_reviewee_id", columnList = "reviewee_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_swap_reviewer", columnNames = {"swap_id", "reviewer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_swap"))
    private Swap swap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_reviewer"))
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_reviewee"))
    private User reviewee;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    @Builder.Default
    private Map<String, Integer> scores = new HashMap<>();

    @Column(columnDefinition = "TEXT")
    private String comment;

    // Helper methods
    public void setScore(String criterion, Integer score) {
        if (this.scores == null) {
            this.scores = new HashMap<>();
        }
        this.scores.put(criterion, score);
    }

    public Integer getScore(String criterion) {
        if (this.scores == null) return null;
        return this.scores.get(criterion);
    }

    public Double getAverageScore() {
        if (this.scores == null || this.scores.isEmpty()) {
            return 0.0;
        }
        return this.scores.values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }
}