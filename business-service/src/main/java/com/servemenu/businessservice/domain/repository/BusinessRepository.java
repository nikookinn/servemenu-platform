package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.enums.*;
import com.servemenu.businessservice.domain.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Optional<Business> findByBusinessOwnerId(UUID businessOwnerId);

    Optional<Business> findByBusinessOwnerIdAndStatus(UUID businessOwnerId, BusinessStatus status);

    Optional<Business> findByBusinessOwnerIdAndStatusNot(UUID businessOwnerId, BusinessStatus status);

    Optional<Business> findByKeycloakUserId(UUID keycloakUserId);

    Optional<Business> findBySlug(String slug);

    Optional<Business> findByCustomDomain(String customDomain);

    boolean existsByBusinessOwnerId(UUID businessOwnerId);

    boolean existsBySlug(String slug);

    boolean existsByCustomDomain(String customDomain);

    @Query("SELECT b FROM Business b LEFT JOIN FETCH b.stores WHERE b.id = :businessId")
    Optional<Business> findByIdWithStores(@Param("businessId") UUID businessId);

    @Query("SELECT b FROM Business b " +
            "LEFT JOIN FETCH b.businessSettings " +
            "LEFT JOIN FETCH b.notificationSettings " +
            "LEFT JOIN FETCH b.orderSettings " +
            "WHERE b.id = :businessId")
    Optional<Business> findByIdWithSettings(@Param("businessId") UUID businessId);

    List<Business> findByStatus(BusinessStatus status);

    @Query("SELECT COUNT(b) FROM Business b WHERE b.subscriptionPlan = :plan AND b.status = 'ACTIVE'")
    long countBySubscriptionPlan(@Param("plan") SubscriptionPlan plan);
}
