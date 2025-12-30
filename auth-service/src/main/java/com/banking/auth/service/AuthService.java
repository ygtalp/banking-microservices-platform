package com.banking.auth.service;

import com.banking.auth.config.JwtConfig;
import com.banking.auth.dto.*;
import com.banking.auth.event.UserLoggedInEvent;
import com.banking.auth.event.UserLoggedOutEvent;
import com.banking.auth.event.UserRegisteredEvent;
import com.banking.auth.exception.*;
import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JwtConfig jwtConfig;

    /**
     * Register a new user
     */
    @Transactional
    public ApiResponse<LoginResponse> register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Get CUSTOMER role (default for new users)
        Role customerRole = roleRepository.findByRoleName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default CUSTOMER role not found"));

        // Create new user
        User user = User.builder()
                .userId(generateUserId())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: userId={}", user.getUserId());

        // Publish UserRegisteredEvent
        publishUserRegisteredEvent(user);

        // Generate tokens
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());
        String accessToken = jwtTokenProvider.generateAccessTokenFromUsername(user.getEmail(), roleNames);
        String refreshToken = jwtTokenProvider.generateRefreshTokenFromUsername(user.getEmail(), roleNames);

        // Build response
        LoginResponse loginResponse = buildLoginResponse(user, accessToken, refreshToken);

        return ApiResponse.success("User registered successfully", loginResponse);
    }

    /**
     * Authenticate user and generate tokens
     */
    @Transactional
    public ApiResponse<LoginResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        // Check if account is locked
        if (user.getAccountLocked()) {
            log.warn("Login attempt for locked account: {}", request.getEmail());
            throw new AccountLockedException();
        }

        // Check if account is active
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login attempt for inactive account: {} (status: {})",
                    request.getEmail(), user.getStatus());
            throw new AuthException("Account is not active", "ACCOUNT_INACTIVE");
        }

        // Authenticate
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedAttempts();
                user.setUpdatedAt(LocalDateTime.now());
            }

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User logged in successfully: userId={}", user.getUserId());

            // Publish UserLoggedInEvent
            publishUserLoggedInEvent(user);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // Build response
            LoginResponse loginResponse = buildLoginResponse(user, accessToken, refreshToken);

            return ApiResponse.success("Login successful", loginResponse);

        } catch (BadCredentialsException ex) {
            // Increment failed attempts
            user.incrementFailedAttempts();
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.warn("Failed login attempt for email: {} (attempt: {})",
                    request.getEmail(), user.getFailedLoginAttempts());

            if (user.getAccountLocked()) {
                throw new AccountLockedException();
            }

            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Logout user by blacklisting token
     */
    @Transactional
    public ApiResponse<Void> logout(String token, String email) {
        log.info("Logout request for email: {}", email);

        // Blacklist the token
        tokenBlacklistService.blacklistToken(token);

        // Find user for event
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Publish UserLoggedOutEvent
        publishUserLoggedOutEvent(user);

        log.info("User logged out successfully: userId={}", user.getUserId());

        return ApiResponse.success("Logout successful", null);
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public ApiResponse<LoginResponse> refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        log.info("Token refresh request");

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Check if token is blacklisted
        if (tokenBlacklistService.isTokenBlacklisted(refreshToken)) {
            throw new TokenBlacklistedException("Refresh token has been blacklisted");
        }

        // Check if token type is refresh
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        // Extract user information
        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        List<String> roles = jwtTokenProvider.getRolesFromToken(refreshToken);

        // Find user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Check if account is still active and not locked
        if (user.getAccountLocked()) {
            throw new AccountLockedException();
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException("Account is not active", "ACCOUNT_INACTIVE");
        }

        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessTokenFromUsername(email, roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenFromUsername(email, roles);

        // Blacklist old refresh token
        tokenBlacklistService.blacklistToken(refreshToken);

        log.info("Tokens refreshed successfully for userId: {}", user.getUserId());

        // Build response
        LoginResponse loginResponse = buildLoginResponse(user, newAccessToken, newRefreshToken);

        return ApiResponse.success("Token refreshed successfully", loginResponse);
    }

    /**
     * Generate unique user ID
     */
    private String generateUserId() {
        return "USR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * Build login response with tokens
     */
    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(roleNames)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresAt(now.plusSeconds(jwtConfig.getAccessTokenExpiration() / 1000))
                .refreshTokenExpiresAt(now.plusSeconds(jwtConfig.getRefreshTokenExpiration() / 1000))
                .tokenType("Bearer")
                .build();
    }

    /**
     * Publish UserRegisteredEvent to Kafka
     */
    private void publishUserRegisteredEvent(User user) {
        try {
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());

            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .roles(roleNames)
                    .registeredAt(user.getCreatedAt())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            kafkaTemplate.send("user-events", event);
            log.info("UserRegisteredEvent published for userId: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent", e);
        }
    }

    /**
     * Publish UserLoggedInEvent to Kafka
     */
    private void publishUserLoggedInEvent(User user) {
        try {
            UserLoggedInEvent event = UserLoggedInEvent.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .loginAt(user.getLastLoginAt())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            kafkaTemplate.send("user-events", event);
            log.info("UserLoggedInEvent published for userId: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserLoggedInEvent", e);
        }
    }

    /**
     * Publish UserLoggedOutEvent to Kafka
     */
    private void publishUserLoggedOutEvent(User user) {
        try {
            UserLoggedOutEvent event = UserLoggedOutEvent.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .logoutAt(LocalDateTime.now())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            kafkaTemplate.send("user-events", event);
            log.info("UserLoggedOutEvent published for userId: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserLoggedOutEvent", e);
        }
    }
}
