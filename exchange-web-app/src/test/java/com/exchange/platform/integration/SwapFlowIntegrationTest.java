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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwapFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M3: Accept creates swap and locks listing; repeated accept -> 409; swaps/mine shows record")
    void swap_creation_and_locking() throws Exception {
        // A register/login
        String emailA = "a_" + UUID.randomUUID() + "@test.com";
        String pwA = "pwA";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"A\"}", emailA, pwA)))
                .andExpect(status().isCreated());
        MvcResult loginA = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailA, pwA)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionA = (MockHttpSession) loginA.getRequest().getSession(false);

        // A creates listing
        MvcResult createdListing = mockMvc.perform(post("/api/listings").session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Item\",\"description\":\"D\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long listingId = Long.valueOf(JsonPath.read(createdListing.getResponse().getContentAsString(), "$.id").toString());

        // B register/login
        String emailB = "b_" + UUID.randomUUID() + "@test.com";
        String pwB = "pwB";
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"B\"}", emailB, pwB)))
                .andExpect(status().isCreated());
        MvcResult loginB = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", emailB, pwB)))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession sessionB = (MockHttpSession) loginB.getRequest().getSession(false);

        // B creates two proposals
        String body = String.format("{\"listingId\":%d,\"message\":\"offer\"}", listingId);
        MvcResult p1 = mockMvc.perform(post("/api/proposals").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Long proposalId1 = Long.valueOf(JsonPath.read(p1.getResponse().getContentAsString(), "$.id").toString());
        MvcResult p2 = mockMvc.perform(post("/api/proposals").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Long proposalId2 = Long.valueOf(JsonPath.read(p2.getResponse().getContentAsString(), "$.id").toString());

        // A accepts first -> 200
        mockMvc.perform(post("/api/proposals/" + proposalId1 + "/accept").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // A accepts second -> 409 conflict
        mockMvc.perform(post("/api/proposals/" + proposalId2 + "/accept").session(sessionA))
                .andExpect(status().isConflict());

        // Swaps mine for A and B should have at least one record
        mockMvc.perform(get("/api/swaps/mine").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
        mockMvc.perform(get("/api/swaps/mine").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }
}
