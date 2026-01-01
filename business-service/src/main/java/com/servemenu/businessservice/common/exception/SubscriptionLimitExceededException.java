package com.servemenu.businessservice.common.exception;

/**
 * Exception thrown when a subscription plan limit is exceeded
 */
public class SubscriptionLimitExceededException extends RuntimeException {
    public SubscriptionLimitExceededException(String message) {
        super(message);
    }
}
