package com.banking.notification.handler;

import com.banking.notification.model.Notification;

public interface NotificationHandler {

    String send(Notification notification);

    boolean supports(com.banking.notification.model.NotificationChannel channel);
}
