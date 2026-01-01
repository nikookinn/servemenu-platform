package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.model.CustomerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
    @Query("SELECT c FROM CustomerProfile c WHERE c.user.id = :userId")
    Optional<CustomerProfile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM CustomerProfile c WHERE c.phoneNumber = :phoneNumber")
    Optional<CustomerProfile> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT c FROM CustomerProfile c WHERE c.createdByBusinessId = :businessId")
    Page<CustomerProfile> findByBusinessId(@Param("businessId") UUID businessId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM CustomerProfile c WHERE c.createdByBusinessId = :businessId")
    long countByBusinessId(@Param("businessId") UUID businessId);

    @Query("SELECT COUNT(c) > 0 FROM CustomerProfile c WHERE c.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}
