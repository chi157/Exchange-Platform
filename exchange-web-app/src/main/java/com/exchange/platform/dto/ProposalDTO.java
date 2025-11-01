package com.exchange.platform.dto;

import com.exchange.platform.entity.Proposal;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProposalDTO {
    private Long id;
    private Long listingId;
    private Long proposerId;
    private String message;
    private Proposal.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
