package com.exchange.platform.service;

import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.SwapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwapServiceTest {

    @Mock
    private SwapRepository swapRepository;

    @Mock
    private ProposalService proposalService;

    @Mock
    private ListingService listingService;

    @InjectMocks
    private SwapService swapService;

    private User userA;
    private User userB;
    private Proposal acceptedProposal;
    private Swap testSwap;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .email("userA@test.com")
                .displayName("User A")
                .build();
        userA.setId(1L);

        userB = User.builder()
                .email("userB@test.com")
                .displayName("User B")
                .build();
        userB.setId(2L);

        acceptedProposal = Proposal.builder()
                .proposer(userA)
                .receiver(userB)
                .status(Proposal.ProposalStatus.ACCEPTED)
                .build();
        acceptedProposal.setId(1L);

        testSwap = Swap.builder()
                .proposal(acceptedProposal)
                .userA(userA)
                .userB(userB)
                .status(Swap.SwapStatus.SHIPPING)
                .receivedAConfirmed(false)
                .receivedBConfirmed(false)
                .build();
        testSwap.setId(1L);
    }

    @Test
    void createSwap_Success() {
        // Arrange
        when(proposalService.getProposalById(1L)).thenReturn(acceptedProposal);
        when(swapRepository.save(any(Swap.class))).thenAnswer(invocation -> {
            Swap swap = invocation.getArgument(0);
            swap.setId(1L);
            return swap;
        });

        // Act
        Swap result = swapService.createSwap(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserA()).isEqualTo(userA);
        assertThat(result.getUserB()).isEqualTo(userB);
        assertThat(result.getStatus()).isEqualTo(Swap.SwapStatus.SHIPPING);
        
        verify(swapRepository).save(any(Swap.class));
    }

    @Test
    void createSwap_ProposalNotAccepted_ThrowsException() {
        // Arrange
        acceptedProposal.setStatus(Proposal.ProposalStatus.PENDING);
        when(proposalService.getProposalById(1L)).thenReturn(acceptedProposal);

        // Act & Assert
        assertThatThrownBy(() -> swapService.createSwap(1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("accepted");
    }

    @Test
    void getSwapById_Success() {
        // Arrange
        when(swapRepository.findById(1L)).thenReturn(Optional.of(testSwap));

        // Act
        Swap result = swapService.getSwapById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(swapRepository).findById(1L);
    }

    @Test
    void getSwapById_NotFound_ThrowsException() {
        // Arrange
        when(swapRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> swapService.getSwapById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserSwaps_Success() {
        // Arrange
        when(swapRepository.findByUserAIdOrUserBId(1L, 1L)).thenReturn(List.of(testSwap));

        // Act
        List<Swap> result = swapService.getUserSwaps(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testSwap);
        
        verify(swapRepository).findByUserAIdOrUserBId(1L, 1L);
    }

    @Test
    void confirmReceived_ByUserA_Success() {
        // Arrange
        when(swapRepository.findById(1L)).thenReturn(Optional.of(testSwap));
        when(swapRepository.save(any(Swap.class))).thenReturn(testSwap);

        // Act
        Swap result = swapService.confirmReceived(1L, 1L);

        // Assert
        assertThat(result.getReceivedAConfirmed()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Swap.SwapStatus.SHIPPING);
        
        verify(swapRepository).save(testSwap);
    }

    @Test
    void confirmReceived_ByUserB_Success() {
        // Arrange
        when(swapRepository.findById(1L)).thenReturn(Optional.of(testSwap));
        when(swapRepository.save(any(Swap.class))).thenReturn(testSwap);

        // Act
        Swap result = swapService.confirmReceived(1L, 2L);

        // Assert
        assertThat(result.getReceivedBConfirmed()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Swap.SwapStatus.SHIPPING);
        
        verify(swapRepository).save(testSwap);
    }

    @Test
    void confirmReceived_BothConfirmed_StatusCompleted() {
        // Arrange
        testSwap.setReceivedAConfirmed(true);
        when(swapRepository.findById(1L)).thenReturn(Optional.of(testSwap));
        when(swapRepository.save(any(Swap.class))).thenReturn(testSwap);

        // Act
        Swap result = swapService.confirmReceived(1L, 2L);

        // Assert
        assertThat(result.getReceivedAConfirmed()).isTrue();
        assertThat(result.getReceivedBConfirmed()).isTrue();
        assertThat(result.getStatus()).isEqualTo(Swap.SwapStatus.COMPLETED);
        
        verify(swapRepository).save(testSwap);
    }

    @Test
    void confirmReceived_NotPartOfSwap_ThrowsException() {
        // Arrange
        when(swapRepository.findById(1L)).thenReturn(Optional.of(testSwap));

        // Act & Assert
        assertThatThrownBy(() -> swapService.confirmReceived(1L, 999L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("not part");
    }
}
