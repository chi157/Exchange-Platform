package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.DisputeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private SwapService swapService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DisputeService disputeService;

    private User complainant;
    private Swap testSwap;
    private Dispute testDispute;
    private User admin;

    @BeforeEach
    void setUp() {
        complainant = User.builder()
                .email("user@test.com")
                .displayName("User")
                .build();
        complainant.setId(1L);

        User otherUser = User.builder()
                .email("other@test.com")
                .displayName("Other")
                .build();
        otherUser.setId(2L);

        testSwap = Swap.builder()
                .userA(complainant)
                .userB(otherUser)
                .status(Swap.SwapStatus.SHIPPING)
                .build();
        testSwap.setId(1L);

        admin = User.builder()
                .email("admin@test.com")
                .displayName("Admin")
                .build();
        admin.setId(99L);

        testDispute = Dispute.builder()
                .swap(testSwap)
                .complainant(complainant)
                .reason("Item not as described")
                .status(Dispute.DisputeStatus.OPEN)
                .build();
        testDispute.setId(1L);
    }

    @Test
    void createDispute_Success() {
        // Arrange
        List<String> evidence = new ArrayList<>();
        evidence.add("photo1.jpg");
        
        when(swapService.getSwapById(1L)).thenReturn(testSwap);
        when(userService.getUserById(1L)).thenReturn(complainant);
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute dispute = invocation.getArgument(0);
            dispute.setId(1L);
            return dispute;
        });

        // Act
        Dispute result = disputeService.createDispute(1L, 1L, "Problem", "Description", evidence);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getComplainant()).isEqualTo(complainant);
        assertThat(result.getReason()).isEqualTo("Problem");
        assertThat(result.getStatus()).isEqualTo(Dispute.DisputeStatus.OPEN);
        
        verify(disputeRepository).save(any(Dispute.class));
    }

    @Test
    void createDispute_NotPartOfSwap_ThrowsException() {
        // Arrange
        when(swapService.getSwapById(1L)).thenReturn(testSwap);
        User outsider = User.builder().build();
        outsider.setId(999L);
        when(userService.getUserById(999L)).thenReturn(outsider);

        // Act & Assert
        assertThatThrownBy(() -> disputeService.createDispute(1L, 999L, "Problem", "Desc", new ArrayList<>()))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("not part");
    }

    @Test
    void getDisputeById_Success() {
        // Arrange
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(testDispute));

        // Act
        Dispute result = disputeService.getDisputeById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(disputeRepository).findById(1L);
    }

    @Test
    void getDisputeById_NotFound_ThrowsException() {
        // Arrange
        when(disputeRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> disputeService.getDisputeById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOpenDisputes_Success() {
        // Arrange
        when(disputeRepository.findByStatus(Dispute.DisputeStatus.OPEN))
                .thenReturn(List.of(testDispute));

        // Act
        List<Dispute> result = disputeService.getOpenDisputes();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testDispute);
        
        verify(disputeRepository).findByStatus(Dispute.DisputeStatus.OPEN);
    }

    @Test
    void assignToAdmin_Success() {
        // Arrange
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(testDispute));
        when(userService.getUserById(99L)).thenReturn(admin);
        when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

        // Act
        Dispute result = disputeService.assignToAdmin(1L, 99L);

        // Assert
        assertThat(result.getAdmin()).isEqualTo(admin);
        assertThat(result.getStatus()).isEqualTo(Dispute.DisputeStatus.IN_REVIEW);
        
        verify(disputeRepository).save(testDispute);
    }

    @Test
    void resolveDispute_Success() {
        // Arrange
        when(disputeRepository.findById(1L)).thenReturn(Optional.of(testDispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(testDispute);

        // Act
        Dispute result = disputeService.resolveDispute(1L, Dispute.DisputeStatus.RESOLVED_FAVOR_A, "Refund issued");

        // Assert
        assertThat(result.getStatus()).isEqualTo(Dispute.DisputeStatus.RESOLVED_FAVOR_A);
        assertThat(result.getAdminResolution()).isEqualTo("Refund issued");
        
        verify(disputeRepository).save(testDispute);
    }
}
