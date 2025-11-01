package com.exchange.platform.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M2 lists: /proposals/mine, /proposals/received, /listings/{id}/proposals with 1-based pagination and default sort")
    void proposal_lists_ok() throws Exception {
        // User A (owner)
        String emailA = "userA_" + UUID.randomUUID() + "@test.com";
        String pwA = "pwA123";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"A\"}", emailA, pwA)))
                .andExpect(status().isCreated());
        MvcResult loginA = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailA, pwA)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionA = (MockHttpSession) loginA.getRequest().getSession(false);

        // A creates a listing
        MvcResult createdListing = mockMvc.perform(post("/api/listings").session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Item\",\"description\":\"Desc\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = Long.valueOf(JsonPath.read(createdListing.getResponse().getContentAsString(), "$.id").toString());

        // User B (proposer)
        String emailB = "userB_" + UUID.randomUUID() + "@test.com";
        String pwB = "pwB123";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"B\"}", emailB, pwB)))
                .andExpect(status().isCreated());
        MvcResult loginB = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailB, pwB)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionB = (MockHttpSession) loginB.getRequest().getSession(false);

        // User C (proposer)
        String emailC = "userC_" + UUID.randomUUID() + "@test.com";
        String pwC = "pwC123";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"C\"}", emailC, pwC)))
                .andExpect(status().isCreated());
        MvcResult loginC = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailC, pwC)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionC = (MockHttpSession) loginC.getRequest().getSession(false);

        // B creates two proposals (later one should appear first by default sort desc createdAt)
        String body = String.format("{\"listingId\":%d,\"message\":\"offer\"}", listingId);
        mockMvc.perform(post("/api/proposals").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/proposals").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // C creates one proposal
        mockMvc.perform(post("/api/proposals").session(sessionC).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // /api/proposals/mine for B -> should return B's 2 proposals
        mockMvc.perform(get("/api/proposals/mine").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // /api/proposals/received for A (owner) -> should return all 3 proposals
        mockMvc.perform(get("/api/proposals/received").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // /api/listings/{id}/proposals -> 3 items total
        mockMvc.perform(get("/api/listings/" + listingId + "/proposals").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // Pagination: size=2 returns 2, page=2 returns remaining 1 (1-based page)
        mockMvc.perform(get("/api/listings/" + listingId + "/proposals?size=2").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/listings/" + listingId + "/proposals?page=2&size=2").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
