package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.model.BusinessOwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BusinessOwnerProfileRepository extends JpaRepository<BusinessOwnerProfile, UUID> {
    @Query("SELECT b FROM BusinessOwnerProfile b WHERE b.user.id = :userId")
    Optional<BusinessOwnerProfile> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT b FROM BusinessOwnerProfile b WHERE b.user.keycloakUserId = :keycloakUserId")
    Optional<BusinessOwnerProfile> findByKeycloakUserId(@Param("keycloakUserId") UUID keycloakUserId);

    @Query("SELECT b FROM BusinessOwnerProfile b JOIN FETCH b.user WHERE b.user.id = :userId")
    Optional<BusinessOwnerProfile> findByUserIdWithUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(b) > 0 FROM BusinessOwnerProfile b WHERE b.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}
