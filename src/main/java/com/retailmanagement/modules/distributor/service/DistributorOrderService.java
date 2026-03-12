package com.retailmanagement.modules.distributor.service;

import com.retailmanagement.modules.distributor.dto.request.DistributorOrderRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderSummaryResponse;
import com.retailmanagement.modules.distributor.enums.DistributorOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface DistributorOrderService {

    DistributorOrderResponse createOrder(DistributorOrderRequest request);

    DistributorOrderResponse updateOrder(Long id, DistributorOrderRequest request);

    DistributorOrderResponse getOrderById(Long id);

    DistributorOrderResponse getOrderByNumber(String orderNumber);

    Page<DistributorOrderResponse> getAllOrders(Pageable pageable);

    List<DistributorOrderResponse> getOrdersByDistributor(Long distributorId);

    Page<DistributorOrderResponse> getOrdersByDistributor(Long distributorId, Pageable pageable);

    List<DistributorOrderResponse> getOrdersByStatus(DistributorOrderStatus status);

    Page<DistributorOrderResponse> getOrdersByStatus(DistributorOrderStatus status, Pageable pageable);

    List<DistributorOrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    void approveOrder(Long id);

    void processOrder(Long id);

    void shipOrder(Long id, String trackingNumber);

    void deliverOrder(Long id);

    void cancelOrder(Long id, String reason);

    void updatePaymentStatus(Long id, Double paidAmount);

    Double getTotalOrderAmount(LocalDateTime startDate, LocalDateTime endDate);

    Long getPendingOrderCount();

    List<DistributorOrderSummaryResponse> getRecentOrders(int limit);

    List<DistributorOrderSummaryResponse> getDelayedOrders();

    boolean isOrderNumberUnique(String orderNumber);
}