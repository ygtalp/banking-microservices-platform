package com.banking.notification.controller;

import com.banking.notification.dto.*;
import com.banking.notification.model.Notification;
import com.banking.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifications")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Sending notification to user: {}", request.getUserId());

        Notification notification = notificationService.createNotification(
                request.getUserId(),
                request.getRecipient(),
                request.getChannel(),
                request.getTemplateCode(),
                request.getParameters()
        );

        notification = notificationService.sendNotification(notification);

        NotificationResponse response = NotificationMapper.toResponse(notification);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent successfully", response));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(
            @PathVariable("notificationId") String notificationId) {

        log.info("Getting notification: {}", notificationId);

        Notification notification = notificationService.getNotification(notificationId);
        NotificationResponse response = NotificationMapper.toResponse(notification);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable("userId") String userId,
            Pageable pageable) {

        log.info("Getting notifications for user: {}", userId);

        Page<Notification> notifications = notificationService.getUserNotificationsPaged(userId, pageable);
        List<NotificationResponse> responses = notifications.getContent().stream()
                .map(NotificationMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @PathVariable("userId") String userId) {

        log.info("Getting unread notifications for user: {}", userId);

        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        List<NotificationResponse> responses = notifications.stream()
                .map(NotificationMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @PathVariable("userId") String userId) {

        log.info("Getting unread count for user: {}", userId);

        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable("notificationId") String notificationId) {

        log.info("Marking notification as read: {}", notificationId);

        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> cancelNotification(
            @PathVariable("notificationId") String notificationId) {

        log.info("Cancelling notification: {}", notificationId);

        notificationService.cancelNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification cancelled", null));
    }
}
