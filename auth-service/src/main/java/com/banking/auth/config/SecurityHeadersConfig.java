package com.banking.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Security Headers Configuration
 * Implements OWASP recommended security headers
 */
@Configuration
public class SecurityHeadersConfig {

    /**
     * Security Headers Filter
     * Adds security headers to all HTTP responses
     */
    @Bean
    public Filter securityHeadersFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                HttpServletResponse httpResponse = (HttpServletResponse) response;

                // Content Security Policy - Prevents XSS attacks
                httpResponse.setHeader("Content-Security-Policy",
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'");

                // Prevents clickjacking attacks
                httpResponse.setHeader("X-Frame-Options", "DENY");

                // Prevents MIME type sniffing
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");

                // XSS Protection (legacy browsers)
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

                // HTTP Strict Transport Security - Enforces HTTPS
                httpResponse.setHeader("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains; preload");

                // Referrer Policy - Controls information sent in Referer header
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

                // Permissions Policy (formerly Feature Policy)
                httpResponse.setHeader("Permissions-Policy",
                        "geolocation=(), microphone=(), camera=()");

                // Remove server information
                httpResponse.setHeader("Server", "");

                chain.doFilter(request, response);
            }
        };
    }
}
