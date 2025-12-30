package com.banking.notification.service.impl;

import com.banking.notification.exception.UserPreferenceNotFoundException;
import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.UserPreference;
import com.banking.notification.repository.UserPreferenceRepository;
import com.banking.notification.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;

    @Override
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#preference.userId")
    public UserPreference createUserPreference(UserPreference preference) {
        log.info("Creating user preference for user: {}", preference.getUserId());

        if (preferenceRepository.existsByUserId(preference.getUserId())) {
            throw new IllegalArgumentException(
                "User preference already exists for user: " + preference.getUserId());
        }

        UserPreference saved = preferenceRepository.save(preference);
        log.info("User preference created for user: {}", saved.getUserId());

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userId")
    public UserPreference updateUserPreference(String userId, UserPreference preference) {
        log.info("Updating user preference for user: {}", userId);

        UserPreference existing = getUserPreference(userId);
        existing.setEmail(preference.getEmail());
        existing.setPhoneNumber(preference.getPhoneNumber());
        existing.setDeviceToken(preference.getDeviceToken());
        existing.setEmailEnabled(preference.getEmailEnabled());
        existing.setSmsEnabled(preference.getSmsEnabled());
        existing.setPushEnabled(preference.getPushEnabled());
        existing.setInAppEnabled(preference.getInAppEnabled());
        existing.setAccountNotifications(preference.getAccountNotifications());
        existing.setTransferNotifications(preference.getTransferNotifications());
        existing.setSecurityNotifications(preference.getSecurityNotifications());
        existing.setMarketingNotifications(preference.getMarketingNotifications());
        existing.setLanguage(preference.getLanguage());
        existing.setTimezone(preference.getTimezone());

        UserPreference updated = preferenceRepository.save(existing);
        log.info("User preference updated for user: {}", updated.getUserId());

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userPreferences", key = "#userId")
    public UserPreference getUserPreference(String userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPreferenceNotFoundException(
                    "User preference not found for user: " + userId));
    }

    @Override
    @Transactional
    public UserPreference getOrCreateUserPreference(String userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating default preference for user: {}", userId);
                    UserPreference defaultPreference = UserPreference.builder()
                            .userId(userId)
                            .build();
                    return preferenceRepository.save(defaultPreference);
                });
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void updateChannelPreference(String userId, NotificationChannel channel,
                                       boolean enabled) {
        log.info("Updating {} channel preference for user: {} to: {}",
                 channel, userId, enabled);

        UserPreference preference = getUserPreference(userId);

        switch (channel) {
            case EMAIL -> preference.setEmailEnabled(enabled);
            case SMS -> preference.setSmsEnabled(enabled);
            case PUSH -> preference.setPushEnabled(enabled);
            case IN_APP -> preference.setInAppEnabled(enabled);
        }

        preferenceRepository.save(preference);
        log.info("Channel preference updated for user: {}", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void updateNotificationTypePreference(String userId, String notificationType,
                                                boolean enabled) {
        log.info("Updating {} notification type preference for user: {} to: {}",
                 notificationType, userId, enabled);

        UserPreference preference = getUserPreference(userId);

        switch (notificationType.toLowerCase()) {
            case "account" -> preference.setAccountNotifications(enabled);
            case "transfer" -> preference.setTransferNotifications(enabled);
            case "security" -> preference.setSecurityNotifications(enabled);
            case "marketing" -> preference.setMarketingNotifications(enabled);
            default -> throw new IllegalArgumentException(
                "Unknown notification type: " + notificationType);
        }

        preferenceRepository.save(preference);
        log.info("Notification type preference updated for user: {}", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userPreferences", key = "#userId")
    public void deleteUserPreference(String userId) {
        log.info("Deleting user preference for user: {}", userId);

        UserPreference preference = getUserPreference(userId);
        preferenceRepository.delete(preference);

        log.info("User preference deleted for user: {}", userId);
    }
}
