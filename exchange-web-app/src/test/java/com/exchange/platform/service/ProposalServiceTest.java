package com.exchange.platform.service;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.ProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private UserService userService;

    @Mock
    private ListingService listingService;

    @InjectMocks
    private ProposalService proposalService;

    private User proposer;
    private User receiver;
    private Listing proposerListing;
    private Listing receiverListing;
    private Proposal testProposal;

    @BeforeEach
    void setUp() {
        proposer = User.builder()
                .email("proposer@test.com")
                .displayName("Proposer")
                .build();
        proposer.setId(1L);

        receiver = User.builder()
                .email("receiver@test.com")
                .displayName("Receiver")
                .build();
        receiver.setId(2L);

        proposerListing = Listing.builder()
                .user(proposer)
                .title("Proposer Card")
                .idolGroup("IVE")
                .memberName("Wonyoung")
                .status(Listing.ListingStatus.ACTIVE)
                .build();
        proposerListing.setId(1L);

        receiverListing = Listing.builder()
                .user(receiver)
                .title("Receiver Card")
                .idolGroup("NewJeans")
                .memberName("Minji")
                .status(Listing.ListingStatus.ACTIVE)
                .build();
        receiverListing.setId(2L);

        testProposal = Proposal.builder()
                .proposer(proposer)
                .receiver(receiver)
                .status(Proposal.ProposalStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .message("Let's exchange!")
                .build();
        testProposal.setId(1L);
    }

    @Test
    void createProposal_Success() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(proposer);
        when(listingService.getListingById(2L)).thenReturn(receiverListing);
        when(listingService.getListingById(1L)).thenReturn(proposerListing);
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> {
            Proposal p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act
        Proposal result = proposalService.createProposal(
                1L, List.of(1L), List.of(2L), "Exchange?"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProposer()).isEqualTo(proposer);
        assertThat(result.getReceiver()).isEqualTo(receiver);
        assertThat(result.getStatus()).isEqualTo(Proposal.ProposalStatus.PENDING);
        
        verify(proposalRepository).save(any(Proposal.class));
    }

    @Test
    void createProposal_NoReceiverListings_ThrowsException() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(proposer);

        // Act & Assert
        assertThatThrownBy(() -> proposalService.createProposal(
                1L, List.of(1L), List.of(), "Exchange?"
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void createProposal_SameUser_ThrowsException() {
        // Arrange
        receiverListing.setUser(proposer);
        when(userService.getUserById(1L)).thenReturn(proposer);
        when(listingService.getListingById(2L)).thenReturn(receiverListing);

        // Act & Assert
        assertThatThrownBy(() -> proposalService.createProposal(
                1L, null, List.of(2L), "Exchange?"
        )).isInstanceOf(BusinessRuleViolationException.class)
          .hasMessageContaining("yourself");
    }

    @Test
    void getProposalById_Success() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));

        // Act
        Proposal result = proposalService.getProposalById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        
        verify(proposalRepository).findById(1L);
    }

    @Test
    void getProposalById_NotFound_ThrowsException() {
        // Arrange
        when(proposalRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> proposalService.getProposalById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReceivedProposals_Success() {
        // Arrange
        when(proposalRepository.findByReceiverId(2L)).thenReturn(List.of(testProposal));

        // Act
        List<Proposal> result = proposalService.getReceivedProposals(2L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReceiver().getId()).isEqualTo(2L);
        
        verify(proposalRepository).findByReceiverId(2L);
    }

    @Test
    void getSentProposals_Success() {
        // Arrange
        when(proposalRepository.findByProposerId(1L)).thenReturn(List.of(testProposal));

        // Act
        List<Proposal> result = proposalService.getSentProposals(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProposer().getId()).isEqualTo(1L);
        
        verify(proposalRepository).findByProposerId(1L);
    }

    @Test
    void acceptProposal_Success() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));
        when(proposalRepository.save(any(Proposal.class))).thenReturn(testProposal);

        // Act
        Proposal result = proposalService.acceptProposal(1L, 2L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Proposal.ProposalStatus.ACCEPTED);
        verify(proposalRepository).save(testProposal);
    }

    @Test
    void acceptProposal_NotReceiver_ThrowsException() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));

        // Act & Assert
        assertThatThrownBy(() -> proposalService.acceptProposal(1L, 999L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void acceptProposal_NotPending_ThrowsException() {
        // Arrange
        testProposal.setStatus(Proposal.ProposalStatus.REJECTED);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));

        // Act & Assert
        assertThatThrownBy(() -> proposalService.acceptProposal(1L, 2L))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void rejectProposal_Success() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));
        when(proposalRepository.save(any(Proposal.class))).thenReturn(testProposal);

        // Act
        Proposal result = proposalService.rejectProposal(1L, 2L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Proposal.ProposalStatus.REJECTED);
        verify(proposalRepository).save(testProposal);
    }

    @Test
    void cancelProposal_Success() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));
        when(proposalRepository.save(any(Proposal.class))).thenReturn(testProposal);

        // Act
        proposalService.cancelProposal(1L, 1L);

        // Assert
        assertThat(testProposal.getStatus()).isEqualTo(Proposal.ProposalStatus.CANCELLED);
        verify(proposalRepository).save(testProposal);
    }

    @Test
    void cancelProposal_NotProposer_ThrowsException() {
        // Arrange
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(testProposal));

        // Act & Assert
        assertThatThrownBy(() -> proposalService.cancelProposal(1L, 999L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}
