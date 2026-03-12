package com.retailmanagement.modules.notification.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.notification.dto.request.TemplateRequest;
import com.retailmanagement.modules.notification.dto.response.TemplateResponse;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.mapper.TemplateMapper;
import com.retailmanagement.modules.notification.model.NotificationTemplate;
import com.retailmanagement.modules.notification.repository.NotificationTemplateRepository;
import com.retailmanagement.modules.notification.service.TemplateService;
import com.retailmanagement.common.utils.TemplateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final TemplateParser templateParser;

    @Override
    public TemplateResponse createTemplate(TemplateRequest request) {
        log.info("Creating new notification template: {}", request.getTemplateCode());

        // Check if template code already exists
        if (templateRepository.existsByTemplateCode(request.getTemplateCode())) {
            throw new BusinessException("Template code already exists: " + request.getTemplateCode());
        }

        NotificationTemplate template = templateMapper.toEntity(request);
        template.setCreatedBy("SYSTEM");

        NotificationTemplate savedTemplate = templateRepository.save(template);
        log.info("Notification template created successfully with ID: {}", savedTemplate.getId());

        return templateMapper.toResponse(savedTemplate);
    }

    @Override
    public TemplateResponse updateTemplate(Long id, TemplateRequest request) {
        log.info("Updating notification template with ID: {}", id);

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        // Check template code uniqueness if changed
        if (!template.getTemplateCode().equals(request.getTemplateCode()) &&
                templateRepository.existsByTemplateCode(request.getTemplateCode())) {
            throw new BusinessException("Template code already exists: " + request.getTemplateCode());
        }

        // Update fields
        template.setTemplateCode(request.getTemplateCode());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setType(request.getType());
        template.setChannel(request.getChannel());
        template.setSubject(request.getSubject());
        template.setContent(request.getContent());
        template.setContentHtml(request.getContentHtml());
        template.setSmsContent(request.getSmsContent());
        template.setPushTitle(request.getPushTitle());
        template.setPushContent(request.getPushContent());
        template.setPlaceholders(request.getPlaceholders());
        template.setIsActive(request.getIsActive());

        NotificationTemplate updatedTemplate = templateRepository.save(template);
        log.info("Notification template updated successfully with ID: {}", updatedTemplate.getId());

        return templateMapper.toResponse(updatedTemplate);
    }

    @Override
    public TemplateResponse getTemplateById(Long id) {
        log.debug("Fetching notification template with ID: {}", id);

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        return templateMapper.toResponse(template);
    }

    @Override
    public TemplateResponse getTemplateByCode(String templateCode) {
        log.debug("Fetching notification template with code: {}", templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with code: " + templateCode));

        return templateMapper.toResponse(template);
    }

    @Override
    public Page<TemplateResponse> getAllTemplates(Pageable pageable) {
        log.debug("Fetching all notification templates with pagination");

        return templateRepository.findAll(pageable)
                .map(templateMapper::toResponse);
    }

    @Override
    public List<TemplateResponse> getTemplatesByType(NotificationType type) {
        log.debug("Fetching templates by type: {}", type);

        return templateRepository.findByType(type).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    @Override
    public List<TemplateResponse> getTemplatesByChannel(NotificationChannel channel) {
        log.debug("Fetching templates by channel: {}", channel);

        return templateRepository.findByChannel(channel).stream()
                .map(templateMapper::toResponse)
                .toList();
    }

    @Override
    public void deleteTemplate(Long id) {
        log.info("Deleting notification template with ID: {}", id);

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        templateRepository.delete(template);
        log.info("Notification template deleted successfully with ID: {}", id);
    }

    @Override
    public void activateTemplate(Long id) {
        log.info("Activating notification template with ID: {}", id);

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        template.setIsActive(true);
        templateRepository.save(template);
    }

    @Override
    public void deactivateTemplate(Long id) {
        log.info("Deactivating notification template with ID: {}", id);

        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        template.setIsActive(false);
        templateRepository.save(template);
    }

    @Override
    public String renderTemplate(String templateCode, Object data) {
        log.debug("Rendering template: {}", templateCode);

        NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with code: " + templateCode));

        String content = template.getContentHtml() != null ?
                template.getContentHtml() : template.getContent();

        return templateParser.parse(content, data);
    }

    @Override
    public boolean isTemplateCodeUnique(String templateCode) {
        return !templateRepository.existsByTemplateCode(templateCode);
    }
}