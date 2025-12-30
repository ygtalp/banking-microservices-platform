package com.banking.notification.config;

import com.banking.notification.handler.*;
import com.banking.notification.model.NotificationChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class NotificationHandlerConfig {

    @Bean
    public Map<NotificationChannel, NotificationHandler> notificationHandlers(
            EmailNotificationHandler emailHandler,
            SmsNotificationHandler smsHandler,
            PushNotificationHandler pushHandler,
            InAppNotificationHandler inAppHandler) {

        Map<NotificationChannel, NotificationHandler> handlers = new HashMap<>();
        handlers.put(NotificationChannel.EMAIL, emailHandler);
        handlers.put(NotificationChannel.SMS, smsHandler);
        handlers.put(NotificationChannel.PUSH, pushHandler);
        handlers.put(NotificationChannel.IN_APP, inAppHandler);

        return handlers;
    }
}
