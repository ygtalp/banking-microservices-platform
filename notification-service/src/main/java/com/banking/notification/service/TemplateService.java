package com.banking.notification.service;

import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.NotificationTemplate;

import java.util.List;

public interface TemplateService {

    NotificationTemplate createTemplate(NotificationTemplate template);

    NotificationTemplate updateTemplate(Long id, NotificationTemplate template);

    NotificationTemplate getTemplate(String templateCode);

    NotificationTemplate getTemplateById(Long id);

    List<NotificationTemplate> getAllTemplates();

    List<NotificationTemplate> getTemplatesByChannel(NotificationChannel channel);

    void deleteTemplate(Long id);

    void activateTemplate(String templateCode);

    void deactivateTemplate(String templateCode);
}
