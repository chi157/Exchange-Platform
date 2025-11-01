package com.exchange.platform.service;

import com.exchange.platform.entity.Shipment;
import com.exchange.platform.entity.ShipmentEvent;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final SwapService swapService;
    private final UserService userService;

    public Shipment createShipment(Long swapId, Long senderId, Long receiverId,
                                  Shipment.DeliveryMethod method, String trackingNumber) {
        Swap swap = swapService.getSwapById(swapId);
        User sender = userService.getUserById(senderId);
        User receiver = userService.getUserById(receiverId);
        
        Shipment shipment = Shipment.builder()
                .swap(swap)
                .sender(sender)
                .receiver(receiver)
                .deliveryMethod(method)
                .trackingNumber(trackingNumber)
                .build();
        
        return shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public List<Shipment> getSwapShipments(Long swapId) {
        return shipmentRepository.findBySwapId(swapId);
    }

    public Shipment addShipmentEvent(Long shipmentId, String status, String location, String description) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        
        ShipmentEvent event = ShipmentEvent.builder()
                .shipment(shipment)
                .status(status)
                .location(location)
                .description(description)
                .build();
        
        shipment.addEvent(event);
        return shipmentRepository.save(shipment);
    }
}
