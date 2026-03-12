package com.retailmanagement.modules.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {
    private Long totalSent;
    private Long pending;
    private Long failed;
    private Long delivered;
    private Long read;
    private Map<String, Long> byType;
    private Map<String, Long> byChannel;
    private Map<String, Long> byPriority;
    private Map<String, Long> byDay;
    private Double successRate;
}