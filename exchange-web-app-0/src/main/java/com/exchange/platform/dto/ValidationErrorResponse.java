package com.exchange.platform.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 驗證錯誤回應 DTO（包含欄位錯誤細節）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> fieldErrors;
    
    public ValidationErrorResponse(String errorCode, String message, 
                                   Map<String, String> fieldErrors, LocalDateTime timestamp) {
        super(errorCode, message, timestamp);
        this.fieldErrors = fieldErrors;
    }
}
