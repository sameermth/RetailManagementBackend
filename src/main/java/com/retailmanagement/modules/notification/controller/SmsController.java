package com.retailmanagement.modules.notification.controller;

import com.retailmanagement.modules.notification.dto.request.SmsRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.service.SmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/sms")
@RequiredArgsConstructor
@Tag(name = "SMS Notifications", description = "SMS notification endpoints")
public class SmsController {

    private final SmsService smsService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send an SMS")
    public ResponseEntity<NotificationResponse> sendSms(@Valid @RequestBody SmsRequest request) {
        return ResponseEntity.ok(smsService.sendSms(request));
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send bulk SMS")
    public ResponseEntity<NotificationResponse> sendBulkSms(
            @RequestParam List<String> phoneNumbers,
            @RequestParam String message) {
        return ResponseEntity.ok(smsService.sendBulkSms(phoneNumbers, message));
    }

    @PostMapping("/send-template/{templateCode}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send a template-based SMS")
    public ResponseEntity<NotificationResponse> sendTemplateSms(
            @PathVariable String templateCode,
            @RequestParam String phoneNumber,
            @RequestBody Object data) {
        return ResponseEntity.ok(smsService.sendTemplateSms(templateCode, phoneNumber, data));
    }

    @GetMapping("/status/{messageId}")
    @Operation(summary = "Get SMS delivery status")
    public ResponseEntity<String> getSmsDeliveryStatus(@PathVariable String messageId) {
        return ResponseEntity.ok(smsService.getSmsDeliveryStatus(messageId));
    }
}