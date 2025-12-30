package com.banking.notification.handler;

import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InAppNotificationHandler implements NotificationHandler {

    @Override
    public String send(Notification notification) {
        // For in-app notifications, we just store them in the database
        // They are already persisted by NotificationService
        log.info("In-app notification created for user: {}", notification.getUserId());

        String externalId = "IN_APP-" + notification.getNotificationId();
        log.info("In-app notification ready, externalId: {}", externalId);

        return externalId;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }
}
