package com.retailmanagement.modules.notification.controller;

import com.retailmanagement.modules.notification.dto.request.NotificationRequest;
import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.dto.response.NotificationStatsResponse;
import com.retailmanagement.modules.notification.service.NotificationService;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Send a notification")
    public ResponseEntity<NotificationResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        return new ResponseEntity<>(notificationService.sendNotification(request), HttpStatus.CREATED);
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Schedule a notification")
    public ResponseEntity<NotificationResponse> scheduleNotification(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(notificationService.scheduleNotification(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @GetMapping("/notification-id/{notificationId}")
    @Operation(summary = "Get notification by notification ID")
    public ResponseEntity<NotificationResponse> getNotificationByNotificationId(@PathVariable String notificationId) {
        return ResponseEntity.ok(notificationService.getNotificationByNotificationId(notificationId));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get notifications by user")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotificationsByUser(userId, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get notifications by customer")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotificationsByCustomer(customerId, pageable));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications by user")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotificationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUser(userId));
    }

    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get unread notifications count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Mark all notifications as read for user")
    public ResponseEntity<Void> markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a notification")
    public ResponseEntity<Void> cancelNotification(@PathVariable Long id) {
        notificationService.cancelNotification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Get notification statistics")
    public ResponseEntity<NotificationStatsResponse> getNotificationStats(
            @RequestParam(defaultValue = "MONTH") String period) {
        return ResponseEntity.ok(notificationService.getNotificationStats(period));
    }

    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retry failed notifications")
    public ResponseEntity<Void> retryFailedNotifications() {
        notificationService.retryFailedNotifications();
        return ResponseEntity.ok().build();
    }
}