package com.banking.notification.repository;

import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    List<NotificationTemplate> findByIsActive(Boolean isActive);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.channel = :channel AND t.isActive = true")
    List<NotificationTemplate> findActiveTemplatesByChannel(@Param("channel") NotificationChannel channel);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.templateCode = :templateCode AND t.isActive = true")
    Optional<NotificationTemplate> findActiveTemplateByCode(@Param("templateCode") String templateCode);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.channel = :channel AND t.language = :language AND t.isActive = true")
    List<NotificationTemplate> findActiveTemplatesByChannelAndLanguage(
        @Param("channel") NotificationChannel channel,
        @Param("language") String language
    );

    boolean existsByTemplateCode(String templateCode);
}
