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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryConfirmationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("M5: Both parties confirm received -> swap completes; idempotent confirms")
    void delivery_confirmation_flow() throws Exception {
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

        // B creates proposal
        String body = String.format("{\"listingId\":%d,\"message\":\"offer\"}", listingId);
        MvcResult p1 = mockMvc.perform(post("/api/proposals").session(sessionB).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Long proposalId = Long.valueOf(JsonPath.read(p1.getResponse().getContentAsString(), "$.id").toString());

        // A accepts -> creates swap
        mockMvc.perform(post("/api/proposals/" + proposalId + "/accept").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Get swap id from swaps/mine (A's view)
        MvcResult swapsMine = mockMvc.perform(get("/api/swaps/mine").session(sessionA))
                .andExpect(status().isOk()).andReturn();
        Long swapId = Long.valueOf(JsonPath.read(swapsMine.getResponse().getContentAsString(), "$[0].id").toString());

        // First confirm by A -> still IN_PROGRESS, aConfirmedAt set
        mockMvc.perform(post("/api/swaps/" + swapId + "/confirm-received").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.aConfirmedAt").exists());

        // Idempotent repeat by A -> stays IN_PROGRESS, timestamps remain
        mockMvc.perform(post("/api/swaps/" + swapId + "/confirm-received").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Second confirm by B -> becomes COMPLETED, completedAt set
        mockMvc.perform(post("/api/swaps/" + swapId + "/confirm-received").session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.bConfirmedAt").exists());
    }
}
