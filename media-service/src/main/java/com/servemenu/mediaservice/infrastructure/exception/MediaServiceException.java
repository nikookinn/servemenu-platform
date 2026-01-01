package com.servemenu.mediaservice.infrastructure.exception;

/**
 * Base exception for all media service related errors
 */
public class MediaServiceException extends RuntimeException {
    
    public MediaServiceException(String message) {
        super(message);
    }
    
    public MediaServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
