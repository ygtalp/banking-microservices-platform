package com.banking.notification.service.impl;

import com.banking.notification.exception.TemplateNotFoundException;
import com.banking.notification.model.NotificationChannel;
import com.banking.notification.model.NotificationTemplate;
import com.banking.notification.repository.NotificationTemplateRepository;
import com.banking.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final NotificationTemplateRepository templateRepository;

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        log.info("Creating template: {}", template.getTemplateCode());

        if (templateRepository.existsByTemplateCode(template.getTemplateCode())) {
            throw new IllegalArgumentException(
                "Template already exists: " + template.getTemplateCode());
        }

        NotificationTemplate saved = templateRepository.save(template);
        log.info("Template created: {}", saved.getTemplateCode());

        return saved;
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public NotificationTemplate updateTemplate(Long id, NotificationTemplate template) {
        log.info("Updating template: {}", id);

        NotificationTemplate existing = getTemplateById(id);
        existing.setTemplateName(template.getTemplateName());
        existing.setDescription(template.getDescription());
        existing.setSubjectTemplate(template.getSubjectTemplate());
        existing.setBodyTemplate(template.getBodyTemplate());
        existing.setLanguage(template.getLanguage());
        existing.setIsActive(template.getIsActive());

        NotificationTemplate updated = templateRepository.save(existing);
        log.info("Template updated: {}", updated.getTemplateCode());

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "templates", key = "#templateCode")
    public NotificationTemplate getTemplate(String templateCode) {
        return templateRepository.findActiveTemplateByCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException(
                    "Active template not found: " + templateCode));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException(
                    "Template not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplate> getTemplatesByChannel(NotificationChannel channel) {
        return templateRepository.findActiveTemplatesByChannel(channel);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public void deleteTemplate(Long id) {
        log.info("Deleting template: {}", id);

        if (!templateRepository.existsById(id)) {
            throw new TemplateNotFoundException("Template not found with id: " + id);
        }

        templateRepository.deleteById(id);
        log.info("Template deleted: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public void activateTemplate(String templateCode) {
        log.info("Activating template: {}", templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException(
                    "Template not found: " + templateCode));

        template.setIsActive(true);
        templateRepository.save(template);

        log.info("Template activated: {}", templateCode);
    }

    @Override
    @Transactional
    @CacheEvict(value = "templates", allEntries = true)
    public void deactivateTemplate(String templateCode) {
        log.info("Deactivating template: {}", templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new TemplateNotFoundException(
                    "Template not found: " + templateCode));

        template.setIsActive(false);
        templateRepository.save(template);

        log.info("Template deactivated: {}", templateCode);
    }
}
