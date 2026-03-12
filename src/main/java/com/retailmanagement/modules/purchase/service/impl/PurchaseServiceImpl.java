package com.retailmanagement.modules.purchase.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.inventory.service.InventoryService;
import com.retailmanagement.modules.product.model.Product;
import com.retailmanagement.modules.product.repository.ProductRepository;
import com.retailmanagement.modules.purchase.dto.request.PurchaseItemRequest;
import com.retailmanagement.modules.purchase.dto.request.PurchaseReceiptRequest;
import com.retailmanagement.modules.purchase.dto.request.PurchaseRequest;
import com.retailmanagement.modules.purchase.dto.request.ReceiptItemRequest;
import com.retailmanagement.modules.purchase.dto.response.PurchaseReceiptResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseResponse;
import com.retailmanagement.modules.purchase.dto.response.PurchaseSummaryResponse;
import com.retailmanagement.modules.purchase.enums.PurchaseStatus;
import com.retailmanagement.modules.purchase.mapper.PurchaseItemMapper;
import com.retailmanagement.modules.purchase.mapper.PurchaseMapper;
import com.retailmanagement.modules.purchase.mapper.PurchaseReceiptMapper;
import com.retailmanagement.modules.purchase.model.Purchase;
import com.retailmanagement.modules.purchase.model.PurchaseItem;
import com.retailmanagement.modules.purchase.model.PurchaseReceipt;
import com.retailmanagement.modules.purchase.model.PurchaseReceiptItem;
import com.retailmanagement.modules.purchase.repository.PurchaseItemRepository;
import com.retailmanagement.modules.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.purchase.repository.PurchaseRepository;
import com.retailmanagement.modules.purchase.service.PurchaseService;
import com.retailmanagement.modules.supplier.model.Supplier;
import com.retailmanagement.modules.supplier.repository.SupplierRepository;
import com.retailmanagement.modules.supplier.service.SupplierService;
import com.retailmanagement.modules.auth.model.User;
import com.retailmanagement.modules.auth.repository.UserRepository;
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
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final PurchaseReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final PurchaseMapper purchaseMapper;
    private final PurchaseItemMapper purchaseItemMapper;
    private final PurchaseReceiptMapper receiptMapper;

    @Override
    public PurchaseResponse createPurchase(PurchaseRequest request) {
        log.info("Creating purchase order for supplier ID: {}", request.getSupplierId());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.getSupplierId()));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        // Validate and process items
        List<PurchaseItem> purchaseItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (PurchaseItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            // Calculate item prices
            BigDecimal unitPrice = itemRequest.getUnitPrice() != null ?
                    itemRequest.getUnitPrice() : product.getCostPrice();

            if (unitPrice == null) {
                throw new BusinessException("Unit price is required for product: " + product.getName());
            }

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

            // Create purchase item
            PurchaseItem purchaseItem = PurchaseItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .receivedQuantity(0)
                    .unitPrice(unitPrice)
                    .discountAmount(itemDiscount)
                    .discountPercentage(itemRequest.getDiscountPercentage())
                    .taxRate(taxRate)
                    .taxAmount(itemTax)
                    .totalPrice(itemTotal)
                    .notes(itemRequest.getNotes())
                    .build();

            purchaseItems.add(purchaseItem);

            subtotal = subtotal.add(itemSubtotal);
            totalTax = totalTax.add(itemTax);
        }

        // Apply order level discount
        BigDecimal orderDiscount = calculateOrderDiscount(subtotal,
                request.getDiscountAmount(), request.getDiscountPercentage());

        BigDecimal totalAmount = subtotal
                .subtract(orderDiscount)
                .add(totalTax)
                .add(request.getShippingAmount() != null ? request.getShippingAmount() : BigDecimal.ZERO);

        // Generate purchase order number
        String purchaseOrderNumber = generatePurchaseOrderNumber();

        // Create purchase
        Purchase purchase = Purchase.builder()
                .purchaseOrderNumber(purchaseOrderNumber)
                .supplier(supplier)
                .user(user)
                .orderDate(LocalDateTime.now())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseStatus.PENDING_APPROVAL)
                .items(purchaseItems)
                .subtotal(subtotal)
                .discountAmount(orderDiscount)
                .discountPercentage(request.getDiscountPercentage())
                .taxAmount(totalTax)
                .shippingAmount(request.getShippingAmount())
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO)
                .pendingAmount(totalAmount)
                .paymentStatus("PENDING")
                .paymentTerms(request.getPaymentTerms())
                .shippingMethod(request.getShippingMethod())
                .notes(request.getNotes())
                .termsAndConditions(request.getTermsAndConditions())
                .createdBy(user.getUsername())
                .updatedBy(user.getUsername())
                .build();

        // Set bidirectional relationship
        purchaseItems.forEach(item -> item.setPurchase(purchase));

        Purchase savedPurchase = purchaseRepository.save(purchase);

        log.info("Purchase order created successfully with number: {}", purchaseOrderNumber);

        return purchaseMapper.toResponse(savedPurchase);
    }

    private String generatePurchaseOrderNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String poNumber = "PO-" + year + month + "-" + randomPart;

        while (purchaseRepository.existsByPurchaseOrderNumber(poNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            poNumber = "PO-" + year + month + "-" + randomPart;
        }

        return poNumber;
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
    public PurchaseResponse updatePurchase(Long id, PurchaseRequest request) {
        log.info("Updating purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));

        // Check if purchase can be updated
        if (purchase.getStatus() != PurchaseStatus.DRAFT &&
                purchase.getStatus() != PurchaseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Cannot update purchase order in " + purchase.getStatus() + " status");
        }

        // Update basic fields
        purchase.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        purchase.setPaymentTerms(request.getPaymentTerms());
        purchase.setShippingMethod(request.getShippingMethod());
        purchase.setNotes(request.getNotes());
        purchase.setTermsAndConditions(request.getTermsAndConditions());
        purchase.setUpdatedBy(purchase.getUser().getUsername());

        Purchase updatedPurchase = purchaseRepository.save(purchase);

        log.info("Purchase order updated successfully with ID: {}", updatedPurchase.getId());

        return purchaseMapper.toResponse(updatedPurchase);
    }

    @Override
    public PurchaseResponse getPurchaseById(Long id) {
        log.debug("Fetching purchase with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));

        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public PurchaseResponse getPurchaseByOrderNumber(String orderNumber) {
        log.debug("Fetching purchase with order number: {}", orderNumber);

        Purchase purchase = purchaseRepository.findByPurchaseOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with order number: " + orderNumber));

        return purchaseMapper.toResponse(purchase);
    }

    @Override
    public Page<PurchaseResponse> getAllPurchases(Pageable pageable) {
        log.debug("Fetching all purchases with pagination");

        return purchaseRepository.findAll(pageable)
                .map(purchaseMapper::toResponse);
    }

    @Override
    public List<PurchaseResponse> getPurchasesBySupplier(Long supplierId) {
        log.debug("Fetching purchases for supplier ID: {}", supplierId);

        return purchaseRepository.findBySupplierId(supplierId).stream()
                .map(purchaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PurchaseResponse> getPurchasesBySupplier(Long supplierId, Pageable pageable) {
        log.debug("Fetching purchases for supplier ID: {} with pagination", supplierId);

        return purchaseRepository.findBySupplierId(supplierId, pageable)
                .map(purchaseMapper::toResponse);
    }

    @Override
    public List<PurchaseResponse> getPurchasesByStatus(PurchaseStatus status) {
        log.debug("Fetching purchases with status: {}", status);

        return purchaseRepository.findByStatus(status).stream()
                .map(purchaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PurchaseResponse> getPurchasesByStatus(PurchaseStatus status, Pageable pageable) {
        log.debug("Fetching purchases with status: {} with pagination", status);

        return purchaseRepository.findByStatus(status, pageable)
                .map(purchaseMapper::toResponse);
    }

    @Override
    public List<PurchaseResponse> getPurchasesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Fetching purchases between {} and {}", startDate, endDate);

        return purchaseRepository.findByOrderDateBetween(startDate, endDate).stream()
                .map(purchaseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void approvePurchase(Long id) {
        log.info("Approving purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));

        if (purchase.getStatus() != PurchaseStatus.PENDING_APPROVAL) {
            throw new BusinessException("Purchase order is not pending approval");
        }

        purchase.setStatus(PurchaseStatus.APPROVED);
        purchase.setUpdatedBy(purchase.getUser().getUsername());
        purchaseRepository.save(purchase);

        log.info("Purchase order approved successfully with ID: {}", id);
    }

    @Override
    public void cancelPurchase(Long id, String reason) {
        log.info("Cancelling purchase order with ID: {}", id);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));

        if (purchase.getStatus() == PurchaseStatus.COMPLETED ||
                purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new BusinessException("Purchase order cannot be cancelled");
        }

        purchase.setStatus(PurchaseStatus.CANCELLED);
        purchase.setNotes(purchase.getNotes() + " [CANCELLED: " + reason + "]");
        purchase.setUpdatedBy(purchase.getUser().getUsername());
        purchaseRepository.save(purchase);

        log.info("Purchase order cancelled successfully with ID: {}", id);
    }

    @Override
    public PurchaseReceiptResponse receivePurchase(PurchaseReceiptRequest request) {
        log.info("Processing receipt for purchase ID: {}", request.getPurchaseId());

        Purchase purchase = purchaseRepository.findById(request.getPurchaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + request.getPurchaseId()));

        if (purchase.getStatus() == PurchaseStatus.COMPLETED) {
            throw new BusinessException("Purchase order is already completed");
        }

        // Generate receipt number
        String receiptNumber = generateReceiptNumber();

        PurchaseReceipt receipt = PurchaseReceipt.builder()
                .receiptNumber(receiptNumber)
                .purchase(purchase)
                .receiptDate(request.getReceiptDate() != null ? request.getReceiptDate() : LocalDateTime.now())
                .receivedBy(purchase.getUser().getUsername())
                .items(new ArrayList<>())
                .notes(request.getNotes())
                .build();

        boolean allItemsReceived = true;
        boolean anyItemReceived = false;

        // Process each received item
        for (ReceiptItemRequest itemRequest : request.getItems()) {
            PurchaseItem purchaseItem = purchaseItemRepository.findById(itemRequest.getPurchaseItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase item not found"));

            if (!purchaseItem.getPurchase().getId().equals(purchase.getId())) {
                throw new BusinessException("Purchase item does not belong to this purchase");
            }

            int newReceivedQuantity = purchaseItem.getReceivedQuantity() + itemRequest.getQuantityReceived();

            if (newReceivedQuantity > purchaseItem.getQuantity()) {
                throw new BusinessException("Received quantity exceeds ordered quantity for item: " +
                        purchaseItem.getProduct().getName());
            }

            // Update purchase item
            purchaseItem.setReceivedQuantity(newReceivedQuantity);
            purchaseItemRepository.save(purchaseItem);

            // Create receipt item
            PurchaseReceiptItem receiptItem = PurchaseReceiptItem.builder()
                    .receipt(receipt)
                    .purchaseItem(purchaseItem)
                    .product(purchaseItem.getProduct())
                    .quantityReceived(itemRequest.getQuantityReceived())
                    .batchNumber(itemRequest.getBatchNumber())
                    .expiryDate(itemRequest.getExpiryDate())
                    .location(itemRequest.getLocation())
                    .build();

            receipt.getItems().add(receiptItem);

            // Update inventory
            inventoryService.addStock(
                    purchaseItem.getProduct().getId(),
                    1L, // Default warehouse, should be configurable
                    itemRequest.getQuantityReceived()
            );

            // Check if all items are received
            if (newReceivedQuantity < purchaseItem.getQuantity()) {
                allItemsReceived = false;
            }
            anyItemReceived = true;
        }

        // Update purchase status
        if (allItemsReceived) {
            purchase.setStatus(PurchaseStatus.COMPLETED);
            purchase.setReceivedDate(LocalDateTime.now());
        } else if (anyItemReceived) {
            purchase.setStatus(PurchaseStatus.PARTIALLY_RECEIVED);
        }

        purchase.setUpdatedBy(purchase.getUser().getUsername());
        purchaseRepository.save(purchase);

        // Update supplier's last purchase date
        supplierService.updateLastPurchaseDate(purchase.getSupplier().getId());

        PurchaseReceipt savedReceipt = receiptRepository.save(receipt);
        log.info("Receipt processed successfully with number: {}", receiptNumber);

        return receiptMapper.toResponse(savedReceipt);
    }

    private String generateReceiptNumber() {
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String receiptNumber = "RCPT-" + year + month + "-" + randomPart;

        while (receiptRepository.existsByReceiptNumber(receiptNumber)) {
            randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            receiptNumber = "RCPT-" + year + month + "-" + randomPart;
        }

        return receiptNumber;
    }

    @Override
    public void updatePaymentStatus(Long id, Double paidAmount) {
        log.debug("Updating payment status for purchase ID: {} with paid amount: {}", id, paidAmount);

        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));

        BigDecimal paid = BigDecimal.valueOf(paidAmount);
        BigDecimal newPaidAmount = purchase.getPaidAmount().add(paid);
        BigDecimal newPendingAmount = purchase.getTotalAmount().subtract(newPaidAmount);

        purchase.setPaidAmount(newPaidAmount);
        purchase.setPendingAmount(newPendingAmount);

        if (newPendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            purchase.setPaymentStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            purchase.setPaymentStatus("PARTIALLY_PAID");
        }

        purchaseRepository.save(purchase);

        // Update supplier's outstanding amount (negative for payments)
        supplierService.updateOutstandingAmount(purchase.getSupplier().getId(), paid.negate());
    }

    @Override
    public Double getTotalPurchaseAmount(LocalDateTime startDate, LocalDateTime endDate) {
        Double total = purchaseRepository.getTotalPurchaseAmountForPeriod(startDate, endDate);
        return total != null ? total : 0.0;
    }

    @Override
    public Long getPendingApprovalCount() {
        return purchaseRepository.countPendingApproval();
    }

    @Override
    public List<PurchaseSummaryResponse> getRecentPurchases(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("orderDate").descending());
        return purchaseRepository.findAll(pageable)
                .map(purchaseMapper::toSummaryResponse)
                .getContent();
    }

    @Override
    public boolean isPurchaseOrderNumberUnique(String orderNumber) {
        return !purchaseRepository.existsByPurchaseOrderNumber(orderNumber);
    }
}