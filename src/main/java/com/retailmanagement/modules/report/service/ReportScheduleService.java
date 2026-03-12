package com.retailmanagement.modules.report.service;

import com.retailmanagement.modules.report.dto.request.ScheduleReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportScheduleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportScheduleService {

    ReportScheduleResponse createSchedule(ScheduleReportRequest request, Long userId);

    ReportScheduleResponse updateSchedule(Long id, ScheduleReportRequest request);

    ReportScheduleResponse getScheduleById(Long id);

    ReportScheduleResponse getScheduleByScheduleId(String scheduleId);

    Page<ReportScheduleResponse> getAllSchedules(Pageable pageable);

    List<ReportScheduleResponse> getActiveSchedules();

    List<ReportScheduleResponse> getSchedulesByUser(Long userId);

    void deleteSchedule(Long id);

    void activateSchedule(Long id);

    void deactivateSchedule(Long id);

    void executeSchedule(Long id);

    void processDueSchedules();

    List<ReportScheduleResponse> getSchedulesDueForExecution();

    boolean isScheduleIdUnique(String scheduleId);
}