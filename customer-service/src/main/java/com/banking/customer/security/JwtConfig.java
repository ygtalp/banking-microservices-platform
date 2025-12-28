package com.banking.customer.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {

    /**
     * Secret key for signing JWT tokens
     * IMPORTANT: Must be set via environment variable in production
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds (default: 15 minutes)
     */
    private Long accessTokenExpiration = 900000L;

    /**
     * Refresh token expiration time in milliseconds (default: 7 days)
     */
    private Long refreshTokenExpiration = 604800000L;

    /**
     * Token issuer (identifies who issued the token)
     */
    private String issuer = "banking-platform";
}
