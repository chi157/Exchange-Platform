package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_events", indexes = {
        @Index(name = "idx_shipment_events_shipment", columnList = "shipment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Column(name = "note")
    private String note;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime at;

    // Legacy column - same value as event_time
    @Column(name = "at", nullable = false)
    private LocalDateTime atLegacy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.at == null) this.at = LocalDateTime.now();
        this.atLegacy = this.at; // Sync legacy column
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atLegacy = this.at; // Keep in sync
    }
}
