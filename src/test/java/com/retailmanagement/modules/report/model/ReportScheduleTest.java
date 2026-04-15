package com.retailmanagement.modules.report.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.retailmanagement.modules.report.enums.ReportFormat;
import com.retailmanagement.modules.report.enums.ReportType;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ReportScheduleTest {

    @Test
    void hydrateFromConfig_defaultsFormatAndMapsLegacySalesSummaryCode() {
        ReportSchedule schedule = ReportSchedule.builder()
                .reportTypeCode("DAILY_SALES_SUMMARY")
                .configJson(new HashMap<>())
                .build();

        schedule.hydrateFromConfig();

        assertEquals(ReportType.SALES_SUMMARY, schedule.getReportType());
        assertEquals(ReportFormat.PDF, schedule.getFormat());
    }

    @Test
    void hydrateFromConfig_mapsLegacyLowStockSummaryCode() {
        ReportSchedule schedule = ReportSchedule.builder()
                .reportTypeCode("LOW_STOCK_SUMMARY")
                .configJson(new HashMap<>())
                .build();

        schedule.hydrateFromConfig();

        assertEquals(ReportType.LOW_STOCK_REPORT, schedule.getReportType());
        assertEquals(ReportFormat.PDF, schedule.getFormat());
    }
}
