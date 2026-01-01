package com.servemenu.businessservice.domain.repository;

import com.servemenu.businessservice.domain.enums.DayOfWeek;
import com.servemenu.businessservice.domain.model.OpeningHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpeningHoursRepository extends JpaRepository<OpeningHours, UUID> {

    List<OpeningHours> findByStoreIdOrderByDayOfWeekAsc(UUID storeId);

    Optional<OpeningHours> findByStoreIdAndDayOfWeek(UUID storeId, DayOfWeek dayOfWeek);

    @Modifying
    @Transactional
    void deleteByStoreId(UUID storeId);

    boolean existsByStoreIdAndDayOfWeek(UUID storeId, DayOfWeek dayOfWeek);

    @Query("SELECT oh FROM OpeningHours oh WHERE oh.store.id = :storeId AND oh.isOpen = true")
    List<OpeningHours> findOpenDaysByStoreId(@Param("storeId") UUID storeId);
}
