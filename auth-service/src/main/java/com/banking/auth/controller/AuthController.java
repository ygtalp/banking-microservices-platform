package com.banking.auth.controller;

import com.banking.auth.dto.*;
import com.banking.auth.security.JwtTokenProvider;
import com.banking.auth.service.AuthService;
import com.banking.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authentication and user management
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     * Public endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<LoginResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - email: {}", request.getEmail());
        ApiResponse<LoginResponse> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     * Public endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login - email: {}", request.getEmail());
        ApiResponse<LoginResponse> response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout user
     * Requires authentication
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        String email = getCurrentUserEmail();

        log.info("POST /auth/logout - email: {}", email);
        ApiResponse<Void> response = authService.logout(token, email);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh access token
     * Public endpoint
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /auth/refresh");
        ApiResponse<LoginResponse> response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user profile
     * Requires authentication
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        String email = getCurrentUserEmail();
        log.info("GET /auth/me - email: {}", email);
        ApiResponse<UserProfileResponse> response = userService.getUserProfileByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * Change password
     * Requires authentication
     */
    @PostMapping("/password/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = getCurrentUserEmail();
        log.info("POST /auth/password/change - email: {}", email);

        // Get userId from email
        ApiResponse<UserProfileResponse> userProfile = userService.getUserProfileByEmail(email);
        String userId = userProfile.getData().getUserId();

        ApiResponse<Void> response = userService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user profile by userId (admin only)
     * Requires ADMIN role
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable("userId") String userId) {
        log.info("GET /auth/users/{} - requested by: {}", userId, getCurrentUserEmail());
        ApiResponse<UserProfileResponse> response = userService.getUserProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Unlock user account (admin only)
     * Requires ADMIN role
     */
    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockAccount(
            @PathVariable("userId") String userId) {
        log.info("POST /auth/users/{}/unlock - requested by: {}", userId, getCurrentUserEmail());
        ApiResponse<Void> response = userService.unlockAccount(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all locked accounts (admin only)
     * Requires ADMIN role
     */
    @GetMapping("/users/locked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getLockedAccounts() {
        log.info("GET /auth/users/locked - requested by: {}", getCurrentUserEmail());
        ApiResponse<List<UserProfileResponse>> response = userService.getLockedAccounts();
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is healthy"));
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }

    /**
     * Get current authenticated user's email
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        throw new IllegalStateException("No authenticated user found");
    }
}
