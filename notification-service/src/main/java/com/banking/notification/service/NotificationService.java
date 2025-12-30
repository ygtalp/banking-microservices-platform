package com.banking.notification.service;

import com.banking.notification.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    Notification createNotification(String userId, String recipient, NotificationChannel channel,
                                   String templateCode, Map<String, String> parameters);

    Notification sendNotification(Notification notification);

    Notification getNotification(String notificationId);

    List<Notification> getUserNotifications(String userId);

    Page<Notification> getUserNotificationsPaged(String userId, Pageable pageable);

    List<Notification> getUnreadNotifications(String userId);

    Long getUnreadCount(String userId);

    void markAsRead(String notificationId);

    void markAsSent(String notificationId);

    void markAsFailed(String notificationId, String errorMessage);

    void retryFailedNotifications();

    void processScheduledNotifications();

    void cancelNotification(String notificationId);
}
