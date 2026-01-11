package com.banking.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * OTP (One-Time Password) Service
 * Generates and verifies OTP codes for SMS and Email MFA
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Generate OTP code
     *
     * @param userId User ID
     * @param type   OTP type (SMS or EMAIL)
     * @return 6-digit OTP code
     */
    public String generateOtp(String userId, String type) {
        String otp = generateNumericOtp(OTP_LENGTH);
        String redisKey = buildRedisKey(userId, type);

        // Store OTP in Redis with 5-minute expiry
        redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

        log.info("Generated OTP for user: {}, type: {}", userId, type);
        return otp;
    }

    /**
     * Verify OTP code
     *
     * @param userId User ID
     * @param type   OTP type (SMS or EMAIL)
     * @param code   OTP code to verify
     * @return true if code is valid
     */
    public boolean verifyOtp(String userId, String type, String code) {
        if (code == null || code.length() != OTP_LENGTH) {
            log.warn("Invalid OTP format for user: {}", userId);
            return false;
        }

        String redisKey = buildRedisKey(userId, type);
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            log.warn("OTP expired or not found for user: {}, type: {}", userId, type);
            return false;
        }

        boolean isValid = storedOtp.equals(code);

        if (isValid) {
            // Delete OTP after successful verification (one-time use)
            redisTemplate.delete(redisKey);
            log.info("OTP verified successfully for user: {}, type: {}", userId, type);
        } else {
            log.warn("OTP verification failed for user: {}, type: {}", userId, type);
        }

        return isValid;
    }

    /**
     * Invalidate OTP (e.g., after max attempts)
     *
     * @param userId User ID
     * @param type   OTP type
     */
    public void invalidateOtp(String userId, String type) {
        String redisKey = buildRedisKey(userId, type);
        redisTemplate.delete(redisKey);
        log.info("Invalidated OTP for user: {}, type: {}", userId, type);
    }

    /**
     * Check if OTP exists and is not expired
     *
     * @param userId User ID
     * @param type   OTP type
     * @return true if OTP exists
     */
    public boolean hasActiveOtp(String userId, String type) {
        String redisKey = buildRedisKey(userId, type);
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
    }

    /**
     * Get remaining TTL for OTP
     *
     * @param userId User ID
     * @param type   OTP type
     * @return TTL in seconds, or -1 if not found
     */
    public long getOtpTtl(String userId, String type) {
        String redisKey = buildRedisKey(userId, type);
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    /**
     * Generate numeric OTP
     */
    private String generateNumericOtp(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(RANDOM.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Build Redis key for OTP storage
     */
    private String buildRedisKey(String userId, String type) {
        return String.format("otp:%s:%s", type.toLowerCase(), userId);
    }
}
