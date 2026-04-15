package com.retailmanagement.modules.report.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.report.dto.request.ReportRequest;
import com.retailmanagement.modules.report.dto.request.ScheduleReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportScheduleResponse;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.model.ReportSchedule;
import com.retailmanagement.modules.report.repository.ReportScheduleRepository;
import com.retailmanagement.modules.report.service.ReportGeneratorService;
import com.retailmanagement.modules.report.service.ReportScheduleService;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportScheduleServiceImpl implements ReportScheduleService {

    private final ReportScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final ReportGeneratorService reportGeneratorService;

    @Override
    public ReportScheduleResponse createSchedule(ScheduleReportRequest request, Long userId) {
        log.info("Creating new report schedule: {} for user: {}", request.getScheduleName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        String normalizedFrequency = normalizeFrequency(request.getFrequency());

        ReportSchedule schedule = ReportSchedule.builder()
                .organizationId(user.getOrganizationId())
                .scheduleId(generateScheduleId())
                .scheduleName(request.getScheduleName())
                .reportType(request.getReportType())
                .format(request.getFormat())
                .createdBy(user)
                .frequency(normalizedFrequency)
                .deliveryChannel(resolveDeliveryChannel(request.getRecipients()))
                .cronExpression(request.getCronExpression())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .parameters(request.getParameters() != null ? request.getParameters() : new HashMap<>())
                .recipients(request.getRecipients())
                .isActive(true)
                .successCount(0)
                .failureCount(0)
                .build();

        // Calculate next run date
        schedule.setNextRunDate(calculateNextRunDate(schedule));

        ReportSchedule savedSchedule = scheduleRepository.save(schedule);

        log.info("Report schedule created successfully with ID: {}", savedSchedule.getScheduleId());

        return convertToResponse(savedSchedule);
    }

    private String generateScheduleId() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String scheduleId = "SCH-" + timestamp + "-" + randomPart;

        while (scheduleRepository.existsByScheduleId(scheduleId)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            scheduleId = "SCH-" + timestamp + "-" + randomPart;
        }

        return scheduleId;
    }

    private LocalDateTime calculateNextRunDate(ReportSchedule schedule) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = schedule.getStartDate() != null ? schedule.getStartDate() : now;

        if (startDate.isAfter(now)) {
            return startDate;
        }

        switch (schedule.getFrequency()) {
            case "DAILY":
                return now.plusDays(1).withHour(0).withMinute(0).withSecond(0);
            case "WEEKLY":
                return now.plusWeeks(1).withHour(0).withMinute(0).withSecond(0);
            case "MONTHLY":
                return now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case "QUARTERLY":
                return now.plusMonths(3).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case "YEARLY":
                return now.plusYears(1).withDayOfYear(1).withHour(0).withMinute(0).withSecond(0);
            default:
                throw new BusinessException("Unsupported report schedule frequency: " + schedule.getFrequency());
        }
    }

    private String normalizeFrequency(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            throw new BusinessException("Report schedule frequency is required");
        }
        String normalized = frequency.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY" -> normalized;
            case "CUSTOM" -> throw new BusinessException("Custom report schedules are not supported in the current ERP schema");
            default -> throw new BusinessException("Unsupported report schedule frequency: " + frequency);
        };
    }

    private String resolveDeliveryChannel(String recipients) {
        return recipients != null && !recipients.isBlank() ? "EMAIL" : "APP";
    }

    @Override
    public ReportScheduleResponse updateSchedule(Long id, ScheduleReportRequest request) {
        log.info("Updating report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));
        String normalizedFrequency = normalizeFrequency(request.getFrequency());

        // Update fields
        schedule.setScheduleName(request.getScheduleName());
        schedule.setReportType(request.getReportType());
        schedule.setFormat(request.getFormat());
        schedule.setFrequency(normalizedFrequency);
        schedule.setDeliveryChannel(resolveDeliveryChannel(request.getRecipients()));
        schedule.setCronExpression(request.getCronExpression());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setParameters(request.getParameters() != null ? request.getParameters() : new HashMap<>());
        schedule.setRecipients(request.getRecipients());

        // Recalculate next run date
        schedule.setNextRunDate(calculateNextRunDate(schedule));

        ReportSchedule updatedSchedule = scheduleRepository.save(schedule);

        log.info("Report schedule updated successfully with ID: {}", updatedSchedule.getId());

        return convertToResponse(updatedSchedule);
    }

    @Override
    public ReportScheduleResponse getScheduleById(Long id) {
        log.debug("Fetching report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));

        return convertToResponse(schedule);
    }

    @Override
    public ReportScheduleResponse getScheduleByScheduleId(String scheduleId) {
        log.debug("Fetching report schedule with schedule ID: {}", scheduleId);

        ReportSchedule schedule = scheduleRepository.findByScheduleId(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with schedule ID: " + scheduleId));

        return convertToResponse(schedule);
    }

    @Override
    public Page<ReportScheduleResponse> getAllSchedules(Pageable pageable) {
        log.debug("Fetching all report schedules with pagination");

        return scheduleRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Override
    public List<ReportScheduleResponse> getActiveSchedules() {
        log.debug("Fetching active report schedules");

        return scheduleRepository.findByIsActiveTrue().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReportScheduleResponse> getSchedulesByUser(Long userId) {
        log.debug("Fetching report schedules for user ID: {}", userId);

        return scheduleRepository.findByCreatedById(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSchedule(Long id) {
        log.info("Deleting report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));

        scheduleRepository.delete(schedule);
        log.info("Report schedule deleted successfully with ID: {}", id);
    }

    @Override
    public void activateSchedule(Long id) {
        log.info("Activating report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));

        schedule.setIsActive(true);
        schedule.setNextRunDate(calculateNextRunDate(schedule));
        scheduleRepository.save(schedule);
    }

    @Override
    public void deactivateSchedule(Long id) {
        log.info("Deactivating report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));

        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
    }

    @Override
    public void executeSchedule(Long id) {
        log.info("Executing report schedule with ID: {}", id);

        ReportSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report schedule not found with id: " + id));

        if (!schedule.getIsActive()) {
            throw new BusinessException("Cannot execute inactive schedule");
        }

        try {
            LocalDateTime executionTime = LocalDateTime.now();
            ReportType reportType = resolveReportType(schedule);
            ReportFormat format = resolveFormat(schedule);

            // Create report request from schedule
            ReportRequest reportRequest = new ReportRequest();
            reportRequest.setReportName(resolveScheduleName(schedule) + "_" + executionTime);
            reportRequest.setReportType(reportType);
            reportRequest.setFormat(format);
            reportRequest.setParameters(schedule.getParameters());
            reportRequest.setStartDate(resolveExecutionStartDate(schedule, executionTime));
            reportRequest.setEndDate(resolveExecutionEndDate(schedule, executionTime));

            // Generate report
            reportGeneratorService.generateReport(reportRequest, schedule.getCreatedBy().getId());

            // Update schedule stats
            schedule.setLastRunDate(executionTime);
            schedule.setSuccessCount(schedule.getSuccessCount() + 1);
            schedule.setNextRunDate(calculateNextRunDate(schedule));

            // Send emails if recipients configured
            if (schedule.getRecipients() != null && !schedule.getRecipients().isEmpty()) {
                sendReportEmails(schedule);
            }

            log.info("Report schedule executed successfully for ID: {}", id);

        } catch (Exception e) {
            log.error("Failed to execute report schedule: {}", e.getMessage(), e);
            schedule.setFailureCount(schedule.getFailureCount() + 1);
            schedule.setLastError(e.getMessage());
        }

        scheduleRepository.save(schedule);
    }

    private void sendReportEmails(ReportSchedule schedule) {
        // Implementation to send emails with report attachments
        String[] recipients = schedule.getRecipients().split(",");
        for (String recipient : recipients) {
            // emailService.sendReportNotification(recipient.trim(), schedule);
        }
    }

    @Override
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void processDueSchedules() {
        log.info("Processing due report schedules at {}", LocalDateTime.now());

        List<ReportScheduleResponse> dueSchedules = getSchedulesDueForExecution();

        for (ReportScheduleResponse schedule : dueSchedules) {
            try {
                executeSchedule(schedule.getId());
            } catch (Exception e) {
                log.error("Failed to process schedule ID: {}", schedule.getId(), e);
            }
        }

        log.info("Processed {} due schedules", dueSchedules.size());
    }

    @Override
    public List<ReportScheduleResponse> getSchedulesDueForExecution() {
        log.debug("Fetching schedules due for execution");

        LocalDateTime now = LocalDateTime.now();
        return scheduleRepository.findSchedulesDueForExecution(now).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isScheduleIdUnique(String scheduleId) {
        return !scheduleRepository.existsByScheduleId(scheduleId);
    }

    private ReportType resolveReportType(ReportSchedule schedule) {
        if (schedule.getReportType() != null) {
            return schedule.getReportType();
        }
        throw new BusinessException("Report type is missing or unsupported for schedule " + schedule.getScheduleId());
    }

    private ReportFormat resolveFormat(ReportSchedule schedule) {
        return schedule.getFormat() != null ? schedule.getFormat() : ReportFormat.PDF;
    }

    private String resolveScheduleName(ReportSchedule schedule) {
        if (schedule.getScheduleName() != null && !schedule.getScheduleName().isBlank()) {
            return schedule.getScheduleName();
        }
        return schedule.getScheduleId();
    }

    private LocalDateTime resolveExecutionStartDate(ReportSchedule schedule, LocalDateTime executionTime) {
        if (schedule.getLastRunDate() != null) {
            return schedule.getLastRunDate();
        }
        if (schedule.getStartDate() != null) {
            return schedule.getStartDate();
        }
        return switch (schedule.getFrequency()) {
            case "DAILY" -> executionTime.minusDays(1);
            case "WEEKLY" -> executionTime.minusWeeks(1);
            case "MONTHLY" -> executionTime.minusMonths(1);
            case "QUARTERLY" -> executionTime.minusMonths(3);
            case "YEARLY" -> executionTime.minusYears(1);
            default -> executionTime.minusDays(1);
        };
    }

    private LocalDateTime resolveExecutionEndDate(ReportSchedule schedule, LocalDateTime executionTime) {
        if (schedule.getEndDate() != null && schedule.getEndDate().isBefore(executionTime)) {
            return schedule.getEndDate();
        }
        return executionTime;
    }

    private ReportScheduleResponse convertToResponse(ReportSchedule schedule) {
        return ReportScheduleResponse.builder()
                .id(schedule.getId())
                .scheduleId(schedule.getScheduleId())
                .scheduleName(schedule.getScheduleName())
                .reportType(schedule.getReportType())
                .format(schedule.getFormat())
                .createdBy(schedule.getCreatedBy() != null ? schedule.getCreatedBy().getUsername() : null)
                .frequency(schedule.getFrequency())
                .cronExpression(schedule.getCronExpression())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .lastRunDate(schedule.getLastRunDate())
                .nextRunDate(schedule.getNextRunDate())
                .parameters(schedule.getParameters())
                .recipients(schedule.getRecipients())
                .isActive(schedule.getIsActive())
                .successCount(schedule.getSuccessCount())
                .failureCount(schedule.getFailureCount())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
