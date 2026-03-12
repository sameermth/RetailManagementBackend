package com.retailmanagement.modules.sales.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.customer.model.Customer;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.inventory.service.InventoryService;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.sales.dto.request.SaleItemRequest;
import com.retailmanagement.modules.sales.dto.request.SaleRequest;
import com.retailmanagement.modules.sales.dto.response.SaleResponse;
import com.retailmanagement.modules.sales.dto.response.SaleSummaryResponse;
import com.retailmanagement.modules.sales.enums.SaleStatus;
import com.retailmanagement.modules.sales.enums.PaymentStatus;
import com.retailmanagement.modules.sales.mapper.SaleMapper;
import com.retailmanagement.modules.sales.model.Sale;
import com.retailmanagement.modules.sales.model.SaleItem;
import com.retailmanagement.modules.sales.repository.SaleRepository;
import com.retailmanagement.modules.sales.service.SalesService;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.notification.service.NotificationService;
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
public class SalesServiceImpl implements SalesService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;
    private final SaleMapper saleMapper;

    @Override
    public SaleResponse createSale(SaleRequest request) {
        log.info("Creating new sale for customer ID: {}", request.getCustomerId());

        // Validate customer if provided
        Customer customer = null;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.getCustomerId()));
        }

        // Validate user
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Validate and process items
        List<SaleItem> saleItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            // Check stock availability (assuming default warehouse, you might want to specify warehouse)
            Long defaultWarehouseId = 1L; // This should be configurable
            if (!inventoryService.checkStockAvailability(product.getId(), defaultWarehouseId, itemRequest.getQuantity())) {
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
            BigDecimal taxRate = product.getGstRate() != null ?
                    product.getGstRate() : BigDecimal.ZERO;
            BigDecimal itemTax = itemSubtotal.subtract(itemDiscount)
                    .multiply(taxRate.divide(BigDecimal.valueOf(100)));

            BigDecimal itemTotal = itemSubtotal.subtract(itemDiscount).add(itemTax);

            // Create sale item
            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .discountAmount(itemDiscount)
                    .discountPercentage(itemRequest.getDiscountPercentage())
                    .taxRate(taxRate)
                    .taxAmount(itemTax)
                    .totalPrice(itemTotal)
                    .notes(itemRequest.getNotes())
                    .build();

            saleItems.add(saleItem);

            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);

            // Reserve stock
            inventoryService.reserveStock(product.getId(), defaultWarehouseId, itemRequest.getQuantity());
        }

        // Apply order level discount
        BigDecimal orderDiscount = calculateOrderDiscount(subtotal,
                request.getDiscountAmount(), request.getDiscountPercentage());

        BigDecimal totalAmount = subtotal
                .subtract(orderDiscount)
                .add(totalTax)
                .add(request.getShippingAmount() != null ? request.getShippingAmount() : BigDecimal.ZERO);

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Create sale
        Sale sale = Sale.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .user(user)
                .saleDate(request.getSaleDate() != null ? request.getSaleDate() : LocalDateTime.now())
                .items(saleItems)
                .subtotal(subtotal)
                .discountAmount(orderDiscount)
                .discountPercentage(request.getDiscountPercentage())
                .taxAmount(totalTax)
                .shippingAmount(request.getShippingAmount())
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(totalAmount)
                .status(SaleStatus.COMPLETED)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING.name())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .billingAddress(request.getBillingAddress())
                .shippingAddress(request.getShippingAddress())
                .build();

        // Set bidirectional relationship
        saleItems.forEach(item -> item.setSale(sale));

        Sale savedSale = saleRepository.save(sale);

        // Release reserved stock (convert to actual stock reduction)
        for (SaleItemRequest itemRequest : request.getItems()) {
            inventoryService.releaseReservedStock(itemRequest.getProductId(), 1L, itemRequest.getQuantity());
            // In a real implementation, you would also reduce actual stock here
        }

        // Send notification
        if (customer != null && customer.getEmail() != null) {
            notificationService.sendEmailNotification(
                    customer.getEmail(),
                    "Order Confirmation - Invoice " + invoiceNumber,
                    "Thank you for your order. Your invoice number is " + invoiceNumber
            );
        }

        log.info("Sale created successfully with invoice number: {}", invoiceNumber);

        return saleMapper.toResponse(savedSale);
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

    private String generateInvoiceNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String invoiceNumber = "INV-" + datePart + "-" + randomPart;

        // Ensure uniqueness
        while (saleRepository.existsByInvoiceNumber(invoiceNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            invoiceNumber = "INV-" + datePart + "-" + randomPart;
        }

        return invoiceNumber;
    }

    @Override
    public SaleResponse updateSale(Long id, SaleRequest request) {
        log.info("Updating sale with ID: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));

        // Check if sale can be updated
        if (sale.getStatus() == SaleStatus.COMPLETED || sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Cannot update completed or cancelled sale");
        }

        // Update basic fields
        sale.setNotes(request.getNotes());
        sale.setBillingAddress(request.getBillingAddress());
        sale.setShippingAddress(request.getShippingAddress());

        // Note: Updating items would require more complex logic (restocking, etc.)
        // For simplicity, we're not allowing item updates through this method

        Sale updatedSale = saleRepository.save(sale);
        log.info("Sale updated successfully with ID: {}", updatedSale.getId());

        return saleMapper.toResponse(updatedSale);
    }

    @Override
    public SaleResponse getSaleById(Long id) {
        log.debug("Fetching sale with ID: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));

        return saleMapper.toResponse(sale);
    }

    @Override
    public SaleResponse getSaleByInvoiceNumber(String invoiceNumber) {
        log.debug("Fetching sale with invoice number: {}", invoiceNumber);

        Sale sale = saleRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with invoice number: " + invoiceNumber));

        return saleMapper.toResponse(sale);
    }

    @Override
    public Page<SaleResponse> getAllSales(Pageable pageable) {
        log.debug("Fetching all sales with pagination");

        return saleRepository.findAll(pageable)
                .map(saleMapper::toResponse);
    }

    @Override
    public List<SaleResponse> getSalesByCustomer(Long customerId) {
        log.debug("Fetching sales for customer ID: {}", customerId);

        return saleRepository.findByCustomerId(customerId).stream()
                .map(saleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<SaleResponse> getSalesByCustomer(Long customerId, Pageable pageable) {
        log.debug("Fetching sales for customer ID: {} with pagination", customerId);

        return saleRepository.findByCustomerId(customerId, pageable)
                .map(saleMapper::toResponse);
    }

    @Override
    public List<SaleResponse> getSalesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching sales between {} and {}", startDate, endDate);

        return saleRepository.findBySaleDateBetween(startDate, endDate).stream()
                .map(saleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SaleResponse> getSalesByStatus(SaleStatus status) {
        log.debug("Fetching sales with status: {}", status);

        return saleRepository.findByStatus(status).stream()
                .map(saleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelSale(Long id, String reason) {
        log.info("Cancelling sale with ID: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));

        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new BusinessException("Sale is already cancelled");
        }

        // Restore stock for each item
        for (SaleItem item : sale.getItems()) {
            // In a real implementation, you would add stock back to inventory
            // inventoryService.addStock(item.getProduct().getId(), 1L, item.getQuantity());
        }

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setNotes(sale.getNotes() + " [Cancelled: " + reason + "]");
        saleRepository.save(sale);

        log.info("Sale cancelled successfully with ID: {}", id);
    }

    @Override
    public SaleResponse processReturn(Long id, String reason, List<Long> itemIds) {
        log.info("Processing return for sale ID: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));

        if (sale.getIsReturned()) {
            throw new BusinessException("Sale has already been returned");
        }

        // Process return for specific items or entire sale
        if (itemIds == null || itemIds.isEmpty()) {
            // Return entire sale
            for (SaleItem item : sale.getItems()) {
                // Restore stock
                // inventoryService.addStock(item.getProduct().getId(), 1L, item.getQuantity());
            }
            sale.setIsReturned(true);
            sale.setReturnReason(reason);
            sale.setStatus(SaleStatus.REFUNDED);
        } else {
            // Return specific items (partial return)
            for (SaleItem item : sale.getItems()) {
                if (itemIds.contains(item.getId())) {
                    // Restore stock for returned items
                    // inventoryService.addStock(item.getProduct().getId(), 1L, item.getQuantity());
                }
            }
            sale.setIsReturned(true);
            sale.setReturnReason(reason);
            sale.setStatus(SaleStatus.PARTIALLY_REFUNDED);
        }

        Sale updatedSale = saleRepository.save(sale);
        log.info("Return processed successfully for sale ID: {}", id);

        return saleMapper.toResponse(updatedSale);
    }

    @Override
    public void updatePaymentStatus(Long id) {
        log.debug("Updating payment status for sale ID: {}", id);

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));

        BigDecimal totalPaid = sale.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(com.retailmanagement.modules.sales.model.Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        sale.setPaidAmount(totalPaid);
        sale.setPendingAmount(sale.getTotalAmount().subtract(totalPaid));

        if (sale.getPendingAmount().compareTo(BigDecimal.ZERO) == 0) {
            sale.setPaymentStatus(PaymentStatus.PAID.name());
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus(PaymentStatus.PARTIALLY_PAID.name());
        }

        saleRepository.save(sale);
    }

    @Override
    public Double getTotalSales(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal total = saleRepository.getTotalSalesForPeriod(startDate, endDate);
        return total != null ? total.doubleValue() : 0.0;
    }

    @Override
    public Long getSalesCount(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.countSalesForPeriod(startDate, endDate);
    }

    @Override
    public List<SaleSummaryResponse> getRecentSales(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("saleDate").descending());
        return saleRepository.findAll(pageable)
                .map(saleMapper::toSummaryResponse)
                .getContent();
    }

    @Override
    public boolean isInvoiceNumberUnique(String invoiceNumber) {
        return !saleRepository.existsByInvoiceNumber(invoiceNumber);
    }
}