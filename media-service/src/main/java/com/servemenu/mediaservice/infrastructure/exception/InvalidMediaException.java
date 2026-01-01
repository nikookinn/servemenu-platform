package com.servemenu.mediaservice.infrastructure.exception;

/**
 * Exception thrown when media validation fails
 */
public class InvalidMediaException extends MediaServiceException {
    
    public InvalidMediaException(String message) {
        super(message);
    }
    
    public InvalidMediaException(String message, Throwable cause) {
        super(message, cause);
    }
}
