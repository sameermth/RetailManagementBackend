package com.retailmanagement.modules.notification.controller;

import com.retailmanagement.modules.notification.dto.request.TemplateRequest;
import com.retailmanagement.modules.notification.dto.response.TemplateResponse;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import com.retailmanagement.modules.notification.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-templates")
@RequiredArgsConstructor
@Tag(name = "Notification Templates", description = "Notification template management endpoints")
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new notification template")
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return new ResponseEntity<>(templateService.createTemplate(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing template")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(templateService.updateTemplate(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @GetMapping("/code/{templateCode}")
    @Operation(summary = "Get template by code")
    public ResponseEntity<TemplateResponse> getTemplateByCode(@PathVariable String templateCode) {
        return ResponseEntity.ok(templateService.getTemplateByCode(templateCode));
    }

    @GetMapping
    @Operation(summary = "Get all templates with pagination")
    public ResponseEntity<Page<TemplateResponse>> getAllTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(templateService.getAllTemplates(pageable));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get templates by type")
    public ResponseEntity<List<TemplateResponse>> getTemplatesByType(@PathVariable NotificationType type) {
        return ResponseEntity.ok(templateService.getTemplatesByType(type));
    }

    @GetMapping("/channel/{channel}")
    @Operation(summary = "Get templates by channel")
    public ResponseEntity<List<TemplateResponse>> getTemplatesByChannel(@PathVariable NotificationChannel channel) {
        return ResponseEntity.ok(templateService.getTemplatesByChannel(channel));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a template")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate a template")
    public ResponseEntity<Void> activateTemplate(@PathVariable Long id) {
        templateService.activateTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate a template")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long id) {
        templateService.deactivateTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{templateCode}/render")
    @Operation(summary = "Render a template with data")
    public ResponseEntity<String> renderTemplate(
            @PathVariable String templateCode,
            @RequestBody Object data) {
        return ResponseEntity.ok(templateService.renderTemplate(templateCode, data));
    }

    @GetMapping("/check-code")
    @Operation(summary = "Check if template code is unique")
    public ResponseEntity<Boolean> checkTemplateCodeUnique(@RequestParam String templateCode) {
        return ResponseEntity.ok(templateService.isTemplateCodeUnique(templateCode));
    }
}