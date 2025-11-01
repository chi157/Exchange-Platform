package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_events", indexes = {
    @Index(name = "idx_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_event_time", columnList = "event_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shipment_event_shipment"))
    private Shipment shipment;

    @Column(name = "event_time", nullable = false)
    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(nullable = false, length = 100)
    private String status;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;
}