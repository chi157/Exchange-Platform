package com.exchange.platform.controller;

import com.exchange.platform.dto.CreateProposalRequest;
import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.service.ProposalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProposalController.class)
class ProposalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProposalService proposalService;

    @Test
    @DisplayName("POST /api/proposals -> 201 when created")
    void create_created() throws Exception {
        Mockito.when(proposalService.create(any(CreateProposalRequest.class), any()))
                .thenReturn(ProposalDTO.builder()
                        .id(10L)
                        .listingId(1L)
                        .proposerId(2L)
                        .message("hi")
                        .status(Proposal.Status.PENDING)
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                        .build());

        String body = "{\"listingId\":1,\"message\":\"hi\"}";
        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST /api/proposals/{id}/accept -> 200")
    void accept_ok() throws Exception {
        Mockito.when(proposalService.accept(eq(10L), any()))
                .thenReturn(ProposalDTO.builder().id(10L).listingId(1L).proposerId(2L)
                        .status(Proposal.Status.ACCEPTED).build());

        mockMvc.perform(post("/api/proposals/10/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("POST /api/proposals/{id}/reject -> 200")
    void reject_ok() throws Exception {
        Mockito.when(proposalService.reject(eq(11L), any()))
                .thenReturn(ProposalDTO.builder().id(11L).listingId(1L).proposerId(2L)
                        .status(Proposal.Status.REJECTED).build());

        mockMvc.perform(post("/api/proposals/11/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Exceptions -> mapped to proper HTTP status")
    void exceptions_mapped() throws Exception {
        Mockito.when(proposalService.create(any(CreateProposalRequest.class), any()))
                .thenThrow(new ProposalService.UnauthorizedException());
        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"listingId\":1}"))
                .andExpect(status().isUnauthorized());

        Mockito.when(proposalService.accept(eq(999L), any()))
                .thenThrow(new ProposalService.NotFoundException());
        mockMvc.perform(post("/api/proposals/999/accept"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/proposals/mine -> 200 list")
    void listMine_ok() throws Exception {
        Mockito.when(proposalService.listMine(any(), any(), any(), any()))
                .thenReturn(java.util.List.of(
                        ProposalDTO.builder().id(1L).listingId(10L).proposerId(2L).status(Proposal.Status.PENDING).build()
                ));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/proposals/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/proposals/received -> 200 list")
    void listReceived_ok() throws Exception {
        Mockito.when(proposalService.listReceived(any(), any(), any(), any()))
                .thenReturn(java.util.List.of(
                        ProposalDTO.builder().id(2L).listingId(11L).proposerId(3L).status(Proposal.Status.PENDING).build()
                ));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/proposals/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }
}
