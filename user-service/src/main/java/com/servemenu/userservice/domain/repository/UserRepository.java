package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.enums.AccountStatus;
import com.servemenu.userservice.domain.enums.UserType;
import com.servemenu.userservice.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.keycloakUserId = :keycloakUserId AND u.deletedAt IS NULL")
    Optional<User> findActiveByKeycloakUserId(@Param("keycloakUserId") UUID keycloakUserId);

    boolean existsByKeycloakUserId(UUID keycloakUserId);

    boolean existsByEmail(String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.userType = :userType AND u.accountStatus = :status AND u.deletedAt IS NULL")
    Optional<User> findByUserTypeAndStatus(@Param("userType") UserType userType, @Param("status") AccountStatus status);
}
