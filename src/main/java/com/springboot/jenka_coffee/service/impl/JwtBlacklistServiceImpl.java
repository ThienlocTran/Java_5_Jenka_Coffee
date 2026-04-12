package com.springboot.jenka_coffee.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.springboot.jenka_coffee.service.JwtBlacklistService;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT Blacklist Implementation using Caffeine Cache
 * VULN-TOKEN-001 FIX: Token bị blacklist khi logout
 * BUG-46 FIX: Custom expiry per token to prevent memory leak
 */
@Service
public class JwtBlacklistServiceImpl implements JwtBlacklistService {
    
    /**
     * BUG-46 FIX: Use custom Expiry to set TTL per token based on actual expiration time
     * 
     * PROBLEM: expireAfterWrite(7 days) applies to ALL tokens regardless of their actual TTL
     * A token with 1 second remaining gets cached for 7 days = massive memory waste
     * 
     * SOLUTION: Use Expiry interface to calculate TTL dynamically per token
     * Each token is cached only until its actual expiration time
     */
    private final Cache<String, Long> blacklist = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, Long>() {
                @Override
                public long expireAfterCreate(String token, Long expirationTimeMs, long currentTime) {
                    // Calculate TTL in nanoseconds (Caffeine uses nanos)
                    long ttlMs = expirationTimeMs - System.currentTimeMillis();
                    if (ttlMs <= 0) {
                        // Token already expired, cache for minimal time (1 second)
                        return TimeUnit.SECONDS.toNanos(1);
                    }
                    // Cap at 7 days to prevent extremely long-lived entries
                    long maxTtlMs = TimeUnit.DAYS.toMillis(7);
                    ttlMs = Math.min(ttlMs, maxTtlMs);
                    return TimeUnit.MILLISECONDS.toNanos(ttlMs);
                }
                
                @Override
                public long expireAfterUpdate(String token, Long expirationTimeMs, 
                                             long currentTime, @NonNegative long currentDuration) {
                    // Not used - tokens are never updated, only added
                    return currentDuration;
                }
                
                @Override
                public long expireAfterRead(String token, Long expirationTimeMs, 
                                           long currentTime, @NonNegative long currentDuration) {
                    // Not used - read doesn't affect expiration
                    return currentDuration;
                }
            })
            .maximumSize(100_000) // Giới hạn 100k tokens để tránh OOM
            .build();
    
    @Override
    public void blacklistToken(String token, long expirationTimeMs) {
        if (token == null || token.isBlank()) return;
        
        // Tính thời gian còn lại đến khi token hết hạn
        long ttlMs = expirationTimeMs - System.currentTimeMillis();
        
        if (ttlMs > 0) {
            // Store expiration time as value - used by Expiry to calculate TTL
            blacklist.put(token, expirationTimeMs);
        }
        // If token already expired, don't blacklist (no point wasting memory)
    }
    
    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) return false;
        return blacklist.getIfPresent(token) != null;
    }
}
