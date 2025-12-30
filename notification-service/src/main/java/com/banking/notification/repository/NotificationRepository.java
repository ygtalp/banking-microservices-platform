package com.banking.notification.repository;

import com.banking.notification.model.Notification;
import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(String notificationId);

    List<Notification> findByUserId(String userId);

    Page<Notification> findByUserId(String userId, Pageable pageable);

    List<Notification> findByStatus(NotificationStatus status);

    Page<Notification> findByUserIdAndStatus(String userId, NotificationStatus status, Pageable pageable);

    List<Notification> findByChannel(NotificationChannel channel);

    Page<Notification> findByUserIdAndChannel(String userId, NotificationChannel channel, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < n.maxRetries")
    List<Notification> findRetryableNotifications(@Param("status") NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.scheduledAt <= :scheduledAt")
    List<Notification> findScheduledNotifications(
        @Param("status") NotificationStatus status,
        @Param("scheduledAt") LocalDateTime scheduledAt
    );

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL AND n.channel = 'IN_APP' ORDER BY n.createdAt DESC")
    List<Notification> findUnreadInAppNotifications(@Param("userId") String userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL AND n.channel = 'IN_APP'")
    Long countUnreadNotifications(@Param("userId") String userId);

    @Query("SELECT n FROM Notification n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    List<Notification> findNotificationsBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.status = :status AND n.createdAt >= :since")
    Long countByStatusSince(
        @Param("status") NotificationStatus status,
        @Param("since") LocalDateTime since
    );

    void deleteByCreatedAtBefore(LocalDateTime createdAt);
}
