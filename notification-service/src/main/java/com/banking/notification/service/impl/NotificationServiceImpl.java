package com.banking.notification.service.impl;

import com.banking.notification.exception.NotificationDeliveryException;
import com.banking.notification.exception.NotificationNotFoundException;
import com.banking.notification.model.*;
import com.banking.notification.repository.NotificationRepository;
import com.banking.notification.service.NotificationService;
import com.banking.notification.service.TemplateService;
import com.banking.notification.service.UserPreferenceService;
import com.banking.notification.handler.NotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final TemplateService templateService;
    private final UserPreferenceService userPreferenceService;
    private final Map<NotificationChannel, NotificationHandler> notificationHandlers;

    @Override
    @Transactional
    public Notification createNotification(String userId, String recipient,
                                          NotificationChannel channel,
                                          String templateCode,
                                          Map<String, String> parameters) {
        log.info("Creating notification for user: {}, channel: {}, template: {}",
                 userId, channel, templateCode);

        // Check user preferences
        UserPreference preference = userPreferenceService.getUserPreference(userId);
        if (!preference.isChannelEnabled(channel)) {
            log.warn("Channel {} is disabled for user: {}", channel, userId);
            throw new NotificationDeliveryException("Notification channel is disabled for user");
        }

        // Render template
        NotificationTemplate template = templateService.getTemplate(templateCode);
        String subject = template.renderSubject(parameters);
        String content = template.renderBody(parameters);

        // Create notification
        Notification notification = Notification.builder()
                .userId(userId)
                .recipient(recipient)
                .channel(channel)
                .priority(NotificationPriority.NORMAL)
                .templateCode(templateCode)
                .subject(subject)
                .content(content)
                .parameters(parameters)
                .status(NotificationStatus.PENDING)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: {}", saved.getNotificationId());

        return saved;
    }

    @Override
    @Transactional
    public Notification sendNotification(Notification notification) {
        log.info("Sending notification: {}", notification.getNotificationId());

        try {
            NotificationHandler handler = notificationHandlers.get(notification.getChannel());
            if (handler == null) {
                throw new NotificationDeliveryException(
                    "No handler found for channel: " + notification.getChannel());
            }

            String externalId = handler.send(notification);
            notification.setExternalId(externalId);
            notification.markAsSent();

            log.info("Notification sent successfully: {}", notification.getNotificationId());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getNotificationId(), e);
            notification.markAsFailed(e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Notification getNotification(String notificationId) {
        return notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                    "Notification not found: " + notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotificationsPaged(String userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findUnreadInAppNotifications(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(String userId) {
        return notificationRepository.countUnreadNotifications(userId);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId) {
        Notification notification = getNotification(notificationId);
        notification.markAsRead();
        notificationRepository.save(notification);
        log.info("Notification marked as read: {}", notificationId);
    }

    @Override
    @Transactional
    public void markAsSent(String notificationId) {
        Notification notification = getNotification(notificationId);
        notification.markAsSent();
        notificationRepository.save(notification);
        log.info("Notification marked as sent: {}", notificationId);
    }

    @Override
    @Transactional
    public void markAsFailed(String notificationId, String errorMessage) {
        Notification notification = getNotification(notificationId);
        notification.markAsFailed(errorMessage);
        notificationRepository.save(notification);
        log.warn("Notification marked as failed: {}, reason: {}", notificationId, errorMessage);
    }

    @Override
    @Transactional
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications...");

        List<Notification> failedNotifications = notificationRepository
                .findRetryableNotifications(NotificationStatus.FAILED);

        log.info("Found {} failed notifications to retry", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            if (notification.canRetry()) {
                notification.incrementRetryCount();
                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);

                try {
                    sendNotification(notification);
                } catch (Exception e) {
                    log.error("Retry failed for notification: {}",
                             notification.getNotificationId(), e);
                }
            }
        }
    }

    @Override
    @Transactional
    public void processScheduledNotifications() {
        log.info("Processing scheduled notifications...");

        List<Notification> scheduledNotifications = notificationRepository
                .findScheduledNotifications(NotificationStatus.PENDING, LocalDateTime.now());

        log.info("Found {} scheduled notifications to process",
                 scheduledNotifications.size());

        for (Notification notification : scheduledNotifications) {
            try {
                sendNotification(notification);
            } catch (Exception e) {
                log.error("Failed to send scheduled notification: {}",
                         notification.getNotificationId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void cancelNotification(String notificationId) {
        Notification notification = getNotification(notificationId);

        if (!notification.isPending()) {
            throw new IllegalStateException(
                "Cannot cancel notification with status: " + notification.getStatus());
        }

        notification.markAsCancelled();
        notificationRepository.save(notification);
        log.info("Notification cancelled: {}", notificationId);
    }
}
