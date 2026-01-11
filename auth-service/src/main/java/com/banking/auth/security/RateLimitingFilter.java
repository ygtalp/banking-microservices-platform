package com.banking.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter
 * Implements Token Bucket algorithm using Redis
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    // Rate limit: 100 requests per minute per IP
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int WINDOW_SIZE_MINUTES = 1;

    // Rate limit: 10 requests per minute for login endpoint
    private static final int MAX_LOGIN_REQUESTS = 10;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        // Apply stricter rate limiting for auth endpoints
        int maxRequests = MAX_REQUESTS_PER_MINUTE;
        if (requestUri.contains("/auth/login") || requestUri.contains("/auth/register")) {
            maxRequests = MAX_LOGIN_REQUESTS;
        }

        String redisKey = "rate_limit:" + clientIp + ":" + requestUri;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(redisKey);

            if (currentCount == null) {
                currentCount = 1L;
            }

            // Set expiry on first request
            if (currentCount == 1) {
                redisTemplate.expire(redisKey, WINDOW_SIZE_MINUTES, TimeUnit.MINUTES);
            }

            // Check if rate limit exceeded
            if (currentCount > maxRequests) {
                log.warn("Rate limit exceeded for IP: {}, URI: {}, Count: {}",
                        clientIp, requestUri, currentCount);

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{" +
                        "\"status\": \"error\"," +
                        "\"message\": \"Too many requests. Please try again later.\"," +
                        "\"data\": null" +
                        "}");
                return;
            }

            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequests));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(maxRequests - currentCount));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() + (WINDOW_SIZE_MINUTES * 60 * 1000)));

        } catch (Exception e) {
            log.error("Error in rate limiting filter", e);
            // On Redis failure, allow request (fail-open approach)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
