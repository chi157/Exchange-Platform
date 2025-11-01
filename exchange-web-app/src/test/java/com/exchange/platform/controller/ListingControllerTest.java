package com.exchange.platform.controller;

import com.exchange.platform.dto.CreateListingRequest;
import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.service.ListingService;
import com.exchange.platform.service.ProposalService;
import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.entity.Proposal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ListingController.class)
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListingService listingService;

        @MockBean
        private ProposalService proposalService;

    @Test
    @DisplayName("POST /api/listings -> 201 when created")
    void create_created() throws Exception {
        Mockito.when(listingService.create(any(CreateListingRequest.class), any()))
                .thenReturn(ListingDTO.builder().id(1L).title("T").description("D").ownerId(7L)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        String body = "{\"title\":\"T\",\"description\":\"D\"}";
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/listings/{id} -> 200")
    void getById_ok() throws Exception {
        Mockito.when(listingService.getById(eq(1L)))
                .thenReturn(ListingDTO.builder().id(1L).title("T").ownerId(7L)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        mockMvc.perform(get("/api/listings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/listings -> 200 list")
    void list_ok() throws Exception {
        Mockito.when(listingService.list(any(), any(), any(), any()))
                .thenReturn(List.of(
                        ListingDTO.builder().id(1L).title("A").ownerId(1L).build(),
                        ListingDTO.builder().id(2L).title("B").ownerId(2L).build()
                ));

        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    @DisplayName("GET /api/listings/{id}/proposals -> 200 list")
    void listProposalsByListing_ok() throws Exception {
        Mockito.when(proposalService.listByListing(eq(1L), any(), any(), any()))
                .thenReturn(java.util.List.of(
                        ProposalDTO.builder().id(100L).listingId(1L).proposerId(9L).status(Proposal.Status.PENDING).build()
                ));

        mockMvc.perform(get("/api/listings/1/proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }
}
