package com.exchange.platform.repository;

import com.exchange.platform.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findBySwapIdAndSenderId(Long swapId, Long senderId);
}
