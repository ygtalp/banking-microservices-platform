package com.banking.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences", indexes = {
    @Index(name = "idx_user_preference_user_id", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    private String userId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "device_token", length = 500)
    private String deviceToken;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = true;

    @Column(name = "account_notifications", nullable = false)
    @Builder.Default
    private Boolean accountNotifications = true;

    @Column(name = "transfer_notifications", nullable = false)
    @Builder.Default
    private Boolean transferNotifications = true;

    @Column(name = "security_notifications", nullable = false)
    @Builder.Default
    private Boolean securityNotifications = true;

    @Column(name = "marketing_notifications", nullable = false)
    @Builder.Default
    private Boolean marketingNotifications = false;

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isChannelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> emailEnabled;
            case SMS -> smsEnabled;
            case PUSH -> pushEnabled;
            case IN_APP -> inAppEnabled;
        };
    }

    public String getRecipientForChannel(NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> email;
            case SMS -> phoneNumber;
            case PUSH -> deviceToken;
            case IN_APP -> userId;
        };
    }

    public boolean isNotificationTypeEnabled(String notificationType) {
        return switch (notificationType.toLowerCase()) {
            case "account" -> accountNotifications;
            case "transfer" -> transferNotifications;
            case "security" -> securityNotifications;
            case "marketing" -> marketingNotifications;
            default -> false;
        };
    }

    public boolean canReceiveNotification(NotificationChannel channel, String notificationType) {
        return isChannelEnabled(channel) && isNotificationTypeEnabled(notificationType);
    }
}
