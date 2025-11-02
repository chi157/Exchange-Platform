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
    private String ownerDisplayName; // owner's display name
    private Listing.Status status;
    private Boolean isMine;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // === 新增卡片屬性 ===
    private String cardName;
    private String groupName;
    private String artistName;
    private Listing.CardSource cardSource;
    private Integer conditionRating; // 1-10成新
    private Boolean hasProtection;
    private String remarks;
    private java.util.List<String> imageUrls; // 解析後的圖片URL列表
}
