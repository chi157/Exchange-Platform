package com.exchange.platform.exception;

import org.springframework.http.HttpStatus;

/**
 * 狀態轉換非法例外
 */
public class InvalidStateTransitionException extends BaseBusinessException {
    
    public InvalidStateTransitionException(String from, String to) {
        super("INVALID_STATE_TRANSITION",
              String.format("Cannot transition from %s to %s", from, to),
              HttpStatus.BAD_REQUEST);
    }
    
    public InvalidStateTransitionException(String message) {
        super("INVALID_STATE_TRANSITION", message, HttpStatus.BAD_REQUEST);
    }
}
