package com.exchange.platform;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.ProposalRepository;
import com.exchange.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * M2 ?��??�詢?��?測試：�??�出?�、�??�到?�、�? listing ?�詢
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ProposalQueryIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private ProposalRepository proposalRepository;

    private User userA, userB, userC;
    private Listing listingFromA, listingFromB;
    private MockHttpSession sessionA, sessionB, sessionC;

    @BeforeEach
    void setup() {
        // 建�?三�?使用??
        userA = userRepository.save(User.builder().email("a@example.com").passwordHash("pass").displayName("UserA").build());
        userB = userRepository.save(User.builder().email("b@example.com").passwordHash("pass").displayName("UserB").build());
        userC = userRepository.save(User.builder().email("c@example.com").passwordHash("pass").displayName("UserC").build());

        sessionA = new MockHttpSession();
        sessionA.setAttribute("userId", userA.getId());
        sessionB = new MockHttpSession();
        sessionB.setAttribute("userId", userB.getId());
        sessionC = new MockHttpSession();
        sessionC.setAttribute("userId", userC.getId());

        // A ??B ?��?一件�???
        listingFromA = listingRepository.save(Listing.builder()
                .userId(userA.getId())
                .cardName("A's Card")
                .artistName("Artist A")
                .groupName("Group A")
                .cardSource(Listing.CardSource.OFFICIAL)
                .conditionRating(9)
                .hasProtection(true)
                .description("desc")
                .status(Listing.Status.AVAILABLE)
                .build());
        listingFromB = listingRepository.save(Listing.builder()
                .userId(userB.getId())
                .cardName("B's Card")
                .artistName("Artist B")
                .groupName("Group B")
                .cardSource(Listing.CardSource.OFFICIAL)
                .conditionRating(9)
                .hasProtection(true)
                .description("desc")
                .status(Listing.Status.AVAILABLE)
                .build());
    }

    @Test
    void testListMine() throws Exception {
        // B �?A ?�物?��??��??��?�?
        Proposal p1 = Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverId(listingFromA.getUserId())
                .message("B proposes to A")
                .status(Proposal.Status.PENDING)
                .build();
        proposalRepository.save(p1);

        // C �?A ?�物?��??�出一?��?�?
        Proposal p2 = Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverId(listingFromA.getUserId())
                .message("C proposes to A")
                .status(Proposal.Status.PENDING)
                .build();
        proposalRepository.save(p2);

        // B ?�詢?�己?�出?��?案�??�該?��??��???
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].proposerId").value(userB.getId()))
                .andExpect(jsonPath("$[0].message").value("B proposes to A"));

        // C ?�詢?�己?�出?��?案�??�該?��??��???
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].proposerId").value(userC.getId()))
                .andExpect(jsonPath("$[0].message").value("C proposes to A"));

        // A ?�詢?�己?�出?��?案�??�該?�空
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testListReceived() throws Exception {
        // B ??C �?A ?�物?��??��?�?
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverId(listingFromA.getUserId())
                .message("B to A")
                .status(Proposal.Status.PENDING)
                .build());
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverId(listingFromA.getUserId())
                .message("C to A")
                .status(Proposal.Status.PENDING)
                .build());

        // A �?B ?�物?��??��??��?�?
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromB.getId())
                .proposerId(userA.getId())
                .receiverId(listingFromB.getUserId())
                .message("A to B")
                .status(Proposal.Status.PENDING)
                .build());

        mvc.perform(get("/api/proposals/received")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("B to A", "C to A")));

        // B ?�詢?�到?��?案�??�該?��?一??
        mvc.perform(get("/api/proposals/received")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message").value("A to B"));
    }

    @Test
    void testListByListing() throws Exception {
        // B ??C �?A ?�物?��??��?�?
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverId(listingFromA.getUserId())
                .message("B to A")
                .status(Proposal.Status.PENDING)
                .build());
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverId(listingFromA.getUserId())
                .message("C to A")
                .status(Proposal.Status.PENDING)
                .build());

        // �?B ?�物?��?一??
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromB.getId())
                .proposerId(userA.getId())
                .receiverId(listingFromB.getUserId())
                .message("A to B")
                .status(Proposal.Status.PENDING)
                .build());

        // ?�詢 listingFromA ?��??��?�?
        mvc.perform(get("/api/proposals/by-listing/" + listingFromA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].listingId", everyItem(is(listingFromA.getId().intValue()))))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("B to A", "C to A")));

        // ?�詢 listingFromB ?��??��?�?
        mvc.perform(get("/api/proposals/by-listing/" + listingFromB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message").value("A to B"));
    }

    @Test
    void testPagination() throws Exception {
        // B �?A ?�物?��???5 ?��?�?
        for (int i = 1; i <= 5; i++) {
            proposalRepository.save(Proposal.builder()
                    .listingId(listingFromA.getId())
                    .proposerId(userB.getId())
                    .receiverId(listingFromA.getUserId())
                    .message("Proposal " + i)
                    .status(Proposal.Status.PENDING)
                    .build());
        }

        // �?1 ?��?每�? 2 �?
        mvc.perform(get("/api/proposals/mine?page=1&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // �?2 ?��?每�? 2 �?
        mvc.perform(get("/api/proposals/mine?page=2&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // �?3 ?��?每�? 2 �?
        mvc.perform(get("/api/proposals/mine?page=3&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testUnauthorized() throws Exception {
        // ?�登?��??��???401
        mvc.perform(get("/api/proposals/mine"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/proposals/received"))
                .andExpect(status().isUnauthorized());
    }
}
