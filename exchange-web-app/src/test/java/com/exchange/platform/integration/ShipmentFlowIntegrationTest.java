package com.exchange.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ShipmentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void m4_shipment_upsert_and_events() throws Exception {
        // Register/login A
        MockHttpSession sessionA = new MockHttpSession();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a_shp@test.com\",\"password\":\"pwA\",\"displayName\":\"A\"}"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).session(sessionA)
                .content("{\"email\":\"a_shp@test.com\",\"password\":\"pwA\"}"))
                .andExpect(status().isOk());

        // A creates listing
        String listingResp = mockMvc.perform(post("/api/listings").session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Item\",\"description\":\"D\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long listingId = objectMapper.readTree(listingResp).get("id").asLong();

        // Register/login B
        MockHttpSession sessionB = new MockHttpSession();
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"b_shp@test.com\",\"password\":\"pwB\",\"displayName\":\"B\"}"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).session(sessionB)
                .content("{\"email\":\"b_shp@test.com\",\"password\":\"pwB\"}"))
                .andExpect(status().isOk());

        // B proposes to listing
        String propResp = mockMvc.perform(post("/api/proposals").session(sessionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("listingId", listingId, "message", "offer"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long proposalId = objectMapper.readTree(propResp).get("id").asLong();

        // A accepts proposal -> swap created and listing locked
        mockMvc.perform(post("/api/proposals/" + proposalId + "/accept").session(sessionA))
                .andExpect(status().isOk());

        // A upsert shipment for the swap (id is not directly returned; query /api/swaps/mine and take first)
        String swapsA = mockMvc.perform(get("/api/swaps/mine").session(sessionA))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long swapId = objectMapper.readTree(swapsA).get(0).get("id").asLong();

        mockMvc.perform(post("/api/swaps/" + swapId + "/shipments/my").session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deliveryMethod\":\"shipnow\",\"trackingNumber\":\"T123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderId").isNumber())
                .andExpect(jsonPath("$.deliveryMethod").value("SHIPNOW"))
                .andExpect(jsonPath("$.trackingNumber").value("T123"));

        // find shipment id by upserting again (response contains id) and then add event
        String upsertResp = mockMvc.perform(post("/api/swaps/" + swapId + "/shipments/my").session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deliveryMethod\":\"shipnow\",\"trackingNumber\":\"T123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long shipmentId = objectMapper.readTree(upsertResp).get("id").asLong();

        // B cannot add event to A's shipment
        mockMvc.perform(post("/api/shipments/" + shipmentId + "/events").session(sessionB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHIPPED\",\"note\":\"n\",\"at\":\"2025-01-01T00:00:00\"}"))
                .andExpect(status().isForbidden());

        // A adds event
        mockMvc.perform(post("/api/shipments/" + shipmentId + "/events").session(sessionA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SHIPPED\",\"note\":\"ok\",\"at\":\"2025-01-01T00:00:00\"}"))
                .andExpect(status().isCreated());
    }
}
