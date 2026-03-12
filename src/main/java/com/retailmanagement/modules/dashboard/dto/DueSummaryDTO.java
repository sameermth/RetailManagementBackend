package com.retailmanagement.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DueSummaryDTO {
    private BigDecimal totalDueAmount;
    private Integer totalDueCustomers;
    private BigDecimal overdueAmount;
    private Integer overdueCount;
    private BigDecimal dueThisWeek;
    private BigDecimal dueNextWeek;

    private List<UpcomingDueDTO> upcomingDues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingDueDTO {
        private Long customerId;
        private String customerName;
        private String customerPhone;
        private BigDecimal dueAmount;
        private LocalDate dueDate;
        private Integer daysRemaining;
        private String status; // "UPCOMING", "OVERDUE", "TODAY"
    }
}