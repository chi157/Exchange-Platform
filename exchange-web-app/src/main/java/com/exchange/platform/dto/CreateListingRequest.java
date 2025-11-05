package com.exchange.platform.dto;

import com.exchange.platform.entity.Listing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CreateListingRequest {
    
    // 1. 卡片名稱 (必填)
    @NotBlank(message = "卡片名稱為必填")
    @Size(max = 200, message = "卡片名稱不可超過200個字元")
    private String cardName;
    
    // 2. 團體名稱 (選填)
    @Size(max = 100, message = "團體名稱不可超過100個字元")
    private String groupName;
    
    // 3. 藝人名稱 (必填)
    @NotBlank(message = "藝人名稱為必填")
    @Size(max = 100, message = "藝人名稱不可超過100個字元")
    private String artistName;
    
    // 4. 描述 (選填)
    private String description;
    
    // 5. 卡片來源 (必填)
    @NotNull(message = "卡片來源為必填")
    private Listing.CardSource cardSource;
    
    // 6. 品相等級 (必填, 1-10)
    @NotNull(message = "品相等級為必填")
    @Min(value = 1, message = "品相等級最低為1")
    @Max(value = 10, message = "品相等級最高為10")
    private Integer conditionRating;
    
    // 7. 是否有保護措施 (必填)
    @NotNull(message = "是否有保護措施為必填")
    private Boolean hasProtection;
    
    // 8. 備註 (選填)
    private String remarks;
    
    // 9. 圖片檔案名稱列表 (必填，前端上傳後回傳檔名)
    @NotNull(message = "圖片為必填")
    @Size(min = 1, message = "至少需要上傳1張圖片")
    private List<String> imageFileNames;
}
