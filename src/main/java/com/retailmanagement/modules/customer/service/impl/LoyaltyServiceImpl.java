package com.retailmanagement.modules.customer.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.customer.dto.response.LoyaltySummaryResponse;
import com.retailmanagement.modules.customer.enums.LoyaltyTransactionType;
import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.model.LoyaltyTransaction;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.customer.repository.LoyaltyTransactionRepository;
import com.retailmanagement.modules.customer.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoyaltyServiceImpl implements LoyaltyService {

    private final CustomerRepository customerRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    // Configuration constants
    private static final int POINTS_PER_RUPEE = 1; // 1 point per rupee spent
    private static final int REDEMPTION_RATE = 100; // 100 points = ₹1
    private static final BigDecimal BRONZE_THRESHOLD = new BigDecimal("0");
    private static final BigDecimal SILVER_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal GOLD_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal PLATINUM_THRESHOLD = new BigDecimal("100000");

    @Override
    public LoyaltySummaryResponse getCustomerLoyaltySummary(Long customerId) {
        log.debug("Fetching loyalty summary for customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        Integer totalPoints = getCustomerPoints(customerId);
        Integer pointsToNextTier = getPointsToNextTier(customerId);
        String nextTier = getNextTier(customerId);
        List<LoyaltySummaryResponse.LoyaltyTransactionResponse> recentTransactions = getRecentTransactions(customerId, 10);

        return LoyaltySummaryResponse.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .totalPoints(totalPoints)
                .currentTier(customer.getLoyaltyTier())
                .totalPurchaseAmount(customer.getTotalPurchaseAmount())
                .pointsToNextTier(pointsToNextTier)
                .nextTier(nextTier)
                .recentTransactions(recentTransactions)
                .build();
    }

    @Override
    public Integer getCustomerPoints(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        return customer.getLoyaltyPoints();
    }

    @Override
    public String getCustomerTier(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        return customer.getLoyaltyTier();
    }

    @Override
    public LoyaltyTransaction earnPoints(Long customerId, BigDecimal purchaseAmount, Long saleId) {
        log.info("Earning loyalty points for customer ID: {} on purchase amount: {}", customerId, purchaseAmount);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Calculate points earned (1 point per rupee)
        int pointsEarned = purchaseAmount.multiply(new BigDecimal(POINTS_PER_RUPEE))
                .setScale(0, RoundingMode.FLOOR).intValue();

        if (pointsEarned <= 0) {
            log.debug("No points earned for purchase amount: {}", purchaseAmount);
            return null;
        }

        // Create transaction
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .transactionReference(generateTransactionReference())
                .transactionType(LoyaltyTransactionType.EARNED)
                .points(pointsEarned)
                .description(String.format("Earned from purchase #%d", saleId))
                .saleId(saleId)
                .expiryDate(LocalDateTime.now().plusMonths(6)) // Points expire in 6 months
                .isExpired(false)
                .build();

        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update customer points
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + pointsEarned);

        // Update total purchase amount
        BigDecimal newTotal = customer.getTotalPurchaseAmount().add(purchaseAmount);
        customer.setTotalPurchaseAmount(newTotal);

        // Update loyalty tier based on new total
        customer.setLoyaltyTier(determineTier(newTotal));
        customer.setLastPurchaseDate(LocalDateTime.now());

        customerRepository.save(customer);

        log.info("Customer earned {} points. New total: {}, New tier: {}",
                pointsEarned, customer.getLoyaltyPoints(), customer.getLoyaltyTier());

        return savedTransaction;
    }

    @Override
    public LoyaltyTransaction redeemPoints(Long customerId, Integer points, String redeemedFor, Long saleId) {
        log.info("Redeeming {} loyalty points for customer ID: {}", points, customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Check if customer has enough points
        if (customer.getLoyaltyPoints() < points) {
            throw new BusinessException("Insufficient loyalty points. Available: " +
                    customer.getLoyaltyPoints() + ", Requested: " + points);
        }

        // Check if points can be redeemed (not expired, etc.)
        if (!canRedeemPoints(customerId, points)) {
            throw new BusinessException("Cannot redeem points. Please check point validity.");
        }

        // Calculate redemption value
        BigDecimal redemptionValue = calculatePointsValue(customerId, points);

        // Create transaction
        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .transactionReference(generateTransactionReference())
                .transactionType(LoyaltyTransactionType.REDEEMED)
                .points(points)
                .description(String.format("Redeemed for %s", redeemedFor))
                .saleId(saleId)
                .redeemedAt(LocalDateTime.now())
                .redeemedFor(redeemedFor)
                .build();

        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update customer points
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() - points);
        customerRepository.save(customer);

        log.info("Customer redeemed {} points for value ₹{}. Remaining points: {}",
                points, redemptionValue, customer.getLoyaltyPoints());

        return savedTransaction;
    }

    @Override
    public LoyaltyTransaction adjustPoints(Long customerId, Integer points, String reason) {
        log.info("Adjusting loyalty points for customer ID: {} by {} points", customerId, points);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        LoyaltyTransactionType type = points > 0 ? LoyaltyTransactionType.EARNED : LoyaltyTransactionType.ADJUSTED;

        LoyaltyTransaction transaction = LoyaltyTransaction.builder()
                .customer(customer)
                .transactionReference(generateTransactionReference())
                .transactionType(type)
                .points(Math.abs(points))
                .description("Manual adjustment: " + reason)
                .build();

        LoyaltyTransaction savedTransaction = loyaltyTransactionRepository.save(transaction);

        // Update customer points
        customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
        customerRepository.save(customer);

        log.info("Points adjusted for customer. New total: {}", customer.getLoyaltyPoints());

        return savedTransaction;
    }

    @Override
    public List<LoyaltyTransaction> getCustomerTransactionHistory(Long customerId) {
        return loyaltyTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public List<LoyaltySummaryResponse.LoyaltyTransactionResponse> getRecentTransactions(Long customerId, int limit) {
        return loyaltyTransactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .limit(limit)
                .map(this::convertToTransactionResponse)
                .collect(Collectors.toList());
    }

    private LoyaltySummaryResponse.LoyaltyTransactionResponse convertToTransactionResponse(LoyaltyTransaction transaction) {
        return LoyaltySummaryResponse.LoyaltyTransactionResponse.builder()
                .transactionReference(transaction.getTransactionReference())
                .transactionType(transaction.getTransactionType().name())
                .points(transaction.getPoints())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    public Integer getPointsToNextTier(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        BigDecimal currentTotal = customer.getTotalPurchaseAmount();
        String currentTier = customer.getLoyaltyTier();

        switch (currentTier) {
            case "BRONZE":
                return SILVER_THRESHOLD.subtract(currentTotal).intValue();
            case "SILVER":
                return GOLD_THRESHOLD.subtract(currentTotal).intValue();
            case "GOLD":
                return PLATINUM_THRESHOLD.subtract(currentTotal).intValue();
            case "PLATINUM":
                return 0; // Already at highest tier
            default:
                return 0;
        }
    }

    @Override
    public String getNextTier(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        String currentTier = customer.getLoyaltyTier();

        switch (currentTier) {
            case "BRONZE":
                return "SILVER";
            case "SILVER":
                return "GOLD";
            case "GOLD":
                return "PLATINUM";
            case "PLATINUM":
                return "PLATINUM"; // Already at highest
            default:
                return "BRONZE";
        }
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void processExpiredPoints() {
        log.info("Processing expired loyalty points at {}", LocalDateTime.now());

        List<LoyaltyTransaction> expiredTransactions = loyaltyTransactionRepository.findExpiredTransactions();

        for (LoyaltyTransaction transaction : expiredTransactions) {
            // Mark transaction as expired
            transaction.setIsExpired(true);
            loyaltyTransactionRepository.save(transaction);

            // Create expiration transaction record
            LoyaltyTransaction expirationTransaction = LoyaltyTransaction.builder()
                    .customer(transaction.getCustomer())
                    .transactionReference(generateTransactionReference())
                    .transactionType(LoyaltyTransactionType.EXPIRED)
                    .points(transaction.getPoints())
                    .description("Points expired on " + LocalDateTime.now().toLocalDate())
                    .build();

            loyaltyTransactionRepository.save(expirationTransaction);

            // Update customer's total points
            Customer customer = transaction.getCustomer();
            customer.setLoyaltyPoints(customer.getLoyaltyPoints() - transaction.getPoints());
            customerRepository.save(customer);

            log.info("Expired {} points for customer ID: {}", transaction.getPoints(), customer.getId());
        }

        log.info("Processed {} expired transactions", expiredTransactions.size());
    }

    @Override
    public BigDecimal calculatePointsValue(Long customerId, Integer points) {
        // 100 points = ₹1
        return new BigDecimal(points)
                .divide(new BigDecimal(REDEMPTION_RATE), 2, RoundingMode.HALF_UP);
    }

    @Override
    public boolean canRedeemPoints(Long customerId, Integer points) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Check if customer has enough points
        if (customer.getLoyaltyPoints() < points) {
            return false;
        }

        // Check minimum redemption (e.g., minimum 100 points)
        if (points < 100) {
            return false;
        }

        // Check if points are valid (not expired)
        // This would require checking the sum of non-expired earned points
        Integer totalEarnedPoints = loyaltyTransactionRepository.getTotalEarnedPoints(customerId);
        Integer totalRedeemedPoints = loyaltyTransactionRepository.getTotalRedeemedPoints(customerId);

        return (totalEarnedPoints - totalRedeemedPoints) >= points;
    }

    private String generateTransactionReference() {
        return "LOY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String determineTier(BigDecimal totalPurchaseAmount) {
        if (totalPurchaseAmount.compareTo(PLATINUM_THRESHOLD) >= 0) {
            return "PLATINUM";
        } else if (totalPurchaseAmount.compareTo(GOLD_THRESHOLD) >= 0) {
            return "GOLD";
        } else if (totalPurchaseAmount.compareTo(SILVER_THRESHOLD) >= 0) {
            return "SILVER";
        } else {
            return "BRONZE";
        }
    }
}