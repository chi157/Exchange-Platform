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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProposalFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Proposal flow: B proposes on A's listing; A accepts and rejects others; unauthorized/forbidden/notfound handled")
    void proposal_flow_ok() throws Exception {
        // Register A and login
        String emailA = "userA_" + UUID.randomUUID() + "@test.com";
        String pwA = "pwA123";
        String regA = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"A\"}", emailA, pwA);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(regA))
                .andExpect(status().isCreated());
        MvcResult loginAResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailA, pwA)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionA = (MockHttpSession) loginAResult.getRequest().getSession(false);

        // A creates a listing
        MvcResult createListing = mockMvc.perform(post("/api/listings")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Item\",\"description\":\"Desc\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = Long.valueOf(JsonPath.read(createListing.getResponse().getContentAsString(), "$.id").toString());

        // Register B and login
        String emailB = "userB_" + UUID.randomUUID() + "@test.com";
        String pwB = "pwB123";
        String regB = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"B\"}", emailB, pwB);
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(regB))
                .andExpect(status().isCreated());
        MvcResult loginBResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailB, pwB)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionB = (MockHttpSession) loginBResult.getRequest().getSession(false);

        // B creates a proposal on A's listing
        String proposalBody = String.format("{\"listingId\":%d,\"message\":\"offer\"}", listingId);
        MvcResult createProposal = mockMvc.perform(post("/api/proposals")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposalBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        Long proposalId = Long.valueOf(JsonPath.read(createProposal.getResponse().getContentAsString(), "$.id").toString());

        // B tries to accept -> 403
        mockMvc.perform(post("/api/proposals/" + proposalId + "/accept").session(sessionB))
                .andExpect(status().isForbidden());

        // A accepts -> 200
        mockMvc.perform(post("/api/proposals/" + proposalId + "/accept").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Another proposal to reject
        MvcResult createProposal2 = mockMvc.perform(post("/api/proposals")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposalBody))
                .andExpect(status().isCreated()).andReturn();
        Long proposalId2 = Long.valueOf(JsonPath.read(createProposal2.getResponse().getContentAsString(), "$.id").toString());

        // A rejects -> 200
        mockMvc.perform(post("/api/proposals/" + proposalId2 + "/reject").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Unauthorized create -> 401
        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposalBody))
                .andExpect(status().isUnauthorized());

        // Not found accept -> 404
        mockMvc.perform(post("/api/proposals/999999/accept").session(sessionA))
                .andExpect(status().isNotFound());
    }
}
