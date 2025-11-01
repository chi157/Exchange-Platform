package com.exchange.platform.exception;

import org.springframework.http.HttpStatus;

/**
 * 業務規則違反例外
 */
public class BusinessRuleViolationException extends BaseBusinessException {
    
    public BusinessRuleViolationException(String rule, String reason) {
        super("BUSINESS_RULE_VIOLATION",
              String.format("Rule '%s' violated: %s", rule, reason),
              HttpStatus.UNPROCESSABLE_ENTITY);
    }
    
    public BusinessRuleViolationException(String message) {
        super("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
