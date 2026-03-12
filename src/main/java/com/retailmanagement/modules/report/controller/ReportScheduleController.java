package com.retailmanagement.modules.report.controller;

import com.retailmanagement.modules.report.dto.request.ScheduleReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportScheduleResponse;
import com.retailmanagement.modules.report.service.ReportScheduleService;
import com.retailmanagement.modules.auth.security.CurrentUser;
import com.retailmanagement.modules.auth.security.UserPrincipal;
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
@RequestMapping("/api/report-schedules")
@RequiredArgsConstructor
@Tag(name = "Report Schedules", description = "Scheduled report management endpoints")
public class ReportScheduleController {

    private final ReportScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Create a new report schedule")
    public ResponseEntity<ReportScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleReportRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return new ResponseEntity<>(
                scheduleService.createSchedule(request, currentUser.getId()),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Update an existing report schedule")
    public ResponseEntity<ReportScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleReportRequest request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ReportScheduleResponse> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getScheduleById(id));
    }

    @GetMapping("/schedule-id/{scheduleId}")
    @Operation(summary = "Get schedule by schedule ID")
    public ResponseEntity<ReportScheduleResponse> getScheduleByScheduleId(@PathVariable String scheduleId) {
        return ResponseEntity.ok(scheduleService.getScheduleByScheduleId(scheduleId));
    }

    @GetMapping
    @Operation(summary = "Get all schedules with pagination")
    public ResponseEntity<Page<ReportScheduleResponse>> getAllSchedules(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(scheduleService.getAllSchedules(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active schedules")
    public ResponseEntity<List<ReportScheduleResponse>> getActiveSchedules() {
        return ResponseEntity.ok(scheduleService.getActiveSchedules());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get schedules by user")
    public ResponseEntity<List<ReportScheduleResponse>> getSchedulesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByUser(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Activate a schedule")
    public ResponseEntity<Void> activateSchedule(@PathVariable Long id) {
        scheduleService.activateSchedule(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Deactivate a schedule")
    public ResponseEntity<Void> deactivateSchedule(@PathVariable Long id) {
        scheduleService.deactivateSchedule(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Execute a schedule immediately")
    public ResponseEntity<Void> executeSchedule(@PathVariable Long id) {
        scheduleService.executeSchedule(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/due")
    @Operation(summary = "Get schedules due for execution")
    public ResponseEntity<List<ReportScheduleResponse>> getSchedulesDueForExecution() {
        return ResponseEntity.ok(scheduleService.getSchedulesDueForExecution());
    }

    @GetMapping("/check-schedule-id")
    @Operation(summary = "Check if schedule ID is unique")
    public ResponseEntity<Boolean> checkScheduleIdUnique(@RequestParam String scheduleId) {
        return ResponseEntity.ok(scheduleService.isScheduleIdUnique(scheduleId));
    }
}