package com.exchange.platform.dto;

import com.exchange.platform.entity.Swap;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, isGetterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
@Builder
public class SwapDTO {
    private Long id;
    private Long listingId;
    private Long proposalId;
    private Long aUserId;
    private Long bUserId;
    private Swap.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime aConfirmedAt;
    private LocalDateTime bConfirmedAt;
    
    // Proposal items information
    private List<ProposalDTO.ProposalItemDTO> proposerItems;
    private List<ProposalDTO.ProposalItemDTO> receiverItems;
    private Long proposerId;

    @JsonProperty("aUserId")
    public Long getAUserId() {
        return aUserId;
    }

    @JsonProperty("bUserId")
    public Long getBUserId() {
        return bUserId;
    }

    @JsonProperty("aConfirmedAt")
    public LocalDateTime getAConfirmedAt() {
        return aConfirmedAt;
    }

    @JsonProperty("bConfirmedAt")
    public LocalDateTime getBConfirmedAt() {
        return bConfirmedAt;
    }
    
    @JsonProperty("proposerItems")
    public List<ProposalDTO.ProposalItemDTO> getProposerItems() {
        return proposerItems;
    }
    
    @JsonProperty("receiverItems")
    public List<ProposalDTO.ProposalItemDTO> getReceiverItems() {
        return receiverItems;
    }
    
    @JsonProperty("proposerId")
    public Long getProposerId() {
        return proposerId;
    }
}
