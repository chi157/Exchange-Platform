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
    private String aUserDisplayName; // a user's display name
    private Long bUserId;
    private String bUserDisplayName; // b user's display name
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
    private String proposerDisplayName; // proposer's display name
    private Long receiverId;
    private String receiverDisplayName; // receiver's display name

    // 面交資訊
    private String meetupLocation;
    private LocalDateTime meetupTime;
    private String meetupNotes;
    private Boolean aMeetupConfirmed;
    private Boolean bMeetupConfirmed;

    @JsonProperty("aUserId")
    public Long getAUserId() {
        return aUserId;
    }

    @JsonProperty("aUserDisplayName")
    public String getAUserDisplayName() {
        return aUserDisplayName;
    }

    @JsonProperty("bUserId")
    public Long getBUserId() {
        return bUserId;
    }

    @JsonProperty("bUserDisplayName")
    public String getBUserDisplayName() {
        return bUserDisplayName;
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

    @JsonProperty("proposerDisplayName")
    public String getProposerDisplayName() {
        return proposerDisplayName;
    }

    @JsonProperty("receiverId")
    public Long getReceiverId() {
        return receiverId;
    }

    @JsonProperty("receiverDisplayName")
    public String getReceiverDisplayName() {
        return receiverDisplayName;
    }

    @JsonProperty("meetupLocation")
    public String getMeetupLocation() {
        return meetupLocation;
    }

    @JsonProperty("meetupTime")
    public LocalDateTime getMeetupTime() {
        return meetupTime;
    }

    @JsonProperty("meetupNotes")
    public String getMeetupNotes() {
        return meetupNotes;
    }

    @JsonProperty("aMeetupConfirmed")
    public Boolean getAMeetupConfirmed() {
        return aMeetupConfirmed;
    }

    @JsonProperty("bMeetupConfirmed")
    public Boolean getBMeetupConfirmed() {
        return bMeetupConfirmed;
    }
}
