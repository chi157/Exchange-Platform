package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    // 為避免 JSON 欄位帶來的 DDL 問題，先用簡單的逗號字串儲存角色
    @Column(name = "roles", nullable = false, length = 100)
    @Builder.Default
    private String roles = "USER";

    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "is_blacklisted", nullable = false)
    @Builder.Default
    private Boolean isBlacklisted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}