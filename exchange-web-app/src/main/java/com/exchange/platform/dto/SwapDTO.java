package com.exchange.platform.dto;

import com.exchange.platform.entity.Swap;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

    @JsonProperty("aUserId")
    public Long getAUserId() {
        return aUserId;
    }

    @JsonProperty("bUserId")
    public Long getBUserId() {
        return bUserId;
    }
}
