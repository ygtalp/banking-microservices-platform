package com.banking.notification.dto;

import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.NotificationPriority;
import com.banking.notification.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private String notificationId;
    private String userId;
    private String recipient;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private String templateCode;
    private String subject;
    private String content;
    private Map<String, String> parameters;
    private NotificationStatus status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
