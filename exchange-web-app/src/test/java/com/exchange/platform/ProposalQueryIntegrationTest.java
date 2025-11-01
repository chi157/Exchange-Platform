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
 * M2 提案查詢整合測試：我提出的、我收到的、依 listing 查詢
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
        // 建立三位使用者
        userA = userRepository.save(User.builder().email("a@example.com").passwordHash("pass").displayName("UserA").build());
        userB = userRepository.save(User.builder().email("b@example.com").passwordHash("pass").displayName("UserB").build());
        userC = userRepository.save(User.builder().email("c@example.com").passwordHash("pass").displayName("UserC").build());

        sessionA = new MockHttpSession();
        sessionA.setAttribute("userId", userA.getId());
        sessionB = new MockHttpSession();
        sessionB.setAttribute("userId", userB.getId());
        sessionC = new MockHttpSession();
        sessionC.setAttribute("userId", userC.getId());

        // A 與 B 各有一件刊登
        listingFromA = listingRepository.save(Listing.builder()
                .ownerId(userA.getId())
                .title("A's item")
                .description("desc")
                .status(Listing.Status.ACTIVE)
                .build());
        listingFromB = listingRepository.save(Listing.builder()
                .ownerId(userB.getId())
                .title("B's item")
                .description("desc")
                .status(Listing.Status.ACTIVE)
                .build());
    }

    @Test
    void testListMine() throws Exception {
        // B 對 A 的物品提出一個提案
        Proposal p1 = Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("B proposes to A")
                .status(Proposal.Status.PENDING)
                .build();
        proposalRepository.save(p1);

        // C 對 A 的物品也提出一個提案
        Proposal p2 = Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("C proposes to A")
                .status(Proposal.Status.PENDING)
                .build();
        proposalRepository.save(p2);

        // B 查詢自己提出的提案，應該只看到一個
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].proposerId").value(userB.getId()))
                .andExpect(jsonPath("$[0].message").value("B proposes to A"));

        // C 查詢自己提出的提案，應該只看到一個
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].proposerId").value(userC.getId()))
                .andExpect(jsonPath("$[0].message").value("C proposes to A"));

        // A 查詢自己提出的提案，應該為空
        mvc.perform(get("/api/proposals/mine")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testListReceived() throws Exception {
        // B 與 C 對 A 的物品提出提案
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("B to A")
                .status(Proposal.Status.PENDING)
                .build());
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("C to A")
                .status(Proposal.Status.PENDING)
                .build());

        // A 對 B 的物品提出一個提案
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromB.getId())
                .proposerId(userA.getId())
                .receiverIdLegacy(listingFromB.getOwnerId())
                .message("A to B")
                .status(Proposal.Status.PENDING)
                .build());

        mvc.perform(get("/api/proposals/received")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("B to A", "C to A")));

        // B 查詢收到的提案，應該只有一個
        mvc.perform(get("/api/proposals/received")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message").value("A to B"));
    }

    @Test
    void testListByListing() throws Exception {
        // B 與 C 對 A 的物品提出提案
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userB.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("B to A")
                .status(Proposal.Status.PENDING)
                .build());
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromA.getId())
                .proposerId(userC.getId())
                .receiverIdLegacy(listingFromA.getOwnerId())
                .message("C to A")
                .status(Proposal.Status.PENDING)
                .build());

        // 對 B 的物品提一個
        proposalRepository.save(Proposal.builder()
                .listingId(listingFromB.getId())
                .proposerId(userA.getId())
                .receiverIdLegacy(listingFromB.getOwnerId())
                .message("A to B")
                .status(Proposal.Status.PENDING)
                .build());

        // 查詢 listingFromA 的所有提案
        mvc.perform(get("/api/proposals/by-listing/" + listingFromA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].listingId", everyItem(is(listingFromA.getId().intValue()))))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("B to A", "C to A")));

        // 查詢 listingFromB 的所有提案
        mvc.perform(get("/api/proposals/by-listing/" + listingFromB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message").value("A to B"));
    }

    @Test
    void testPagination() throws Exception {
        // B 對 A 的物品提出 5 個提案
        for (int i = 1; i <= 5; i++) {
            proposalRepository.save(Proposal.builder()
                    .listingId(listingFromA.getId())
                    .proposerId(userB.getId())
                    .receiverIdLegacy(listingFromA.getOwnerId())
                    .message("Proposal " + i)
                    .status(Proposal.Status.PENDING)
                    .build());
        }

        // 第 1 頁，每頁 2 筆
        mvc.perform(get("/api/proposals/mine?page=1&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 第 2 頁，每頁 2 筆
        mvc.perform(get("/api/proposals/mine?page=2&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // 第 3 頁，每頁 2 筆
        mvc.perform(get("/api/proposals/mine?page=3&size=2")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testUnauthorized() throws Exception {
        // 未登入存取應回 401
        mvc.perform(get("/api/proposals/mine"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/proposals/received"))
                .andExpect(status().isUnauthorized());
    }
}
