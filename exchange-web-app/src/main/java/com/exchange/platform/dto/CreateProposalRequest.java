package com.exchange.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProposalRequest {
    @NotNull
    private Long listingId;
    private String message;
}
