package com.exchange.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 驗證失敗例外
 */
@Getter
public class ValidationException extends BaseBusinessException {
    
    private Map<String, String> fieldErrors = new HashMap<>();
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }
    
    public void addFieldError(String field, String error) {
        fieldErrors.put(field, error);
    }
}
