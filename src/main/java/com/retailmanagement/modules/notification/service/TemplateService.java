package com.retailmanagement.modules.notification.service;

import com.retailmanagement.modules.notification.dto.request.TemplateRequest;
import com.retailmanagement.modules.notification.dto.response.TemplateResponse;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TemplateService {

    TemplateResponse createTemplate(TemplateRequest request);

    TemplateResponse updateTemplate(Long id, TemplateRequest request);

    TemplateResponse getTemplateById(Long id);

    TemplateResponse getTemplateByCode(String templateCode);

    Page<TemplateResponse> getAllTemplates(Pageable pageable);

    List<TemplateResponse> getTemplatesByType(NotificationType type);

    List<TemplateResponse> getTemplatesByChannel(NotificationChannel channel);

    void deleteTemplate(Long id);

    void activateTemplate(Long id);

    void deactivateTemplate(Long id);

    String renderTemplate(String templateCode, Object data);

    boolean isTemplateCodeUnique(String templateCode);
}