package com.servemenu.userservice.domain.repository;

import com.servemenu.userservice.domain.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, UUID> {
    @Query("SELECT p FROM UserPreferences p WHERE p.user.id = :userId")
    Optional<UserPreferences> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(p) > 0 FROM UserPreferences p WHERE p.user.id = :userId")
    boolean existsByUserId(@Param("userId") UUID userId);
}
