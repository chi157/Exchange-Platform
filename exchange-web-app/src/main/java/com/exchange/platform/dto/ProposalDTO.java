package com.exchange.platform.dto;

import com.exchange.platform.entity.Proposal;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProposalDTO {
    private Long id;
    private Long listingId; // receiver's listing (for backward compatibility)
    private Long proposerId;
    private String proposerDisplayName; // proposer's display name
    private Long receiverId;
    private String receiverDisplayName; // receiver's display name
    private String message;
    private Proposal.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Items involved in the exchange
    private List<ProposalItemDTO> proposerItems; // what proposer offers
    private List<ProposalItemDTO> receiverItems; // what receiver offers (currently just one)
    
    @Data
    @Builder
    public static class ProposalItemDTO {
        private Long itemId;
        private Long listingId;
        private String listingDisplay; // Card display: "cardName - artistName"
        private String side; // PROPOSER or RECEIVER
    }
}
