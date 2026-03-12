package com.retailmanagement.modules.distributor.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.distributor.dto.request.DistributorOrderItemRequest;
import com.retailmanagement.modules.distributor.dto.request.DistributorOrderRequest;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderResponse;
import com.retailmanagement.modules.distributor.dto.response.DistributorOrderSummaryResponse;
import com.retailmanagement.modules.distributor.enums.DistributorOrderStatus;
import com.retailmanagement.modules.distributor.mapper.DistributorOrderItemMapper;
import com.retailmanagement.modules.distributor.mapper.DistributorOrderMapper;
import com.retailmanagement.modules.distributor.model.Distributor;
import com.retailmanagement.modules.distributor.model.DistributorOrder;
import com.retailmanagement.modules.distributor.model.DistributorOrderItem;
import com.retailmanagement.modules.distributor.repository.DistributorOrderRepository;
import com.retailmanagement.modules.distributor.repository.DistributorRepository;
import com.retailmanagement.modules.distributor.service.DistributorOrderService;
import com.retailmanagement.modules.distributor.service.DistributorService;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DistributorOrderServiceImpl implements DistributorOrderService {

    private final DistributorOrderRepository orderRepository;
    private final DistributorRepository distributorRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DistributorService distributorService;
    private final DistributorOrderMapper orderMapper;
    private final DistributorOrderItemMapper itemMapper;

    @Override
    public DistributorOrderResponse createOrder(DistributorOrderRequest request) {
        log.info("Creating order for distributor ID: {}", request.getDistributorId());

        Distributor distributor = distributorRepository.findById(request.getDistributorId())
                .orElseThrow(() -> new ResourceNotFoundException("Distributor not found with id: " + request.getDistributorId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Check distributor status
        if (distributor.getStatus() != DistributorStatus.ACTIVE) {
            throw new BusinessException("Cannot create order for inactive distributor");
        }

        // Check credit limit if applicable
        BigDecimal orderTotal = calculateOrderTotal(request);
        BigDecimal newOutstanding = distributor.getOutstandingAmount().add(orderTotal);
        if (distributor.getCreditLimit() != null &&
                newOutstanding.compareTo(distributor.getCreditLimit()) > 0) {
            throw new BusinessException("Order would exceed distributor's credit limit");
        }

        // Validate and process items
        List<DistributorOrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (DistributorOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            // Check stock availability
            if (!inventoryService.checkStockAvailability(product.getId(), 1L, itemRequest.getQuantity())) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }

            // Calculate item prices
            BigDecimal unitPrice = itemRequest.getUnitPrice() != null ?
                    itemRequest.getUnitPrice() : product.getUnitPrice();

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            // Apply item discount
            BigDecimal itemDiscount = calculateItemDiscount(itemSubtotal,
                    itemRequest.getDiscountAmount(), itemRequest.getDiscountPercentage());

            // Calculate tax
            BigDecimal taxRate = itemRequest.getTaxRate() != null ?
                    itemRequest.getTaxRate() : (product.getGstRate() != null ? product.getGstRate() : BigDecimal.ZERO);
            BigDecimal itemTax = itemSubtotal.subtract(itemDiscount)
                    .multiply(taxRate.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

            BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount).add(itemTax);

            // Create order item
            DistributorOrderItem orderItem = DistributorOrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .shippedQuantity(0)
                    .unitPrice(unitPrice)
                    .discountAmount(itemDiscount)
                    .discountPercentage(itemRequest.getDiscountPercentage())
                    .taxRate(taxRate)
                    .taxAmount(itemTax)
                    .totalPrice(itemTotal)
                    .notes(itemRequest.getNotes())
                    .build();

            orderItems.add(orderItem);

            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);

            // Reserve stock
            inventoryService.reserveStock(product.getId(), 1L, itemRequest.getQuantity());
        }

        // Apply order level discount
        BigDecimal orderDiscount = calculateOrderDiscount(subtotal,
                request.getDiscountAmount(), request.getDiscountPercentage());

        BigDecimal totalAmount = subtotal
                .subtract(orderDiscount)
                .add(totalTax)
                .add(request.getShippingAmount() != null ? request.getShippingAmount() : BigDecimal.ZERO);

        // Generate order number
        String orderNumber = generateOrderNumber();

        // Create order
        DistributorOrder order = DistributorOrder.builder()
                .orderNumber(orderNumber)
                .distributor(distributor)
                .user(user)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(DistributorOrderStatus.PENDING_APPROVAL)
                .items(orderItems)
                .subtotal(subtotal)
                .discountAmount(orderDiscount)
                .discountPercentage(request.getDiscountPercentage())
                .taxAmount(totalTax)
                .shippingAmount(request.getShippingAmount())
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(totalAmount)
                .paymentStatus("PENDING")
                .shippingMethod(request.getShippingMethod())
                .notes(request.getNotes())
                .createdBy(user.getUsername())
                .updatedBy(user.getUsername())
                .build();

        // Set bidirectional relationship
        orderItems.forEach(item -> item.setOrder(order));

        DistributorOrder savedOrder = orderRepository.save(order);

        // Update distributor's outstanding amount
        distributorService.updateOutstandingAmount(distributor.getId(), totalAmount);

        log.info("Distributor order created successfully with number: {}", orderNumber);

        return orderMapper.toResponse(savedOrder);
    }

    private String generateOrderNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "DO-" + year + month + "-" + randomPart;

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            orderNumber = "DO-" + year + month + "-" + randomPart;
        }

        return orderNumber;
    }

    private BigDecimal calculateOrderTotal(DistributorOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        for (DistributorOrderItemRequest item : request.getItems()) {
            BigDecimal itemTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        return total;
    }

    private BigDecimal calculateItemDiscount(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal discountPercentage) {
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            return discountAmount;
        } else if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return subtotal.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateOrderDiscount(BigDecimal subtotal, BigDecimal discountAmount, BigDecimal discountPercentage) {
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            return discountAmount;
        } else if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return subtotal.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public DistributorOrderResponse updateOrder(Long id, DistributorOrderRequest request) {
        log.info("Updating order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        // Check if order can be updated
        if (order.getStatus() != DistributorOrderStatus.DRAFT &&
                order.getStatus() != DistributorOrderStatus.PENDING_APPROVAL) {
            throw new BusinessException("Cannot update order in " + order.getStatus() + " status");
        }

        // Update fields
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setShippingMethod(request.getShippingMethod());
        order.setNotes(request.getNotes());
        order.setUpdatedBy(order.getUser().getUsername());

        DistributorOrder updatedOrder = orderRepository.save(order);

        log.info("Order updated successfully with ID: {}", updatedOrder.getId());

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    public DistributorOrderResponse getOrderById(Long id) {
        log.debug("Fetching order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return orderMapper.toResponse(order);
    }

    @Override
    public DistributorOrderResponse getOrderByNumber(String orderNumber) {
        log.debug("Fetching order with number: {}", orderNumber);

        DistributorOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        return orderMapper.toResponse(order);
    }

    @Override
    public Page<DistributorOrderResponse> getAllOrders(Pageable pageable) {
        log.debug("Fetching all orders with pagination");

        return orderRepository.findAll(pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    public List<DistributorOrderResponse> getOrdersByDistributor(Long distributorId) {
        log.debug("Fetching orders for distributor ID: {}", distributorId);

        return orderRepository.findByDistributorId(distributorId).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DistributorOrderResponse> getOrdersByDistributor(Long distributorId, Pageable pageable) {
        log.debug("Fetching orders for distributor ID: {} with pagination", distributorId);

        return orderRepository.findByDistributorId(distributorId, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    public List<DistributorOrderResponse> getOrdersByStatus(DistributorOrderStatus status) {
        log.debug("Fetching orders with status: {}", status);

        return orderRepository.findByStatus(status).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DistributorOrderResponse> getOrdersByStatus(DistributorOrderStatus status, Pageable pageable) {
        log.debug("Fetching orders with status: {} with pagination", status);

        return orderRepository.findByStatus(status, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    public List<DistributorOrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching orders between {} and {}", startDate, endDate);

        return orderRepository.findByOrderDateBetween(startDate, endDate).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void approveOrder(Long id) {
        log.info("Approving order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != DistributorOrderStatus.PENDING_APPROVAL) {
            throw new BusinessException("Order is not pending approval");
        }

        order.setStatus(DistributorOrderStatus.APPROVED);
        order.setUpdatedBy(order.getUser().getUsername());
        orderRepository.save(order);

        log.info("Order approved successfully with ID: {}", id);
    }

    @Override
    public void processOrder(Long id) {
        log.info("Processing order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != DistributorOrderStatus.APPROVED) {
            throw new BusinessException("Order must be approved before processing");
        }

        order.setStatus(DistributorOrderStatus.PROCESSING);
        order.setUpdatedBy(order.getUser().getUsername());
        orderRepository.save(order);

        log.info("Order processing started with ID: {}", id);
    }

    @Override
    public void shipOrder(Long id, String trackingNumber) {
        log.info("Shipping order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != DistributorOrderStatus.PROCESSING) {
            throw new BusinessException("Order must be in processing status to ship");
        }

        // Update shipped quantities
        for (DistributorOrderItem item : order.getItems()) {
            item.setShippedQuantity(item.getQuantity());
        }

        order.setStatus(DistributorOrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        order.setUpdatedBy(order.getUser().getUsername());
        orderRepository.save(order);

        // Release reserved stock and reduce actual stock
        for (DistributorOrderItem item : order.getItems()) {
            inventoryService.releaseReservedStock(item.getProduct().getId(), 1L, item.getQuantity());
            inventoryService.removeStock(item.getProduct().getId(), 1L, item.getQuantity());
        }

        log.info("Order shipped successfully with ID: {}, tracking: {}", id, trackingNumber);
    }

    @Override
    public void deliverOrder(Long id) {
        log.info("Delivering order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != DistributorOrderStatus.SHIPPED) {
            throw new BusinessException("Order must be shipped before delivery");
        }

        order.setStatus(DistributorOrderStatus.DELIVERED);
        order.setDeliveredDate(LocalDateTime.now());
        order.setUpdatedBy(order.getUser().getUsername());
        orderRepository.save(order);

        // Update distributor's last order date
        distributorService.updateLastOrderDate(order.getDistributor().getId());

        log.info("Order delivered successfully with ID: {}", id);
    }

    @Override
    public void cancelOrder(Long id, String reason) {
        log.info("Cancelling order with ID: {}", id);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (order.getStatus() == DistributorOrderStatus.DELIVERED ||
                order.getStatus() == DistributorOrderStatus.CANCELLED) {
            throw new BusinessException("Order cannot be cancelled");
        }

        // Release reserved stock if not shipped
        if (order.getStatus() != DistributorOrderStatus.SHIPPED) {
            for (DistributorOrderItem item : order.getItems()) {
                inventoryService.releaseReservedStock(item.getProduct().getId(), 1L, item.getQuantity());
            }
        }

        order.setStatus(DistributorOrderStatus.CANCELLED);
        order.setNotes(order.getNotes() + " [CANCELLED: " + reason + "]");
        order.setUpdatedBy(order.getUser().getUsername());
        orderRepository.save(order);

        // Update distributor's outstanding amount (negative for cancellation)
        distributorService.updateOutstandingAmount(order.getDistributor().getId(), order.getTotalAmount().negate());

        log.info("Order cancelled successfully with ID: {}", id);
    }

    @Override
    public void updatePaymentStatus(Long id, Double paidAmount) {
        log.debug("Updating payment status for order ID: {} with paid amount: {}", id, paidAmount);

        DistributorOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        BigDecimal paid = BigDecimal.valueOf(paidAmount);
        BigDecimal newPaidAmount = order.getPaidAmount().add(paid);
        BigDecimal newPendingAmount = order.getTotalAmount().subtract(newPaidAmount);

        order.setPaidAmount(newPaidAmount);
        order.setPendingAmount(newPendingAmount);

        if (newPendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus("PARTIALLY_PAID");
        }

        orderRepository.save(order);

        // Update distributor's outstanding amount (negative for payments)
        distributorService.updateOutstandingAmount(order.getDistributor().getId(), paid.negate());
    }

    @Override
    public Double getTotalOrderAmount(LocalDateTime startDate, LocalDateTime endDate) {
        Double total = orderRepository.getTotalOrderAmountForPeriod(startDate, endDate);
        return total != null ? total : 0.0;
    }

    @Override
    public Long getPendingOrderCount() {
        return orderRepository.findByStatus(DistributorOrderStatus.PENDING_APPROVAL).stream().count();
    }

    @Override
    public List<DistributorOrderSummaryResponse> getRecentOrders(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("orderDate").descending());
        return orderRepository.findAll(pageable)
                .map(this::convertToSummaryResponse)
                .getContent();
    }

    private DistributorOrderSummaryResponse convertToSummaryResponse(DistributorOrder order) {
        return DistributorOrderSummaryResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .distributorName(order.getDistributor().getName())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .paymentStatus(order.getPaymentStatus())
                .build();
    }

    @Override
    public List<DistributorOrderSummaryResponse> getDelayedOrders() {
        LocalDateTime now = LocalDateTime.now();
        return orderRepository.findDelayedOrders(now).stream()
                .map(this::convertToSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isOrderNumberUnique(String orderNumber) {
        return !orderRepository.existsByOrderNumber(orderNumber);
    }
}