package com.exchange.platform.repository;

import com.exchange.platform.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findBySwapId(Long swapId);

    List<Shipment> findBySenderId(Long senderId);

    List<Shipment> findByReceiverId(Long receiverId);

    @Query("SELECT s FROM Shipment s WHERE s.sender.id = :userId OR s.receiver.id = :userId")
    List<Shipment> findByUserId(@Param("userId") Long userId);
}