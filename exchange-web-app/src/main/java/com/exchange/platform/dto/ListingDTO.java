package com.exchange.platform.dto;

import com.exchange.platform.entity.Listing;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ListingDTO {
    private Long id;
    
    // 1. 卡片名稱 (必填)
    private String cardName;
    
    // 2. 團體名稱 (選填)
    private String groupName;
    
    // 3. 藝人名稱 (必填)
    private String artistName;
    
    // 4. 描述 (選填)
    private String description;
    
    // 5. 卡片來源 (必填)
    private Listing.CardSource cardSource;
    private String cardSourceDisplay; // 卡片來源的顯示名稱
    
    // 6. 品相等級 (必填, 1-10)
    private Integer conditionRating;
    
    // 7. 是否有保護措施 (必填)
    private Boolean hasProtection;
    
    // 8. 備註 (選填)
    private String remarks;
    
    // 9. 圖片 (必填)
    private List<String> imageUrls; // 解析後的圖片URL列表
    
    // 擁有者資訊
    private Long userId;
    private String ownerDisplayName; // 擁有者顯示名稱
    private Boolean isMine; // 是否為當前使用者的卡片
    
    // 10. 上架時間
    private LocalDateTime createdAt;
    
    // 更新時間
    private LocalDateTime updatedAt;
    
    // 11. 卡片狀態 (必填)
    private Listing.Status status;
    private String statusDisplay; // 狀態的顯示名稱
}
