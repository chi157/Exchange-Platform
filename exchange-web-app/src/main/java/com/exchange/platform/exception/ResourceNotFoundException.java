package com.exchange.platform.exception;

import org.springframework.http.HttpStatus;

/**
 * 資源不存在例外
 */
public class ResourceNotFoundException extends BaseBusinessException {
    
    public ResourceNotFoundException(String resourceType, Long id) {
        super("RESOURCE_NOT_FOUND",
              String.format("%s with id %d not found", resourceType, id),
              HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
