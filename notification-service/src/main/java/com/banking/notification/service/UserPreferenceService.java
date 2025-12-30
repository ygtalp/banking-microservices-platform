package com.banking.notification.service;

import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.UserPreference;

public interface UserPreferenceService {

    UserPreference createUserPreference(UserPreference preference);

    UserPreference updateUserPreference(String userId, UserPreference preference);

    UserPreference getUserPreference(String userId);

    UserPreference getOrCreateUserPreference(String userId);

    void updateChannelPreference(String userId, NotificationChannel channel, boolean enabled);

    void updateNotificationTypePreference(String userId, String notificationType, boolean enabled);

    void deleteUserPreference(String userId);
}
