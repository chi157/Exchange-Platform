package com.exchange.platform.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 基礎業務例外類別
 * 所有自訂業務例外皆繼承此類別
 */
@Getter
public abstract class BaseBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public BaseBusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public BaseBusinessException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
