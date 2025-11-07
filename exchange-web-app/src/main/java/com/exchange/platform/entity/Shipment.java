package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shipment_swap_sender", columnNames = {"swap_id", "sender_id"})
}, indexes = {
        @Index(name = "idx_shipments_swap", columnList = "swap_id"),
        @Index(name = "idx_shipments_sender", columnList = "sender_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "swap_id", nullable = false)
    private Long swapId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    // Legacy column for existing MySQL schema compatibility (NOT NULL)
    // Represents the counterparty (receiver) of this shipment's sender in the same swap
    @Column(name = "receiver_id")
    private Long receiverIdLegacy;

    @Convert(converter = DeliveryMethodConverter.class)
    @Column(name = "delivery_method", nullable = false, length = 32)
    private DeliveryMethod deliveryMethod;

    // 交貨便：使用者希望的收貨711門市
    @Column(name = "preferred_store_711", length = 500)
    private String preferredStore711;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "tracking_url")
    private String trackingUrl;

    @Column(name = "last_status")
    private String lastStatus;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.shippedAt == null) this.shippedAt = now;
    }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public enum DeliveryMethod { SHIPNOW, FACE_TO_FACE }
}
