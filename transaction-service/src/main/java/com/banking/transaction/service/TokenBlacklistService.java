package com.banking.transaction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist Service for Transaction Service
 * Checks if JWT tokens are blacklisted (after logout in Auth Service)
 * NOTE: Only Auth Service adds tokens to blacklist
 *       This service only reads blacklist status
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    /**
     * Check if token is blacklisted
     * IMPORTANT: Graceful degradation - if Redis is down, allow the request
     * This prevents Redis outages from blocking all API requests
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            // Graceful degradation: if Redis is unavailable, allow the request
            log.error("Redis unavailable - allowing request (graceful degradation): {}", e.getMessage());
            return false;  // Fail-open
        }
    }

    /**
     * Remove token from blacklist (useful for testing)
     */
    public void removeFromBlacklist(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            redisTemplate.delete(key);
            log.debug("Token removed from blacklist");
        } catch (Exception e) {
            log.error("Error removing token from blacklist: {}", e.getMessage());
        }
    }

    /**
     * Get remaining TTL for blacklisted token (for monitoring)
     */
    public Long getTokenBlacklistTTL(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            return redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Error getting token blacklist TTL: {}", e.getMessage());
            return -1L;
        }
    }
}
