package com.banking.notification.controller;

import com.banking.notification.dto.ApiResponse;
import com.banking.notification.dto.UpdateUserPreferenceRequest;
import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.UserPreference;
import com.banking.notification.service.UserPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/preferences")
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreference>> getUserPreference(
            @PathVariable("userId") String userId) {

        log.info("Getting preference for user: {}", userId);

        UserPreference preference = userPreferenceService.getOrCreateUserPreference(userId);
        return ResponseEntity.ok(ApiResponse.success(preference));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreference>> updateUserPreference(
            @PathVariable("userId") String userId,
            @Valid @RequestBody UpdateUserPreferenceRequest request) {

        log.info("Updating preference for user: {}", userId);

        UserPreference preference = UserPreference.builder()
                .userId(userId)
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .deviceToken(request.getDeviceToken())
                .emailEnabled(request.getEmailEnabled())
                .smsEnabled(request.getSmsEnabled())
                .pushEnabled(request.getPushEnabled())
                .inAppEnabled(request.getInAppEnabled())
                .accountNotifications(request.getAccountNotifications())
                .transferNotifications(request.getTransferNotifications())
                .securityNotifications(request.getSecurityNotifications())
                .marketingNotifications(request.getMarketingNotifications())
                .language(request.getLanguage())
                .timezone(request.getTimezone())
                .build();

        UserPreference updated = userPreferenceService.updateUserPreference(userId, preference);
        return ResponseEntity.ok(ApiResponse.success("Preference updated successfully", updated));
    }

    @PutMapping("/{userId}/channel/{channel}")
    public ResponseEntity<ApiResponse<Void>> updateChannelPreference(
            @PathVariable("userId") String userId,
            @PathVariable("channel") NotificationChannel channel,
            @RequestParam("enabled") boolean enabled) {

        log.info("Updating {} channel preference for user: {} to: {}", channel, userId, enabled);

        userPreferenceService.updateChannelPreference(userId, channel, enabled);
        return ResponseEntity.ok(ApiResponse.success("Channel preference updated", null));
    }

    @PutMapping("/{userId}/type/{notificationType}")
    public ResponseEntity<ApiResponse<Void>> updateNotificationTypePreference(
            @PathVariable("userId") String userId,
            @PathVariable("notificationType") String notificationType,
            @RequestParam("enabled") boolean enabled) {

        log.info("Updating {} type preference for user: {} to: {}",
                 notificationType, userId, enabled);

        userPreferenceService.updateNotificationTypePreference(userId, notificationType, enabled);
        return ResponseEntity.ok(ApiResponse.success("Notification type preference updated", null));
    }
}
