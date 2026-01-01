package com.servemenu.businessservice.infrastructure.grpc;

import com.servemenu.businessservice.application.service.BusinessService;
import com.servemenu.businessservice.application.service.StoreService;
import com.servemenu.businessservice.domain.enums.SubscriptionPlan;
import com.servemenu.businessservice.domain.model.Business;
import com.servemenu.businessservice.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * gRPC Server Implementation for Business Service
 * Provides inter-service communication for user-service and other microservices
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BusinessGrpcServiceImpl extends BusinessGrpcServiceGrpc.BusinessGrpcServiceImplBase {

    private final BusinessService businessService;
    private final StoreService storeService;

    @Override
    @Transactional(readOnly = true)
    public void getBusinessByOwnerId(
            GetBusinessByOwnerIdRequest request,
            StreamObserver<BusinessResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received GetBusinessByOwnerId request for ownerId: {}", request.getBusinessOwnerId());
            
            UUID businessOwnerId = UUID.fromString(request.getBusinessOwnerId());
            Business business = businessService.findBusinessByOwnerId(businessOwnerId);

            BusinessResponse response = mapToBusinessResponse(business);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Successfully returned business for ownerId: {}", request.getBusinessOwnerId());
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getBusinessOwnerId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid business owner ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting business by owner ID: {}", e.getMessage(), e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Business not found for owner ID: " + request.getBusinessOwnerId())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getBusinessById(
            GetBusinessByIdRequest request,
            StreamObserver<BusinessResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Received GetBusinessById request for businessId: {}", request.getBusinessId());
            
            UUID businessId = UUID.fromString(request.getBusinessId());
            Business business = businessService.findBusinessById(businessId);

            BusinessResponse response = mapToBusinessResponse(business);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Successfully returned business with id: {}", request.getBusinessId());
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getBusinessId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid business ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting business by ID: {}", e.getMessage(), e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Business not found with ID: " + request.getBusinessId())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBusinessOwnership(
            ValidateOwnershipRequest request,
            StreamObserver<ValidateOwnershipResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Validating ownership for userId: {} and businessId: {}", 
                    request.getUserId(), request.getBusinessId());
            
            UUID userId = UUID.fromString(request.getUserId());
            UUID businessId = UUID.fromString(request.getBusinessId());
            
            Business business = businessService.findBusinessById(businessId);
            boolean isOwner = business.getBusinessOwnerId().equals(userId);
            
            ValidateOwnershipResponse response = ValidateOwnershipResponse.newBuilder()
                    .setIsOwner(isOwner)
                    .setMessage(isOwner ? "User is the owner" : "User is not the owner")
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Ownership validation result: {}", isOwner);
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format in ownership validation");
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid user ID or business ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error validating ownership: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error validating business ownership")
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkStoreLimit(
            CheckStoreLimitRequest request,
            StreamObserver<CheckStoreLimitResponse> responseObserver) {
        
        try {
            log.debug("gRPC: Checking store limit for businessId: {}", request.getBusinessId());
            
            UUID businessId = UUID.fromString(request.getBusinessId());
            Business business = businessService.findBusinessById(businessId);
            
            int currentStoreCount = storeService.countActiveStoresByBusinessId(businessId);
            SubscriptionPlan plan = business.getSubscriptionPlan();
            int maxStores = getMaxStoresForPlan(plan);
            
            boolean canCreateMore = maxStores == -1 || currentStoreCount < maxStores;
            
            CheckStoreLimitResponse response = CheckStoreLimitResponse.newBuilder()
                    .setCanCreateMore(canCreateMore)
                    .setCurrentCount(currentStoreCount)
                    .setMaxAllowed(maxStores)
                    .setSubscriptionPlan(plan.name())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
            log.debug("gRPC: Store limit check - current: {}, max: {}, canCreate: {}", 
                    currentStoreCount, maxStores, canCreateMore);
            
        } catch (IllegalArgumentException e) {
            log.error("gRPC: Invalid UUID format: {}", request.getBusinessId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid business ID format")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error checking store limit: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error checking store limit")
                    .asRuntimeException());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Maps Business domain entity to gRPC BusinessResponse
     */
    private BusinessResponse mapToBusinessResponse(Business business) {
        return BusinessResponse.newBuilder()
                .setId(business.getId().toString())
                .setBusinessOwnerId(business.getBusinessOwnerId().toString())
                .setKeycloakUserId(business.getKeycloakUserId().toString())
                .setBusinessName(business.getBusinessName())
                .setBusinessType(business.getBusinessType().name())
                .setCurrency(business.getCurrency())
                .addAllSupportedLanguages(business.getSupportedLanguages())
                .setDefaultLanguage(business.getDefaultLanguage())
                .setSlug(business.getSlug() != null ? business.getSlug() : "")
                .setCustomDomain(business.getCustomDomain() != null ? business.getCustomDomain() : "")
                .setCustomDomainVerified(Boolean.TRUE.equals(business.getCustomDomainVerified()))
                .setOnboardingCompleted(Boolean.TRUE.equals(business.getIsOnboardingCompleted()))
                .setStatus(business.getStatus().name())
                .setSubscriptionPlan(business.getSubscriptionPlan().name())
                .setCreatedAt(business.getCreatedAt().getEpochSecond())
                .setUpdatedAt(business.getUpdatedAt().getEpochSecond())
                .build();
    }

    /**
     * Gets max stores allowed for a subscription plan
     * Returns -1 for unlimited
     */
    private int getMaxStoresForPlan(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> 1;
            case REGULAR -> 5;
            case PREMIUM -> -1; // unlimited
        };
    }
}

