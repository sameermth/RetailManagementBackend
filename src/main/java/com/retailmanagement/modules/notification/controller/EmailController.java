package com.retailmanagement.modules.notification.controller;

import com.retailmanagement.modules.notification.dto.request.EmailRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/email")
@RequiredArgsConstructor
@Tag(name = "Email Notifications", description = "Email notification endpoints")
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send a simple email")
    public ResponseEntity<NotificationResponse> sendSimpleEmail(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(emailService.sendSimpleEmail(request));
    }

    @PostMapping("/send-html")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send an HTML email")
    public ResponseEntity<NotificationResponse> sendHtmlEmail(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(emailService.sendHtmlEmail(request));
    }

    @PostMapping("/send-with-attachment")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send an email with attachment")
    public ResponseEntity<NotificationResponse> sendEmailWithAttachment(@Valid @RequestBody EmailRequest request) {
        return ResponseEntity.ok(emailService.sendEmailWithAttachment(request));
    }

    @PostMapping("/send-template/{templateCode}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send a template-based email")
    public ResponseEntity<NotificationResponse> sendTemplateEmail(
            @PathVariable String templateCode,
            @RequestParam String to,
            @RequestParam(required = false) String subject,
            @RequestBody Object data) {
        return ResponseEntity.ok(emailService.sendTemplateEmail(templateCode, to, subject, data));
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send bulk emails")
    public ResponseEntity<Void> sendBulkEmails(
            @Valid @RequestBody EmailRequest request,
            @RequestParam List<String> recipients) {
        emailService.sendBulkEmails(request, recipients);
        return ResponseEntity.ok().build();
    }
}