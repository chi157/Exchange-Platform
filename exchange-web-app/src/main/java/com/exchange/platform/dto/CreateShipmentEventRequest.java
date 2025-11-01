package com.exchange.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateShipmentEventRequest {
    @NotBlank
    private String status;

    private String note;

    @NotNull
    private LocalDateTime at;
}
