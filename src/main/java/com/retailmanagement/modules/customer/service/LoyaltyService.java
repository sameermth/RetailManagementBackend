package com.retailmanagement.modules.customer.service;

import com.retailmanagement.modules.customer.dto.response.LoyaltySummaryResponse;
import com.retailmanagement.modules.customer.model.LoyaltyTransaction;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyService {

    LoyaltySummaryResponse getCustomerLoyaltySummary(Long customerId);

    Integer getCustomerPoints(Long customerId);

    String getCustomerTier(Long customerId);

    LoyaltyTransaction earnPoints(Long customerId, BigDecimal purchaseAmount, Long saleId);

    LoyaltyTransaction redeemPoints(Long customerId, Integer points, String redeemedFor, Long saleId);

    LoyaltyTransaction adjustPoints(Long customerId, Integer points, String reason);

    List<LoyaltyTransaction> getCustomerTransactionHistory(Long customerId);

    List<LoyaltySummaryResponse.LoyaltyTransactionResponse> getRecentTransactions(Long customerId, int limit);

    Integer getPointsToNextTier(Long customerId);

    String getNextTier(Long customerId);

    void processExpiredPoints();

    BigDecimal calculatePointsValue(Long customerId, Integer points);

    boolean canRedeemPoints(Long customerId, Integer points);
}