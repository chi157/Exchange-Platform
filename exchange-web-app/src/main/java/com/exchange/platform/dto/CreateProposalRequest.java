package com.exchange.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateProposalRequest {
    @NotNull
    private Long listingId; // receiver's listing (what proposer wants)
    
    @NotEmpty(message = "Must offer at least one item")
    private List<Long> proposerListingIds; // proposer's listings (what proposer offers)
    
    private String message;
}
