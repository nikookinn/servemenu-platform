package com.servemenu.mediaservice.infrastructure.storage;

import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.infrastructure.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Instant;
import java.util.UUID;

/**
 * Helper class for S3 upload operations with multi-size support
 * 
 * @author ServeMenu Platform Team
 * @version 1.0.0
 * @since 2025-11-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3UploadHelper {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    /**
     * Upload resized image to S3
     * 
     * @param imageBytes Image bytes
     * @param entityId Entity ID
     * @param mediaType Media type
     * @param sizeType Size type (thumbnail, medium, large)
     * @param extension File extension
     * @param contentType MIME type
     * @return S3 key
     */
    public String uploadResizedImage(
            byte[] imageBytes,
            UUID entityId,
            MediaType mediaType,
            String sizeType,
            String extension,
            String contentType
    ) {
        String s3Key = generateResizedS3Key(entityId, mediaType, sizeType, extension);
        
        try {
            log.debug("Uploading {} image to S3: bucket={}, key={}", sizeType, bucketName, s3Key);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .contentLength((long) imageBytes.length)
                .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
            
            log.debug("Successfully uploaded {} image to S3: {} ({} bytes)", sizeType, s3Key, imageBytes.length);
            return s3Key;
            
        } catch (S3Exception e) {
            log.error("S3 error while uploading {} image: {}", sizeType, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to upload " + sizeType + " image to S3", e);
        } catch (Exception e) {
            log.error("Unexpected error while uploading {} image: {}", sizeType, e.getMessage(), e);
            throw new StorageException("Failed to upload " + sizeType + " image", e);
        }
    }
    
    /**
     * Generate S3 key for resized image
     * Format: {sizeType}/{type}/{entityId}/{timestamp}.{extension}
     * 
     * @param entityId Entity ID
     * @param mediaType Media type
     * @param sizeType Size type (thumbnails, medium, large)
     * @param extension File extension
     * @return S3 key
     */
    private String generateResizedS3Key(UUID entityId, MediaType mediaType, String sizeType, String extension) {
        long timestamp = Instant.now().toEpochMilli();
        return String.format("%s/%s/%s/%d.%s", 
            sizeType,  // thumbnails, medium, large
            mediaType.getS3Prefix(), 
            entityId.toString(), 
            timestamp, 
            extension
        );
    }
}
