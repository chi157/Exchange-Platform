package com.exchange.platform.service;

import com.exchange.platform.dto.CreateShipmentEventRequest;
import com.exchange.platform.dto.ShipmentDTO;
import com.exchange.platform.dto.UpsertShipmentRequest;
import com.exchange.platform.entity.Shipment;
import com.exchange.platform.entity.ShipmentEvent;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.repository.ShipmentEventRepository;
import com.exchange.platform.repository.ShipmentRepository;
import com.exchange.platform.repository.SwapRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final SwapRepository swapRepository;

    private static final String SESSION_USER_ID = "userId";

    public ShipmentDTO getMyShipment(Long swapId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();

        return shipmentRepository.findBySwapIdAndSenderId(swapId, userId)
                .map(this::toDTO)
                .orElseThrow(NotFoundException::new);
    }

    public java.util.List<ShipmentDTO> getAllShipments(Long swapId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();

        return shipmentRepository.findBySwapId(swapId).stream()
                .map(this::toDTO)
                .toList();
    }

    public ShipmentDTO upsertMyShipment(Long swapId, UpsertShipmentRequest req, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();

        Shipment.DeliveryMethod method = parseMethod(req.getDeliveryMethod());
        // Note: For SHIPNOW, preferredStore711 can be filled first, trackingNumber can be filled later

    Long receiverId = swap.getAUserId().equals(userId) ? swap.getBUserId() : swap.getAUserId();
    Shipment shipment = shipmentRepository.findBySwapIdAndSenderId(swapId, userId)
        .orElseGet(() -> Shipment.builder()
            .swapId(swapId)
            .senderId(userId)
            .deliveryMethod(method)
            .preferredStore711(req.getPreferredStore711())
            .trackingNumber(req.getTrackingNumber())
            .trackingUrl(req.getTrackingUrl())
            .build());

        // Update fields if exists
        shipment.setDeliveryMethod(method);
        if (method == Shipment.DeliveryMethod.FACE_TO_FACE) {
            shipment.setTrackingNumber(null);
            shipment.setTrackingUrl(null);
            shipment.setPreferredStore711(null);
        } else {
            shipment.setPreferredStore711(req.getPreferredStore711());
            shipment.setTrackingNumber(req.getTrackingNumber());
            shipment.setTrackingUrl(req.getTrackingUrl());
        }
    // Always (re)assign legacy receiver_id for compatibility and to satisfy DB CHECKs
    shipment.setReceiverIdLegacy(receiverId);

        shipment = shipmentRepository.save(shipment);
        return toDTO(shipment);
    }

    public void addEvent(Long shipmentId, CreateShipmentEventRequest req, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Shipment shipment = shipmentRepository.findById(shipmentId).orElseThrow(NotFoundException::new);
        if (!shipment.getSenderId().equals(userId)) throw new ForbiddenException();

        ShipmentEvent ev = ShipmentEvent.builder()
                .shipmentId(shipment.getId())
                .status(req.getStatus())
                .note(req.getNote())
                .at(req.getAt())
                .build();
        shipmentEventRepository.save(ev);

        shipment.setLastStatus(req.getStatus());
        shipmentRepository.save(shipment);
    }

    public ShipmentDTO toDTO(Shipment s) {
        return ShipmentDTO.builder()
                .id(s.getId())
                .swapId(s.getSwapId())
                .senderId(s.getSenderId())
                .deliveryMethod(s.getDeliveryMethod())
                .preferredStore711(s.getPreferredStore711())
                .trackingNumber(s.getTrackingNumber())
                .trackingUrl(s.getTrackingUrl())
                .lastStatus(s.getLastStatus())
                .shippedAt(s.getShippedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private Shipment.DeliveryMethod parseMethod(String method) {
        String m = method == null ? "" : method.trim().toLowerCase(Locale.ROOT);
        if ("shipnow".equals(m)) return Shipment.DeliveryMethod.SHIPNOW;
        if ("face_to_face".equals(m)) return Shipment.DeliveryMethod.FACE_TO_FACE;
        throw new BadRequestException();
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class BadRequestException extends RuntimeException {}
}
