package com.banking.notification.handler;

import com.banking.notification.exception.NotificationDeliveryException;
import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationHandler implements NotificationHandler {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Override
    public String send(Notification notification) {
        if (!emailEnabled) {
            log.warn("Email notifications are disabled");
            return "EMAIL_DISABLED";
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(notification.getRecipient());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getContent(), true);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", notification.getRecipient());
            return "EMAIL-" + System.currentTimeMillis();

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", notification.getRecipient(), e);
            throw new NotificationDeliveryException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email", e);
            throw new NotificationDeliveryException("Unexpected error sending email", e);
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }
}
