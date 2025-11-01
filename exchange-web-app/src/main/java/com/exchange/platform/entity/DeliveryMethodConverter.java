package com.exchange.platform.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DeliveryMethodConverter implements AttributeConverter<Shipment.DeliveryMethod, String> {
    @Override
    public String convertToDatabaseColumn(Shipment.DeliveryMethod attribute) {
        if (attribute == null) return null;
        return switch (attribute) {
            case SHIPNOW -> "shipnow";
            case FACE_TO_FACE -> "face_to_face";
        };
    }

    @Override
    public Shipment.DeliveryMethod convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String v = dbData.trim().toLowerCase();
        return switch (v) {
            case "shipnow" -> Shipment.DeliveryMethod.SHIPNOW;
            case "face_to_face" -> Shipment.DeliveryMethod.FACE_TO_FACE;
            default -> throw new IllegalArgumentException("Unknown delivery_method: " + dbData);
        };
    }
}
