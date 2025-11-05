package com.exchange.platform.integration;

import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.UserRepository;
import com.exchange.platform.service.ListingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ListingPaginationOrderIntegrationTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingService listingService;

    @Test
    @Transactional
    @DisplayName("Completed listings only surface after all non-completed pages")
    void completed_listings_push_to_trailing_pages() {
        String marker = "order-" + UUID.randomUUID();

        User owner = userRepository.save(User.builder()
                .email(marker + "@mail.test")
                .passwordHash("hash")
                .displayName("Owner")
                .build());

        // create three active/locked listings
        listingRepository.save(Listing.builder()
                .cardName(marker + "-a1").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("active")
                .userId(owner.getId())
                .status(Listing.Status.AVAILABLE)
                .build());
        listingRepository.save(Listing.builder()
                .cardName(marker + "-a2").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("locked")
                .userId(owner.getId())
                .status(Listing.Status.LOCKED)
                .build());
        listingRepository.save(Listing.builder()
                .cardName(marker + "-a3").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("active")
                .userId(owner.getId())
                .status(Listing.Status.AVAILABLE)
                .build());

        // create two completed listings
        listingRepository.save(Listing.builder()
                .cardName(marker + "-c1").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("completed")
                .userId(owner.getId())
                .status(Listing.Status.COMPLETED)
                .build());
        listingRepository.save(Listing.builder()
                .cardName(marker + "-c2").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("completed")
                .userId(owner.getId())
                .status(Listing.Status.COMPLETED)
                .build());

        // page size 2 ensures multiple pages
        // page size 2 ensures multiple pages
        var page1 = listingService.listPage(1, 2, marker, "createdAt,ASC", null);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getContent()).allMatch(dto -> dto.getStatus() != Listing.Status.COMPLETED);

        var page2 = listingService.listPage(2, 2, marker, "createdAt,ASC", null);
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent()).allMatch(dto -> dto.getStatus() != Listing.Status.COMPLETED);

        var page3 = listingService.listPage(3, 2, marker, "createdAt,ASC", null);
        assertThat(page3.getContent()).hasSize(2);
        assertThat(page3.getContent()).allMatch(dto -> dto.getStatus() == Listing.Status.COMPLETED);
    }

    @Test
    @Transactional
    @DisplayName("My listings view mirrors trailing completed ordering")
    void my_listings_page_includes_completed_at_tail() {
        String marker = "mine-order-" + UUID.randomUUID();

        User owner = userRepository.save(User.builder()
                .email(marker + "@mail.test")
                .passwordHash("hash")
                .displayName("Owner")
                .build());

        listingRepository.save(Listing.builder()
                .cardName(marker + "-mine-a1").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("active")
                .userId(owner.getId())
                .status(Listing.Status.AVAILABLE)
                .build());
        listingRepository.save(Listing.builder()
                .cardName(marker + "-mine-a2").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("locked")
                .userId(owner.getId())
                .status(Listing.Status.LOCKED)
                .build());
        listingRepository.save(Listing.builder()
                .cardName(marker + "-mine-c1").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("completed")
                .userId(owner.getId())
                .status(Listing.Status.COMPLETED)
                .build());

        var page1 = listingService.myListingsPage(owner.getId(), 1, 2, marker, "createdAt,ASC");
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getContent())
                .extracting(ListingDTO::getStatus)
                .doesNotContain(Listing.Status.COMPLETED);

        var page2 = listingService.myListingsPage(owner.getId(), 2, 2, marker, "createdAt,ASC");
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent())
                .extracting(ListingDTO::getStatus)
                .containsOnly(Listing.Status.COMPLETED);
    }
    
    @Test
    @Transactional
    @DisplayName("Listing with card properties are correctly stored and retrieved")
    void card_properties_correctly_handled() {
        String marker = "card-" + UUID.randomUUID();

        User owner = userRepository.save(User.builder()
                .email(marker + "@mail.test")
                .passwordHash("hash")
                .displayName("CardOwner")
                .build());

        // ?µÂª∫Â∏∂Ê?ÂÆåÊï¥?°Á?Â±¨ÊÄßÁ??äÁôª
        listingRepository.save(Listing.builder()
                .cardName(marker + "-jimin-proof").artistName("Artist").groupName("Group").cardSource(Listing.CardSource.OFFICIAL).conditionRating(9).hasProtection(true)
                .description("BTS Jimin Proof Â∞èÂç°")
                .userId(owner.getId())
                .status(Listing.Status.AVAILABLE)
                .cardName("Jimin - Proof")
                .groupName("BTS")
                .artistName("Jimin")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(9)
                .hasProtection(true)
                .remarks("?∂Ë??ÅÔ??ÄÊ≥ÅËâØÂ•?)
                .imagePaths("[\"image1.jpg\",\"image2.jpg\"]")
                .build());

        // ?èÈ??çÂ??ñÂ??äÁôª
        var result = listingService.listPage(1, 10, marker, "createdAt,ASC", null);
        
        assertThat(result.getContent()).hasSize(1);
        
        var dto = result.getContent().get(0);
        assertThat(dto.getCardName()).isEqualTo("Jimin - Proof");
        assertThat(dto.getGroupName()).isEqualTo("BTS");
        assertThat(dto.getArtistName()).isEqualTo("Jimin");
        assertThat(dto.getCardSource()).isEqualTo(Listing.CardSource.ALBUM);
        assertThat(dto.getConditionRating()).isEqualTo(9);
        assertThat(dto.getHasProtection()).isTrue();
        assertThat(dto.getRemarks()).isEqualTo("?∂Ë??ÅÔ??ÄÊ≥ÅËâØÂ•?);
        assertThat(dto.getImageUrls()).containsExactly("/images/image1.jpg", "/images/image2.jpg");
    }
}
