package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.model.SocialAccounts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, UUID> {

    Optional<SocialAccounts> findByStoreId(UUID storeId);

    boolean existsByStoreId(UUID storeId);
}
