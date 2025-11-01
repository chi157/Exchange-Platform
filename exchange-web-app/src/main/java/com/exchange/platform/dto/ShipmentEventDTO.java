package com.exchange.platform.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShipmentEventDTO {
    private Long id;
    private Long shipmentId;
    private String status;
    private String note;
    private LocalDateTime at;
    private LocalDateTime createdAt;
}
