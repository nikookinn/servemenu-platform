package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.enums.DomainVerificationStatus;
import com.servemenu.businessservice.domain.model.CustomDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomDomainRepository extends JpaRepository<CustomDomain, UUID> {

    Optional<CustomDomain> findByBusinessId(UUID businessId);

    Optional<CustomDomain> findByDomain(String domain);

    Optional<CustomDomain> findByVerificationToken(String verificationToken);

    boolean existsByDomain(String domain);

    boolean existsByBusinessId(UUID businessId);

    @Query("SELECT cd FROM CustomDomain cd WHERE cd.verificationStatus = :status")
    List<CustomDomain> findByVerificationStatus(@Param("status") DomainVerificationStatus status);
}
