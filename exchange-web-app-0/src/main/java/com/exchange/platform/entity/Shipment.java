package com.exchange.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_swap_id", columnList = "swap_id"),
    @Index(name = "idx_sender_id", columnList = "sender_id"),
    @Index(name = "idx_receiver_id", columnList = "receiver_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shipment_swap"))
    private Swap swap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shipment_sender"))
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shipment_receiver"))
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false, length = 20)
    private DeliveryMethod deliveryMethod;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("eventTime DESC")
    @Builder.Default
    private List<ShipmentEvent> events = new ArrayList<>();

    // Enum
    public enum DeliveryMethod {
        CVS_711, // 7-11 賣貨便
        FACE_TO_FACE
    }

    // Helper methods
    public void addEvent(ShipmentEvent event) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        this.events.add(event);
        event.setShipment(this);
    }

    public ShipmentEvent getLatestEvent() {
        if (this.events == null || this.events.isEmpty()) {
            return null;
        }
        return this.events.get(0);
    }

    public boolean isFaceToFace() {
        return this.deliveryMethod == DeliveryMethod.FACE_TO_FACE;
    }

    public boolean hasCVSTracking() {
        return this.deliveryMethod == DeliveryMethod.CVS_711 && this.trackingNumber != null;
    }
}
