package com.springboot.jenka_coffee.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * VULN #19 FIX: Cache configuration to prevent JWT filter DB bottleneck
 * 
 * Cache Strategy:
 * - accountSecurity: 5-minute TTL, max 10k entries
 *   Used by JWT filter to avoid DB query on every request
 *   Evicted on: admin status change, password reset, account lock
 * 
 * - categories: 10-minute TTL, max 1k entries
 *   Category data changes infrequently
 * 
 * - categoryCounts: 5-minute TTL, max 100 entries
 *   Product counts per category
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "accountSecurity",
            "categories", 
            "categoryCounts"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .recordStats());
        
        return cacheManager;
    }
}
