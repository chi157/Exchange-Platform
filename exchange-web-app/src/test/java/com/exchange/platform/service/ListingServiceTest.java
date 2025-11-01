package com.exchange.platform.service;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.ResourceNotFoundException;
import com.exchange.platform.exception.UnauthorizedAccessException;
import com.exchange.platform.exception.ValidationException;
import com.exchange.platform.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ListingService listingService;

    private User testUser;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .build();
        testUser.setId(1L);

        testListing = Listing.builder()
                .user(testUser)
                .title("IVE Liz Photocard")
                .description("Official photocard from Love Dive album")
                .idolGroup("IVE")
                .memberName("Liz")
                .album("Love Dive")
                .era("2022")
                .version("Limited")
                .cardCode("LD-LIZ-01")
                .isOfficial(true)
                .condition(Listing.CardCondition.S)
                .status(Listing.ListingStatus.ACTIVE)
                .build();
        testListing.setId(1L);
    }

    @Test
    void createListing_Success() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            listing.setId(1L);
            return listing;
        });

        // Act
        Listing result = listingService.createListing(
                1L, "IVE Liz PC", "Official PC",
                "IVE", "Liz", "Love Dive",
                "2022", "Limited", "LD-LIZ-01",
                true, Listing.CardCondition.S, List.of("photo1.jpg")
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("IVE Liz PC");
        assertThat(result.getIdolGroup()).isEqualTo("IVE");
        assertThat(result.getMemberName()).isEqualTo("Liz");
        assertThat(result.getStatus()).isEqualTo(Listing.ListingStatus.ACTIVE);
        
        verify(userService).getUserById(1L);
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void getListingById_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));

        // Act
        Listing result = listingService.getListingById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("IVE Liz Photocard");
        
        verify(listingRepository).findById(1L);
    }

    @Test
    void getListingById_NotFound_ThrowsException() {
        // Arrange
        when(listingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> listingService.getListingById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(listingRepository).findById(999L);
    }

    @Test
    void searchListings_WithKeyword() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> page = new PageImpl<>(List.of(testListing));
        when(listingRepository.searchListings("IVE", pageable)).thenReturn(page);

        // Act
        Page<Listing> result = listingService.searchListings("IVE", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIdolGroup()).isEqualTo("IVE");
        
        verify(listingRepository).searchListings("IVE", pageable);
    }

    @Test
    void searchByIdolGroup_Success() {
        // Arrange
        when(listingRepository.findByIdolGroupAndStatus("IVE", Listing.ListingStatus.ACTIVE))
                .thenReturn(List.of(testListing));

        // Act
        List<Listing> result = listingService.searchByIdolGroup("IVE");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIdolGroup()).isEqualTo("IVE");
        
        verify(listingRepository).findByIdolGroupAndStatus("IVE", Listing.ListingStatus.ACTIVE);
    }

    @Test
    void getUserListings_Success() {
        // Arrange
        when(listingRepository.findByUserId(1L)).thenReturn(List.of(testListing));

        // Act
        List<Listing> result = listingService.getUserListings(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
        
        verify(listingRepository).findByUserId(1L);
    }

    @Test
    void updateListing_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        Listing result = listingService.updateListing(
                1L, 1L, "Updated Title", "Updated Desc",
                "New Album", "2023", "Special", Listing.CardCondition.A
        );

        // Assert
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(listingRepository).save(testListing);
    }

    @Test
    void updateListing_UnauthorizedUser_ThrowsException() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        assertThatThrownBy(() -> listingService.updateListing(
                1L, 999L, "Title", null, null, null, null, null
        )).isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void updateListing_NotActiveStatus_ThrowsException() {
        // Arrange
        testListing.setStatus(Listing.ListingStatus.TRADED);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        assertThatThrownBy(() -> listingService.updateListing(
                1L, 1L, "Title", null, null, null, null, null
        )).isInstanceOf(ValidationException.class);
    }

    @Test
    void lockListing_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        listingService.lockListing(1L, 100L);

        // Assert
        assertThat(testListing.getStatus()).isEqualTo(Listing.ListingStatus.LOCKED);
        assertThat(testListing.getLockedByProposalId()).isEqualTo(100L);
        verify(listingRepository).save(testListing);
    }

    @Test
    void unlockListing_Success() {
        // Arrange
        testListing.lock(100L);
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        listingService.unlockListing(1L);

        // Assert
        assertThat(testListing.getStatus()).isEqualTo(Listing.ListingStatus.ACTIVE);
        assertThat(testListing.getLockedByProposalId()).isNull();
        verify(listingRepository).save(testListing);
    }

    @Test
    void markAsTraded_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        listingService.markAsTraded(1L);

        // Assert
        assertThat(testListing.getStatus()).isEqualTo(Listing.ListingStatus.TRADED);
        verify(listingRepository).save(testListing);
    }

    @Test
    void deleteListing_Success() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class))).thenReturn(testListing);

        // Act
        listingService.deleteListing(1L, 1L);

        // Assert
        assertThat(testListing.getStatus()).isEqualTo(Listing.ListingStatus.DELETED);
        verify(listingRepository).save(testListing);
    }

    @Test
    void deleteListing_UnauthorizedUser_ThrowsException() {
        // Arrange
        when(listingRepository.findById(1L)).thenReturn(Optional.of(testListing));

        // Act & Assert
        assertThatThrownBy(() -> listingService.deleteListing(1L, 999L))
                .isInstanceOf(UnauthorizedAccessException.class);
    }
}
