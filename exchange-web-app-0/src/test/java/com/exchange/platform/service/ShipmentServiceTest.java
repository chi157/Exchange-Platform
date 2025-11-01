package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private SwapService swapService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ShipmentService shipmentService;

    private User sender;
    private User receiver;
    private Swap testSwap;
    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .email("sender@test.com")
                .displayName("Sender")
                .build();
        sender.setId(1L);

        receiver = User.builder()
                .email("receiver@test.com")
                .displayName("Receiver")
                .build();
        receiver.setId(2L);

        testSwap = Swap.builder()
                .userA(sender)
                .userB(receiver)
                .status(Swap.SwapStatus.SHIPPING)
                .build();
        testSwap.setId(1L);

        testShipment = Shipment.builder()
                .swap(testSwap)
                .sender(sender)
                .receiver(receiver)
                .deliveryMethod(Shipment.DeliveryMethod.CVS_711)
                .trackingNumber("TRK123456")
                .build();
        testShipment.setId(1L);
    }

    @Test
    void createShipment_Success() {
        // Arrange
        when(swapService.getSwapById(1L)).thenReturn(testSwap);
        when(userService.getUserById(1L)).thenReturn(sender);
        when(userService.getUserById(2L)).thenReturn(receiver);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
            Shipment shipment = invocation.getArgument(0);
            shipment.setId(1L);
            return shipment;
        });

        // Act
        Shipment result = shipmentService.createShipment(1L, 1L, 2L, 
                Shipment.DeliveryMethod.CVS_711, "TRK123456");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getReceiver()).isEqualTo(receiver);
        assertThat(result.getDeliveryMethod()).isEqualTo(Shipment.DeliveryMethod.CVS_711);
        assertThat(result.getTrackingNumber()).isEqualTo("TRK123456");
        
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void getSwapShipments_Success() {
        // Arrange
        when(shipmentRepository.findBySwapId(1L)).thenReturn(List.of(testShipment));

        // Act
        List<Shipment> result = shipmentService.getSwapShipments(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testShipment);
        
        verify(shipmentRepository).findBySwapId(1L);
    }

    @Test
    void addShipmentEvent_Success() {
        // Arrange
        when(shipmentRepository.findById(1L)).thenReturn(java.util.Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment);

        // Act
        Shipment result = shipmentService.addShipmentEvent(1L, 
                "PICKED_UP", "Store 001", "Picked up from 7-11");

        // Assert
        assertThat(result).isNotNull();
        
        verify(shipmentRepository).save(testShipment);
    }

    @Test
    void addShipmentEvent_NotFound_ThrowsException() {
        // Arrange
        when(shipmentRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> shipmentService.addShipmentEvent(999L, 
                "PICKED_UP", "Store 001", "Event"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
