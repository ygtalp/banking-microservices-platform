package com.banking.notification.handler;

import com.banking.notification.exception.NotificationDeliveryException;
import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationHandler implements NotificationHandler {

    @Value("${notification.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${notification.push.firebase-key:}")
    private String firebaseKey;

    @Override
    public String send(Notification notification) {
        if (!pushEnabled) {
            log.warn("Push notifications are disabled");
            return "PUSH_DISABLED";
        }

        try {
            // Mock Push sending - in production, integrate with Firebase Cloud Messaging
            log.info("Sending Push notification to device token: {}",
                     notification.getRecipient());
            log.info("Push Title: {}", notification.getSubject());
            log.info("Push Body: {}", notification.getContent());

            // Simulate push sending
            String externalId = "PUSH-" + System.currentTimeMillis();
            log.info("Push notification sent successfully, externalId: {}", externalId);

            return externalId;

        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            throw new NotificationDeliveryException("Failed to send push notification", e);
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }
}
