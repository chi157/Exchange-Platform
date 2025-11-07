package com.exchange.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpsertShipmentRequest {
    // 接受 "shipnow" 或 "face_to_face"（不分大小寫）
    @NotBlank
    @Pattern(regexp = "(?i)^(shipnow|face_to_face)$", message = "deliveryMethod must be shipnow or face_to_face")
    private String deliveryMethod;

    private String preferredStore711;
    private String trackingNumber;
    private String trackingUrl;
}
