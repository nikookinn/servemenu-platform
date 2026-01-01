package com.servemenu.businessservice.common.exception;

public class StoreLimitExceededException extends RuntimeException {
    public StoreLimitExceededException(String message) {
        super(message);
    }
}
