package com.banking.auth.security;

import com.banking.auth.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generate access token from authentication
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(
                userDetails.getUsername(),
                getRoles(userDetails),
                jwtConfig.getAccessTokenExpiration(),
                "access"
        );
    }

    /**
     * Generate refresh token from authentication
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(
                userDetails.getUsername(),
                getRoles(userDetails),
                jwtConfig.getRefreshTokenExpiration(),
                "refresh"
        );
    }

    /**
     * Generate access token from username (used after refresh)
     */
    public String generateAccessTokenFromUsername(String username, List<String> roles) {
        return generateToken(
                username,
                roles,
                jwtConfig.getAccessTokenExpiration(),
                "access"
        );
    }

    /**
     * Generate refresh token from username (used after refresh)
     */
    public String generateRefreshTokenFromUsername(String username, List<String> roles) {
        return generateToken(
                username,
                roles,
                jwtConfig.getRefreshTokenExpiration(),
                "refresh"
        );
    }

    /**
     * Core token generation method
     */
    private String generateToken(String username, List<String> roles, Long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .claim("type", tokenType)
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    /**
     * Extract roles from JWT token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Extract token type from JWT token
     */
    public String getTokenType(String token) {
        Claims claims = getClaims(token);
        return (String) claims.get("type");
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * Get token expiration date
     */
    public Date getExpirationDate(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration();
    }

    /**
     * Get all claims from token
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract roles from UserDetails
     */
    private List<String> getRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}
