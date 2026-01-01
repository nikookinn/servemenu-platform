package com.servemenu.mediaservice.infrastructure.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested media asset is not found
 */
public class MediaNotFoundException extends MediaServiceException {
    
    public MediaNotFoundException(UUID id) {
        super(String.format("Media asset not found with ID: %s", id));
    }
    
    public MediaNotFoundException(String s3Key) {
        super(String.format("Media asset not found with S3 key: %s", s3Key));
    }
}
