package com.retailmanagement.modules.report.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.report.dto.request.ReportRequest;
import com.retailmanagement.modules.report.dto.response.ReportResponse;
import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import com.retailmanagement.modules.report.model.ReportSchedule;
import com.retailmanagement.modules.report.repository.ReportScheduleRepository;
import com.retailmanagement.modules.report.service.ReportGeneratorService;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportScheduleServiceImplTest {

    @Mock
    private ReportScheduleRepository scheduleRepository;

    @Mock
    private com.retailmanagement.modules.auth.repository.UserRepository userRepository;

    @Mock
    private ReportGeneratorService reportGeneratorService;

    @InjectMocks
    private ReportScheduleServiceImpl reportScheduleService;

    @Captor
    private ArgumentCaptor<ReportRequest> reportRequestCaptor;

    @Test
    void executeSchedule_usesLegacySalesSummaryAliasAndDefaultsFormat() {
        ReportSchedule schedule = legacySchedule(1L, "SCH-001", "DAILY_SALES_SUMMARY");

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(ReportSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportGeneratorService.generateReport(any(ReportRequest.class), eq(99L)))
                .thenReturn(ReportResponse.builder().id(1L).status("COMPLETED").build());

        reportScheduleService.executeSchedule(1L);

        verify(reportGeneratorService).generateReport(reportRequestCaptor.capture(), eq(99L));
        ReportRequest request = reportRequestCaptor.getValue();
        assertEquals(ReportType.SALES_SUMMARY, request.getReportType());
        assertEquals(ReportFormat.PDF, request.getFormat());
        assertNotNull(request.getStartDate());
        assertNotNull(request.getEndDate());
    }

    @Test
    void executeSchedule_usesLegacyLowStockAliasAndDefaultsFormat() {
        ReportSchedule schedule = legacySchedule(2L, "SCH-002", "LOW_STOCK_SUMMARY");

        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(ReportSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportGeneratorService.generateReport(any(ReportRequest.class), eq(99L)))
                .thenReturn(ReportResponse.builder().id(2L).status("COMPLETED").build());

        reportScheduleService.executeSchedule(2L);

        verify(reportGeneratorService).generateReport(reportRequestCaptor.capture(), eq(99L));
        ReportRequest request = reportRequestCaptor.getValue();
        assertEquals(ReportType.LOW_STOCK_REPORT, request.getReportType());
        assertEquals(ReportFormat.PDF, request.getFormat());
        assertNotNull(request.getStartDate());
        assertNotNull(request.getEndDate());
    }

    private ReportSchedule legacySchedule(Long id, String scheduleId, String reportTypeCode) {
        User createdBy = User.builder()
                .id(99L)
                .organizationId(33L)
                .accountId(3L)
                .personId(3L)
                .build();

        ReportSchedule schedule = ReportSchedule.builder()
                .id(id)
                .organizationId(33L)
                .scheduleId(scheduleId)
                .reportTypeCode(reportTypeCode)
                .frequency("DAILY")
                .deliveryChannel("APP")
                .isActive(true)
                .createdBy(createdBy)
                .configJson(new HashMap<>())
                .build();

        ReflectionTestUtils.invokeMethod(schedule, "hydrateFromConfig");
        return schedule;
    }
}
