package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_idol_group", columnList = "idol_group"),
    @Index(name = "idx_member_name", columnList = "member_name"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_listing_user"))
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "idol_group", nullable = false, length = 100)
    private String idolGroup;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(length = 200)
    private String album;

    @Column(length = 100)
    private String era;

    @Column(length = 100)
    private String version;

    @Column(name = "card_code", length = 50)
    private String cardCode;

    @Column(name = "is_official", nullable = false)
    @Builder.Default
    private Boolean isOfficial = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardCondition condition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "locked_by_proposal_id")
    private Long lockedByProposalId;

    public enum CardCondition {
        S, A, B, C
    }

    public enum ListingStatus {
        ACTIVE, LOCKED, TRADED, DELETED
    }

    public void addPhoto(String photoUrl) {
        if (this.photos == null) {
            this.photos = new ArrayList<>();
        }
        this.photos.add(photoUrl);
    }

    public void removePhoto(String photoUrl) {
        if (this.photos != null) {
            this.photos.remove(photoUrl);
        }
    }

    public boolean isAvailable() {
        return this.status == ListingStatus.ACTIVE && this.lockedByProposalId == null;
    }

    public void lock(Long proposalId) {
        this.status = ListingStatus.LOCKED;
        this.lockedByProposalId = proposalId;
    }

    public void unlock() {
        this.status = ListingStatus.ACTIVE;
        this.lockedByProposalId = null;
    }

    public void markAsTraded() {
        this.status = ListingStatus.TRADED;
    }

    public void markAsDeleted() {
        this.status = ListingStatus.DELETED;
    }
}