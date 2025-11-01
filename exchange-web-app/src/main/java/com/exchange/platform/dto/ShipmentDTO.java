package com.exchange.platform.dto;

import com.exchange.platform.entity.Shipment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShipmentDTO {
    private Long id;
    private Long swapId;
    private Long senderId;
    private Shipment.DeliveryMethod deliveryMethod;
    private String trackingNumber;
    private String trackingUrl;
    private String lastStatus;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
