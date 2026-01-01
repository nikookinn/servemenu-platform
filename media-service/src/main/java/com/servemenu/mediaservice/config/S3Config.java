package com.servemenu.mediaservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.upload-timeout:60}")
    private Integer uploadTimeout;

    @Value("${aws.s3.connection-timeout:10}")
    private Integer connectionTimeout;

    /**
     * Development profile - LocalStack S3 configuration
     */
    @Bean
    @Profile("dev")
    public S3Client devS3Client(
            @Value("${aws.endpoint}") String endpoint,
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey) {

        log.info("===========================================");
        log.info("Initializing LocalStack S3 Client (DEV)");
        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucketName);
        log.info("===========================================");

        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)  // LocalStack requires path-style
                        .build())
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(uploadTimeout))
                        .apiCallAttemptTimeout(Duration.ofSeconds(connectionTimeout)))
                .build();

        // Auto-create bucket in dev
        createBucketIfNotExists(s3Client);

        log.info("LocalStack S3 Client initialized successfully");
        return s3Client;
    }

    /**
     * Production profile - Real AWS S3 configuration
     */
    @Bean
    @Profile("prod")
    public S3Client prodS3Client(
            @Value("${aws.s3.use-accelerate-endpoint:false}") boolean useAccelerate) {

        log.info("===========================================");
        log.info("Initializing Production S3 Client");
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucketName);
        log.info("Transfer Acceleration: {}", useAccelerate);
        log.info("===========================================");

        // Uses IAM role, environment variables, or AWS credentials file
        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .serviceConfiguration(S3Configuration.builder()
                        .accelerateModeEnabled(useAccelerate)
                        .pathStyleAccessEnabled(false)
                        .build())
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(uploadTimeout))
                        .apiCallAttemptTimeout(Duration.ofSeconds(connectionTimeout)))
                .build();

        // Verify bucket exists in production
        verifyBucketExists(s3Client);

        log.info("Production S3 Client initialized successfully");
        return s3Client;
    }

    /**
     * Create bucket if it doesn't exist (dev only)
     */
    private void createBucketIfNotExists(S3Client s3Client) {
        try {
            s3Client.headBucket(builder -> builder.bucket(bucketName));
            log.info("✓ Bucket '{}' already exists", bucketName);
            
            // Ensure CORS is configured (for frontend access to presigned URLs)
            configureBucketCors(s3Client);
        } catch (Exception e) {
            try {
                s3Client.createBucket(builder -> builder.bucket(bucketName));
                log.info("✓ Created bucket '{}'", bucketName);

                // Wait a bit for bucket to be ready
                Thread.sleep(2000);
                
                // Configure CORS for frontend access
                configureBucketCors(s3Client);
            } catch (Exception createException) {
                log.error("✗ Failed to create bucket '{}'", bucketName, createException);
                throw new RuntimeException("Could not create S3 bucket", createException);
            }
        }
    }
    
    /**
     * Configure CORS for bucket (allows frontend to load images from S3)
     */
    private void configureBucketCors(S3Client s3Client) {
        try {
            software.amazon.awssdk.services.s3.model.CORSRule corsRule = software.amazon.awssdk.services.s3.model.CORSRule.builder()
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "*")
                .allowedMethods("GET", "HEAD")
                .allowedHeaders("*")
                .maxAgeSeconds(3600)
                .build();
            
            software.amazon.awssdk.services.s3.model.CORSConfiguration corsConfiguration = 
                software.amazon.awssdk.services.s3.model.CORSConfiguration.builder()
                    .corsRules(corsRule)
                    .build();
            
            s3Client.putBucketCors(builder -> builder
                .bucket(bucketName)
                .corsConfiguration(corsConfiguration));
            
            log.info("✓ CORS configured for bucket '{}'", bucketName);
        } catch (Exception e) {
            log.warn("⚠ Failed to configure CORS for bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    /**
     * Verify bucket exists (production)
     */
    private void verifyBucketExists(S3Client s3Client) {
        try {
            s3Client.headBucket(builder -> builder.bucket(bucketName));
            log.info("✓ Bucket '{}' verified", bucketName);
        } catch (Exception e) {
            log.error("✗ Bucket '{}' does not exist or is not accessible", bucketName);
            throw new RuntimeException("S3 bucket not accessible: " + bucketName, e);
        }
    }
    
    /**
     * S3 Presigner for generating pre-signed URLs - Development
     */
    @Bean
    @Profile("dev")
    public S3Presigner devS3Presigner(
            @Value("${aws.endpoint}") String endpoint,
            @Value("${aws.access-key-id}") String accessKey,
            @Value("${aws.secret-access-key}") String secretKey) {
        
        log.info("Initializing LocalStack S3 Presigner (DEV)");
        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    /**
     * S3 Presigner for generating pre-signed URLs - Production
     */
    @Bean
    @Profile("prod")
    public S3Presigner prodS3Presigner() {
        log.info("Initializing Production S3 Presigner for region: {}", region);
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}