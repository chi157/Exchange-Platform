package com.exchange.platform.exception;

import org.springframework.http.HttpStatus;

/**
 * 權限不足例外
 */
public class UnauthorizedAccessException extends BaseBusinessException {
    
    public UnauthorizedAccessException(Long userId, String resource) {
        super("UNAUTHORIZED_ACCESS",
              String.format("User %d cannot access %s", userId, resource),
              HttpStatus.FORBIDDEN);
    }
    
    public UnauthorizedAccessException(String message) {
        super("UNAUTHORIZED_ACCESS", message, HttpStatus.FORBIDDEN);
    }
}
