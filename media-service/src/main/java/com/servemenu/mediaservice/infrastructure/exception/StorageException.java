package com.servemenu.mediaservice.infrastructure.exception;

/**
 * Exception thrown when S3 storage operations fail
 */
public class StorageException extends MediaServiceException {
    
    public StorageException(String message) {
        super(message);
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
