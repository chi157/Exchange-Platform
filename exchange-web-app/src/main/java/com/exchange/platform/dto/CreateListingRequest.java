package com.exchange.platform.dto;

import com.exchange.platform.entity.Listing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import java.util.List;

@Data
public class CreateListingRequest {
    @NotBlank
    private String title;
    private String description;
    
    // === 新增卡片屬性欄位 ===
    private String cardName;
    private String groupName;
    private String artistName;
    private Listing.CardSource cardSource;
    
    @Min(1)
    @Max(10)
    private Integer conditionRating; // 1-10成新
    
    private Boolean hasProtection = false;
    private String remarks;
    
    // 圖片檔案名稱列表（前端上傳後回傳檔名）
    private List<String> imageFileNames;
}
