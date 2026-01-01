package com.servemenu.userservice.infrastructure.grpc;

import com.servemenu.businessservice.grpc.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * gRPC Client for communicating with Business Service
 * Provides strongly-typed, efficient inter-service communication
 */
@Service
public class BusinessGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BusinessGrpcClient.class);

    @GrpcClient("business-service")
    private BusinessGrpcServiceGrpc.BusinessGrpcServiceBlockingStub businessServiceStub;

    /**
     * Get business by owner ID
     * Returns Optional.empty() if not found
     */
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Optional<BusinessResponse> getBusinessByOwnerId(UUID businessOwnerId) {
        try {
            log.debug("gRPC Client: Fetching business for ownerId: {}", businessOwnerId);
            
            GetBusinessByOwnerIdRequest request = GetBusinessByOwnerIdRequest.newBuilder()
                    .setBusinessOwnerId(businessOwnerId.toString())
                    .build();

            BusinessResponse response = businessServiceStub.getBusinessByOwnerId(request);
            
            log.debug("gRPC Client: Business fetched successfully: {}", response.getId());
            return Optional.of(response);
            
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.warn("gRPC Client: Business not found for ownerId: {}", businessOwnerId);
                return Optional.empty();
            }
            log.error("gRPC Client: Error fetching business by ownerId: {}", e.getMessage());
            throw new GrpcCommunicationException("Failed to fetch business by owner ID", e);
        }
    }

    /**
     * Get business by ID
     * Returns Optional.empty() if not found
     */
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Optional<BusinessResponse> getBusinessById(UUID businessId) {
        try {
            log.debug("gRPC Client: Fetching business by ID: {}", businessId);
            
            GetBusinessByIdRequest request = GetBusinessByIdRequest.newBuilder()
                    .setBusinessId(businessId.toString())
                    .build();

            BusinessResponse response = businessServiceStub.getBusinessById(request);
            
            log.debug("gRPC Client: Business fetched successfully: {}", response.getId());
            return Optional.of(response);
            
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                log.warn("gRPC Client: Business not found with ID: {}", businessId);
                return Optional.empty();
            }
            log.error("gRPC Client: Error fetching business by ID: {}", e.getMessage());
            throw new GrpcCommunicationException("Failed to fetch business by ID", e);
        }
    }

    /**
     * Validate business ownership
     * Returns true if user owns the business, false otherwise
     */
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public boolean validateBusinessOwnership(UUID userId, UUID businessId) {
        try {
            log.debug("gRPC Client: Validating ownership - userId: {}, businessId: {}", userId, businessId);
            
            ValidateOwnershipRequest request = ValidateOwnershipRequest.newBuilder()
                    .setUserId(userId.toString())
                    .setBusinessId(businessId.toString())
                    .build();

            ValidateOwnershipResponse response = businessServiceStub.validateBusinessOwnership(request);
            
            log.debug("gRPC Client: Ownership validation result: {}", response.getIsOwner());
            return response.getIsOwner();
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Error validating ownership: {}", e.getMessage());
            throw new GrpcCommunicationException("Failed to validate business ownership", e);
        }
    }

    /**
     * Check if business can create more stores based on subscription plan
     * Returns CheckStoreLimitResponse with details
     */
    @Retryable(
            retryFor = {StatusRuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public CheckStoreLimitResponse checkStoreLimit(UUID businessId) {
        try {
            log.debug("gRPC Client: Checking store limit for businessId: {}", businessId);
            
            CheckStoreLimitRequest request = CheckStoreLimitRequest.newBuilder()
                    .setBusinessId(businessId.toString())
                    .build();

            CheckStoreLimitResponse response = businessServiceStub.checkStoreLimit(request);
            
            log.debug("gRPC Client: Store limit check - canCreateMore: {}, current: {}, max: {}",
                    response.getCanCreateMore(), response.getCurrentCount(), response.getMaxAllowed());
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC Client: Error checking store limit: {}", e.getMessage());
            throw new GrpcCommunicationException("Failed to check store limit", e);
        }
    }

    /**
     * Check if business can create more stores (simplified boolean version)
     */
    public boolean canCreateMoreStores(UUID businessId) {
        try {
            CheckStoreLimitResponse response = checkStoreLimit(businessId);
            return response.getCanCreateMore();
        } catch (Exception e) {
            log.error("gRPC Client: Error in canCreateMoreStores: {}", e.getMessage());
            // Fail-safe: return false to prevent creation if check fails
            return false;
        }
    }
}

