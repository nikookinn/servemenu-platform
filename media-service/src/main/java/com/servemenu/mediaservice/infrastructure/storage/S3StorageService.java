package com.servemenu.mediaservice.infrastructure.storage;

import com.servemenu.mediaservice.domain.enums.MediaType;
import com.servemenu.mediaservice.infrastructure.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for interacting with AWS S3 storage.
 * Handles file upload, download, and deletion operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Value("${aws.s3.presigned-url-expiration:15}")
    private int presignedUrlExpirationMinutes;
    
    /**
     * Upload a file to S3
     * 
     * @param file MultipartFile to upload
     * @param entityId Entity ID this media belongs to
     * @param mediaType Type of media
     * @return S3 object key
     * @throws StorageException if upload fails
     */
    public String uploadFile(MultipartFile file, UUID entityId, MediaType mediaType) {
        String s3Key = generateS3Key(entityId, mediaType, getFileExtension(file.getOriginalFilename()));
        
        try {
            log.info("Uploading file to S3: bucket={}, key={}", bucketName, s3Key);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            log.info("Successfully uploaded file to S3: {}", s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            log.error("S3 error while uploading file: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            log.error("IO error while reading file: {}", e.getMessage(), e);
            throw new StorageException("Failed to read file content", e);
        } catch (Exception e) {
            log.error("Unexpected error while uploading file: {}", e.getMessage(), e);
            throw new StorageException("Failed to upload file", e);
        }
    }

    /**
     * Upload byte array to S3 with custom key (for QR codes)
     * 
     * @param s3Key Custom S3 key
     * @param data Byte array data
     * @param contentType Content type (e.g., "image/png")
     * @return Public URL of uploaded file
     * @throws StorageException if upload fails
     */
    public String uploadBytes(String s3Key, byte[] data, String contentType) {
        try {
            log.info("Uploading bytes to S3: bucket={}, key={}, size={}", bucketName, s3Key, data.length);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            
            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                    bucketName, region, s3Key);
            
            log.info("Successfully uploaded bytes to S3: {}", s3Key);
            return publicUrl;
            
        } catch (S3Exception e) {
            log.error("S3 error while uploading bytes: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to upload bytes to S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while uploading bytes: {}", e.getMessage(), e);
            throw new StorageException("Failed to upload bytes", e);
        }
    }
    
    /**
     * Delete a file from S3
     * 
     * @param s3Key S3 object key
     * @throws StorageException if deletion fails
     */
    public void deleteFile(String s3Key) {
        try {
            log.info("Deleting file from S3: bucket={}, key={}", bucketName, s3Key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            
            log.info("Successfully deleted file from S3: {}", s3Key);
            
        } catch (S3Exception e) {
            log.error("S3 error while deleting file: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting file: {}", e.getMessage(), e);
            throw new StorageException("Failed to delete file", e);
        }
    }
    
    /**
     * Download a file from S3
     * 
     * @param s3Key S3 object key
     * @return File content as byte array
     * @throws StorageException if download fails
     */
    public byte[] downloadFile(String s3Key) {
        try {
            log.info("Downloading file from S3: bucket={}, key={}", bucketName, s3Key);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            byte[] content = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            
            log.info("Successfully downloaded file from S3: {} ({} bytes)", s3Key, content.length);
            return content;
            
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", s3Key);
            throw new StorageException("File not found in S3: " + s3Key, e);
        } catch (S3Exception e) {
            log.error("S3 error while downloading file: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to download file from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while downloading file: {}", e.getMessage(), e);
            throw new StorageException("Failed to download file", e);
        }
    }
    
    /**
     * Check if a file exists in S3
     * 
     * @param s3Key S3 object key
     * @return true if file exists
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("S3 error while checking file existence: {}", e.awsErrorDetails().errorMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate public URL for an S3 object
     * 
     * @param s3Key S3 object key
     * @return Public URL
     */
    public String generatePublicUrl(String s3Key) {
        // Format: https://{bucket}.s3.{region}.amazonaws.com/{key}
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, s3Key);
    }
    
    /**
     * Generate S3 key for a media asset
     * Format: {type}/{entityId}/{timestamp}.{extension}
     * 
     * @param entityId Entity ID
     * @param mediaType Media type
     * @param extension File extension
     * @return S3 key
     */
    private String generateS3Key(UUID entityId, MediaType mediaType, String extension) {
        long timestamp = Instant.now().toEpochMilli();
        return String.format("%s/%s/%d.%s", 
            mediaType.getS3Prefix(), 
            entityId.toString(), 
            timestamp, 
            extension
        );
    }
    
    /**
     * Extract file extension from filename
     * 
     * @param filename Original filename
     * @return File extension (lowercase, without dot)
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Generate pre-signed URL for secure temporary access
     * URL expires after configured duration (default: 15 minutes)
     * 
     * @param s3Key S3 object key
     * @return Pre-signed URL
     * @throws StorageException if generation fails
     */
    public String generatePresignedUrl(String s3Key) {
        return generatePresignedUrl(s3Key, Duration.ofMinutes(presignedUrlExpirationMinutes));
    }
    
    /**
     * Generate pre-signed URL with custom expiration
     * 
     * @param s3Key S3 object key
     * @param expiration Duration until URL expires
     * @return Pre-signed URL
     * @throws StorageException if generation fails
     */
    public String generatePresignedUrl(String s3Key, Duration expiration) {
        try {
            log.debug("Generating presigned URL for: {} (expires in: {})", s3Key, expiration);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.debug("Presigned URL generated successfully");
            return url;
            
        } catch (S3Exception e) {
            log.error("S3 error while generating presigned URL: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to generate presigned URL", e);
        } catch (Exception e) {
            log.error("Unexpected error while generating presigned URL: {}", e.getMessage(), e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }
    
    /**
     * Generate presigned URL for download with Content-Disposition header
     * Forces browser to download instead of displaying
     * 
     * @param s3Key S3 object key
     * @param filename Desired filename for download
     * @return Pre-signed download URL
     * @throws StorageException if generation fails
     */
    public String generatePresignedDownloadUrl(String s3Key, String filename) {
        try {
            log.debug("Generating presigned download URL for: {} as {}", s3Key, filename);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .responseContentDisposition("attachment; filename=\"" + filename + "\"")
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.debug("Presigned download URL generated successfully");
            return url;
            
        } catch (S3Exception e) {
            log.error("S3 error while generating presigned download URL: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to generate presigned download URL", e);
        } catch (Exception e) {
            log.error("Unexpected error while generating presigned download URL: {}", e.getMessage(), e);
            throw new StorageException("Failed to generate presigned download URL", e);
        }
    }
    
    /**
     * Get file metadata from S3
     * 
     * @param s3Key S3 object key
     * @return HeadObjectResponse containing metadata
     * @throws StorageException if operation fails
     */
    public HeadObjectResponse getFileMetadata(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
            
            return s3Client.headObject(headObjectRequest);
            
        } catch (NoSuchKeyException e) {
            throw new StorageException("File not found in S3: " + s3Key, e);
        } catch (S3Exception e) {
            log.error("S3 error while getting file metadata: {}", e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to get file metadata from S3", e);
        }
    }
}
