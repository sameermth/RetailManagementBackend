package com.retailmanagement.modules.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltySummaryResponse {
    private Long customerId;
    private String customerName;
    private Integer totalPoints;
    private String currentTier;
    private BigDecimal totalPurchaseAmount;
    private Integer pointsToNextTier;
    private String nextTier;
    private List<LoyaltyTransactionResponse> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoyaltyTransactionResponse {
        private String transactionReference;
        private String transactionType;
        private Integer points;
        private String description;
        private LocalDateTime createdAt;
    }
}