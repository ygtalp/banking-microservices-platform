package com.banking.auth.service;

import com.banking.auth.dto.ApiResponse;
import com.banking.auth.dto.ChangePasswordRequest;
import com.banking.auth.dto.UserProfileResponse;
import com.banking.auth.event.UserPasswordChangedEvent;
import com.banking.auth.exception.InvalidPasswordException;
import com.banking.auth.exception.UserNotFoundException;
import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Get user profile by userId
     */
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfile(String userId) {
        log.info("Fetching profile for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        UserProfileResponse response = buildUserProfileResponse(user);

        return ApiResponse.success(response);
    }

    /**
     * Get user profile by email
     */
    @Transactional(readOnly = true)
    public ApiResponse<UserProfileResponse> getUserProfileByEmail(String email) {
        log.info("Fetching profile for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        UserProfileResponse response = buildUserProfileResponse(user);

        return ApiResponse.success(response);
    }

    /**
     * Change user password
     */
    @Transactional
    public ApiResponse<Void> changePassword(String userId, ChangePasswordRequest request) {
        log.info("Password change request for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Validate new password is different
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new InvalidPasswordException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password changed successfully for userId: {}", userId);

        // Publish event
        publishPasswordChangedEvent(user);

        return ApiResponse.success("Password changed successfully", null);
    }

    /**
     * Unlock user account (admin operation)
     */
    @Transactional
    public ApiResponse<Void> unlockAccount(String userId) {
        log.info("Unlock account request for userId: {}", userId);

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.getAccountLocked()) {
            return ApiResponse.success("Account is not locked", null);
        }

        user.unlock();
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Account unlocked successfully for userId: {}", userId);

        return ApiResponse.success("Account unlocked successfully", null);
    }

    /**
     * Get all locked accounts
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<UserProfileResponse>> getLockedAccounts() {
        log.info("Fetching all locked accounts");

        List<User> lockedUsers = userRepository.findByAccountLocked(true);

        List<UserProfileResponse> responses = lockedUsers.stream()
                .map(this::buildUserProfileResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(responses);
    }

    /**
     * Build UserProfileResponse from User entity
     */
    private UserProfileResponse buildUserProfileResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .roles(roleNames)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Publish UserPasswordChangedEvent to Kafka
     */
    private void publishPasswordChangedEvent(User user) {
        try {
            UserPasswordChangedEvent event = UserPasswordChangedEvent.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .changedAt(LocalDateTime.now())
                    .eventId(UUID.randomUUID().toString())
                    .build();

            kafkaTemplate.send("user-events", event);
            log.info("UserPasswordChangedEvent published for userId: {}", user.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserPasswordChangedEvent", e);
        }
    }
}
