package com.exchange.platform.dto;

import com.exchange.platform.entity.Listing;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ListingDTO {
    private Long id;
    private String title;
    private String description;
    private Long ownerId;
    private Listing.Status status;
    private Boolean isMine;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
