package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.BatchOpeningHoursCommand;
import com.servemenu.businessservice.application.dto.command.OpeningHourCommand;
import com.servemenu.businessservice.application.dto.command.TimeSlotCommand;
import com.servemenu.businessservice.application.dto.response.OpeningHoursResponse;
import com.servemenu.businessservice.application.mapper.OpeningHoursMapper;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.enums.DayOfWeek;
import com.servemenu.businessservice.domain.model.OpeningHours;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.repository.OpeningHoursRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Opening Hours Service
 * Manages store opening hours with multiple time slots per day
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OpeningHoursService {

    private final OpeningHoursRepository openingHoursRepository;
    private final StoreRepository storeRepository;
    private final OpeningHoursMapper openingHoursMapper;

    /**
     * Update opening hours for a store
     * Supports multiple time slots per day
     */
    @Transactional
    @CacheEvict(value = "store", allEntries = true)
    public List<OpeningHoursResponse> updateOpeningHours(
            UUID storeId,
            BatchOpeningHoursCommand command
    ) {
        log.info("Updating opening hours: storeId={}", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        // Fetch existing opening hours
        List<OpeningHours> existingHours = openingHoursRepository.findByStoreIdOrderByDayOfWeekAsc(storeId);
        
        // Create a map for quick lookup
        Map<DayOfWeek, OpeningHours> existingHoursMap = existingHours.stream()
                .collect(Collectors.toMap(OpeningHours::getDayOfWeek, h -> h));

        List<OpeningHours> openingHoursList = new ArrayList<>();

        for (OpeningHourCommand hourCommand : command.openingHours()) {
            OpeningHours hours;
            
            // Check if opening hours already exist for this day
            if (existingHoursMap.containsKey(hourCommand.dayOfWeek())) {
                // Update existing record
                hours = existingHoursMap.get(hourCommand.dayOfWeek());
                hours.setIsOpen(hourCommand.isOpen());
                hours.clearTimeSlots();
            } else {
                // Create new record
                hours = OpeningHours.builder()
                        .store(store)
                        .dayOfWeek(hourCommand.dayOfWeek())
                        .isOpen(hourCommand.isOpen())
                        .timeSlots(new ArrayList<>())
                        .build();
            }

            // Add time slots
            if (hourCommand.timeSlots() != null && !hourCommand.timeSlots().isEmpty()) {
                for (TimeSlotCommand slotCommand : hourCommand.timeSlots()) {
                    LocalTime openTime = LocalTime.parse(slotCommand.openTime(), DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime closeTime = LocalTime.parse(slotCommand.closeTime(), DateTimeFormatter.ofPattern("HH:mm"));
                    hours.addTimeSlot(openTime, closeTime);
                }
            }

            openingHoursList.add(hours);
        }

        List<OpeningHours> saved = openingHoursRepository.saveAll(openingHoursList);
        return openingHoursMapper.toResponseList(saved);
    }

    /**
     * Get opening hours for a store
     */
    public List<OpeningHoursResponse> getOpeningHours(UUID storeId) {
        log.debug("Fetching opening hours: storeId={}", storeId);
        
        List<OpeningHours> hours = openingHoursRepository.findByStoreIdOrderByDayOfWeekAsc(storeId);
        return openingHoursMapper.toResponseList(hours);
    }

    /**
     * Initialize default opening hours for a new store
     * Creates 7 days (Monday-Sunday) with default time slot 00:00-23:59, all enabled
     */
    @Transactional
    public void initializeDefaultOpeningHours(UUID storeId) {
        log.info("Initializing default opening hours: storeId={}", storeId);

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        List<OpeningHours> defaultHours = new ArrayList<>();
        
        // Create opening hours for all 7 days
        for (DayOfWeek day : DayOfWeek.values()) {
            OpeningHours hours = OpeningHours.builder()
                    .store(store)
                    .dayOfWeek(day)
                    .isOpen(true)
                    .timeSlots(new ArrayList<>())
                    .build();
            
            // Add default time slot: 00:00 - 23:59 (24 hours)
            hours.addTimeSlot(LocalTime.of(0, 0), LocalTime.of(23, 59));
            
            defaultHours.add(hours);
        }

        openingHoursRepository.saveAll(defaultHours);
        log.info("✅ Default opening hours initialized: storeId={}", storeId);
    }
}
