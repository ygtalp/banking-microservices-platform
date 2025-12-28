package com.banking.auth.service;

import com.banking.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    /**
     * Add token to blacklist with TTL = token expiration time
     */
    public void blacklistToken(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            Date expirationDate = jwtTokenProvider.getExpirationDate(token);
            long ttlMillis = expirationDate.getTime() - System.currentTimeMillis();

            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(key, "blacklisted", ttlMillis, TimeUnit.MILLISECONDS);
                log.debug("Token blacklisted successfully with TTL: {} ms", ttlMillis);
            } else {
                log.warn("Token already expired, not adding to blacklist");
            }
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Error checking token blacklist status: {}", e.getMessage());
            // Fail-safe: if Redis is down, deny access
            return true;
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
