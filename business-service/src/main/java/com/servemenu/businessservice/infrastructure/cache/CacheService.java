package com.servemenu.businessservice.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final CacheManager cacheManager;

    /**
     * Evict all caches
     */
    public void evictAllCaches() {
        log.info("Evicting all caches");
        cacheManager.getCacheNames()
                .forEach(cacheName -> {
                    Cache cache = cacheManager.getCache(cacheName);
                    cache.clear();
                });
    }

    /**
     * Evict specific cache
     */
    public void evictCache(String cacheName) {
        log.info("Evicting cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        cache.clear();
    }

    /**
     * Evict cache by key
     */
    public void evictCacheByKey(String cacheName, Object key) {
        log.debug("Evicting cache entry: cache={}, key={}", cacheName, key);
        Cache cache = cacheManager.getCache(cacheName);
        cache.evict(key);
    }
}
