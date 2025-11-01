package com.exchange.platform.controller;

import com.exchange.platform.dto.CreateShipmentEventRequest;
import com.exchange.platform.dto.ShipmentDTO;
import com.exchange.platform.dto.UpsertShipmentRequest;
import com.exchange.platform.service.ShipmentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    // 建立或更新我在該 Swap 的出貨資料
    @PostMapping("/swaps/{id}/shipments/my")
    public ResponseEntity<ShipmentDTO> upsertMyShipment(@PathVariable("id") Long swapId,
                                                        @Valid @RequestBody UpsertShipmentRequest req,
                                                        HttpSession session) {
        ShipmentDTO dto = shipmentService.upsertMyShipment(swapId, req, session);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    // 在指定的 Shipment 新增事件（僅限 sender 本人）
    @PostMapping("/shipments/{id}/events")
    public ResponseEntity<Void> addEvent(@PathVariable("id") Long shipmentId,
                                         @Valid @RequestBody CreateShipmentEventRequest req,
                                         HttpSession session) {
        shipmentService.addEvent(shipmentId, req, session);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ExceptionHandler(ShipmentService.UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

    @ExceptionHandler(ShipmentService.ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

    @ExceptionHandler(ShipmentService.NotFoundException.class)
    public ResponseEntity<Void> handleNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }

    @ExceptionHandler(ShipmentService.BadRequestException.class)
    public ResponseEntity<Void> handleBadRequest() { return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); }
}
