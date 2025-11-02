package com.exchange.platform.integration;

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
                .title(marker + "-a1")
                .description("active")
                .ownerId(owner.getId())
                .status(Listing.Status.ACTIVE)
                .build());
        listingRepository.save(Listing.builder()
                .title(marker + "-a2")
                .description("locked")
                .ownerId(owner.getId())
                .status(Listing.Status.LOCKED)
                .build());
        listingRepository.save(Listing.builder()
                .title(marker + "-a3")
                .description("active")
                .ownerId(owner.getId())
                .status(Listing.Status.ACTIVE)
                .build());

        // create two completed listings
        listingRepository.save(Listing.builder()
                .title(marker + "-c1")
                .description("completed")
                .ownerId(owner.getId())
                .status(Listing.Status.COMPLETED)
                .build());
        listingRepository.save(Listing.builder()
                .title(marker + "-c2")
                .description("completed")
                .ownerId(owner.getId())
                .status(Listing.Status.COMPLETED)
                .build());

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
}
