package com.banking.notification.dto;

import com.banking.notification.model.Notification;

public class NotificationMapper {

    public static NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .recipient(notification.getRecipient())
                .channel(notification.getChannel())
                .priority(notification.getPriority())
                .templateCode(notification.getTemplateCode())
                .subject(notification.getSubject())
                .content(notification.getContent())
                .parameters(notification.getParameters())
                .status(notification.getStatus())
                .errorMessage(notification.getErrorMessage())
                .retryCount(notification.getRetryCount())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .externalId(notification.getExternalId())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
