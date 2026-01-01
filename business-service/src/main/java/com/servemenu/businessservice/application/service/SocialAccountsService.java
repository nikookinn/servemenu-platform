package com.servemenu.businessservice.application.service;

import com.servemenu.businessservice.application.dto.command.UpdateSocialAccountsCommand;
import com.servemenu.businessservice.application.dto.response.SocialAccountsResponse;
import com.servemenu.businessservice.application.mapper.SettingsMapper;
import com.servemenu.businessservice.common.exception.ResourceNotFoundException;
import com.servemenu.businessservice.domain.model.SocialAccounts;
import com.servemenu.businessservice.domain.model.Store;
import com.servemenu.businessservice.domain.repository.SocialAccountsRepository;
import com.servemenu.businessservice.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Social Accounts Service
 * Manages store social media accounts (Facebook, Instagram, etc.)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SocialAccountsService {

    private final SocialAccountsRepository socialAccountsRepository;
    private final StoreRepository storeRepository;
    private final SettingsMapper settingsMapper;

    /**
     * Update social accounts
     */
    @Transactional
    @CacheEvict(value = "store", allEntries = true)
    public SocialAccountsResponse updateSocialAccounts(
            UUID storeId,
            UpdateSocialAccountsCommand command
    ) {
        log.info("Updating social accounts: storeId={}", storeId);

        SocialAccounts accounts = socialAccountsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSocialAccounts(storeId));

        if (command.facebook() != null) accounts.setFacebook(command.facebook());
        if (command.twitter() != null) accounts.setTwitter(command.twitter());
        if (command.instagram() != null) accounts.setInstagram(command.instagram());
        if (command.snapchat() != null) accounts.setSnapchat(command.snapchat());
        if (command.pinterest() != null) accounts.setPinterest(command.pinterest());
        if (command.foursquare() != null) accounts.setFoursquare(command.foursquare());
        if (command.tripadvisor() != null) accounts.setTripadvisor(command.tripadvisor());
        if (command.zomato() != null) accounts.setZomato(command.zomato());
        if (command.tiktok() != null) accounts.setTiktok(command.tiktok());

        SocialAccounts saved = socialAccountsRepository.save(accounts);
        return settingsMapper.toSocialAccountsResponse(saved);
    }

    /**
     * Get social accounts
     */
    public SocialAccountsResponse getSocialAccounts(UUID storeId) {
        log.debug("Fetching social accounts: storeId={}", storeId);
        
        SocialAccounts accounts = socialAccountsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSocialAccounts(storeId));
        return settingsMapper.toSocialAccountsResponse(accounts);
    }

    /**
     * Create default social accounts
     */
    private SocialAccounts createDefaultSocialAccounts(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));
        SocialAccounts socialAccounts = SocialAccounts.builder()
                .store(store)
                .build();
        return socialAccountsRepository.save(socialAccounts);
    }

    /**
     * Initialize default social accounts for a new store
     */
    @Transactional
    public void initializeDefaultSocialAccounts(UUID storeId) {
        log.info("Initializing default social accounts: storeId={}", storeId);
        createDefaultSocialAccounts(storeId);
        log.info("✅ Default social accounts initialized: storeId={}", storeId);
    }
}
