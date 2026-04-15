package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.inventory.service.InventoryBinService;
import com.retailmanagement.modules.erp.party.entity.Customer;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.SalesDispatch;
import com.retailmanagement.modules.erp.sales.entity.SalesDispatchLine;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.SalesDispatchLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesDispatchRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesDispatchService {

    private static final List<String> DISPATCH_CREATABLE_INVOICE_STATUSES = List.of(
            ErpDocumentStatuses.POSTED,
            ErpDocumentStatuses.PARTIALLY_PAID,
            ErpDocumentStatuses.PAID
    );

    private final SalesDispatchRepository salesDispatchRepository;
    private final SalesDispatchLineRepository salesDispatchLineRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final InventoryBinService inventoryBinService;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final AuditEventWriter auditEventWriter;

    @Transactional(readOnly = true)
    public List<SalesDispatch> listDispatches(Long organizationId, Long salesInvoiceId) {
        if (salesInvoiceId != null) {
            SalesInvoice invoice = salesInvoiceRepository.findById(salesInvoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + salesInvoiceId));
            accessGuard.assertOrganizationAccess(invoice.getOrganizationId());
            accessGuard.assertBranchAccess(invoice.getOrganizationId(), invoice.getBranchId());
            subscriptionAccessService.assertFeature(invoice.getOrganizationId(), "sales");
            return salesDispatchRepository.findBySalesInvoiceIdOrderByDispatchDateDescIdDesc(salesInvoiceId);
        }
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return salesDispatchRepository.findTop100ByOrganizationIdOrderByDispatchDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesDispatchResponse getDispatch(Long dispatchId) {
        SalesDispatch dispatch = salesDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales dispatch not found: " + dispatchId));
        accessGuard.assertOrganizationAccess(dispatch.getOrganizationId());
        accessGuard.assertBranchAccess(dispatch.getOrganizationId(), dispatch.getBranchId());
        subscriptionAccessService.assertFeature(dispatch.getOrganizationId(), "sales");
        return toDispatchResponse(dispatch);
    }

    public ErpSalesResponses.SalesDispatchResponse createDispatch(Long invoiceId,
                                                                  Long organizationId,
                                                                  Long branchId,
                                                                  ErpSalesDtos.CreateSalesDispatchRequest request) {
        SalesInvoice invoice = salesInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + invoiceId));
        accessGuard.assertBranchAccess(invoice.getOrganizationId(), invoice.getBranchId());
        subscriptionAccessService.assertFeature(invoice.getOrganizationId(), "sales");
        validateDispatchableInvoice(invoice);

        Customer customer = customerRepository.findByIdAndOrganizationId(invoice.getCustomerId(), invoice.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + invoice.getCustomerId()));
        List<SalesInvoiceLine> invoiceLines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
        Map<Long, SalesInvoiceLine> invoiceLineMap = invoiceLines.stream()
                .collect(LinkedHashMap::new, (map, line) -> map.put(line.getId(), line), LinkedHashMap::putAll);

        SalesDispatch dispatch = new SalesDispatch();
        dispatch.setOrganizationId(invoice.getOrganizationId());
        dispatch.setBranchId(branchId != null ? branchId : invoice.getBranchId());
        dispatch.setSalesInvoiceId(invoice.getId());
        dispatch.setWarehouseId(invoice.getWarehouseId());
        dispatch.setCustomerId(invoice.getCustomerId());
        dispatch.setDispatchNumber(nextDispatchNumber());
        dispatch.setDispatchDate(request.dispatchDate() == null ? LocalDate.now() : request.dispatchDate());
        dispatch.setExpectedDeliveryDate(request.expectedDeliveryDate());
        dispatch.setStatus(ErpDocumentStatuses.PICK_PENDING);
        dispatch.setTransporterName(blankToNull(request.transporterName()));
        dispatch.setTransporterId(blankToNull(request.transporterId()));
        dispatch.setVehicleNumber(blankToNull(request.vehicleNumber()));
        dispatch.setTrackingNumber(blankToNull(request.trackingNumber()));
        dispatch.setDeliveryAddress(firstNonBlank(request.deliveryAddress(), customer.getShippingAddress(), customer.getBillingAddress()));
        dispatch.setRemarks(blankToNull(request.remarks()));
        dispatch.setPackedAt(LocalDateTime.now());
        dispatch = salesDispatchRepository.save(dispatch);

        Map<Long, BigDecimal> alreadyDispatchedBaseByInvoiceLine = activeDispatchedBaseByInvoiceLine(invoice.getId(), null);
        BigDecimal totalDispatchBase = BigDecimal.ZERO;
        for (ErpSalesDtos.CreateSalesDispatchLineRequest requestLine : request.lines()) {
            SalesInvoiceLine invoiceLine = invoiceLineMap.get(requestLine.salesInvoiceLineId());
            if (invoiceLine == null) {
                throw new BusinessException("Dispatch line does not belong to invoice " + invoice.getInvoiceNumber());
            }
            BigDecimal remainingBaseQuantity = invoiceLine.getBaseQuantity()
                    .subtract(alreadyDispatchedBaseByInvoiceLine.getOrDefault(invoiceLine.getId(), BigDecimal.ZERO));
            if (remainingBaseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Invoice line " + invoiceLine.getId() + " is already fully dispatched");
            }
            if (requestLine.baseQuantity().compareTo(remainingBaseQuantity) > 0) {
                throw new BusinessException("Dispatch quantity exceeds remaining invoice quantity for invoice line " + invoiceLine.getId());
            }
            validateDispatchLineQuantity(invoiceLine, requestLine.quantity(), requestLine.baseQuantity());

            SalesDispatchLine dispatchLine = new SalesDispatchLine();
            dispatchLine.setSalesDispatchId(dispatch.getId());
            dispatchLine.setSalesInvoiceLineId(invoiceLine.getId());
            dispatchLine.setProductId(invoiceLine.getProductId());
            dispatchLine.setUomId(invoiceLine.getUomId());
            dispatchLine.setQuantity(requestLine.quantity());
            dispatchLine.setBaseQuantity(requestLine.baseQuantity());
            dispatchLine.setRemarks(blankToNull(requestLine.remarks()));
            salesDispatchLineRepository.save(dispatchLine);
            totalDispatchBase = totalDispatchBase.add(requestLine.baseQuantity());
        }

        auditEventWriter.write(
                dispatch.getOrganizationId(),
                dispatch.getBranchId(),
                "SALES_DISPATCH_CREATED",
                "sales_dispatch",
                dispatch.getId(),
                dispatch.getDispatchNumber(),
                "CREATE",
                dispatch.getWarehouseId(),
                dispatch.getCustomerId(),
                null,
                "Sales dispatch created",
                ErpJsonPayloads.of(
                        "dispatchNumber", dispatch.getDispatchNumber(),
                        "salesInvoiceId", dispatch.getSalesInvoiceId(),
                        "salesInvoiceNumber", invoice.getInvoiceNumber(),
                        "status", dispatch.getStatus(),
                        "baseQuantity", totalDispatchBase
                )
        );
        return toDispatchResponse(dispatch);
    }

    public ErpSalesResponses.SalesDispatchResponse updateDispatchStatus(Long dispatchId,
                                                                        ErpSalesDtos.UpdateSalesDispatchStatusRequest request) {
        SalesDispatch dispatch = salesDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales dispatch not found: " + dispatchId));
        accessGuard.assertBranchAccess(dispatch.getOrganizationId(), dispatch.getBranchId());
        subscriptionAccessService.assertFeature(dispatch.getOrganizationId(), "sales");

        String targetStatus = normalizeStatus(request.status());
        validateStatusTransition(dispatch.getStatus(), targetStatus);
        dispatch.setStatus(targetStatus);
        if (request.remarks() != null && !request.remarks().isBlank()) {
            dispatch.setRemarks(request.remarks().trim());
        }

        LocalDateTime now = LocalDateTime.now();
        if (ErpDocumentStatuses.PACKED.equals(targetStatus)) {
            dispatch.setPackedAt(dispatch.getPackedAt() == null ? now : dispatch.getPackedAt());
        } else if (ErpDocumentStatuses.DISPATCHED.equals(targetStatus)) {
            dispatch.setPackedAt(dispatch.getPackedAt() == null ? now : dispatch.getPackedAt());
            dispatch.setDispatchedAt(now);
        } else if (ErpDocumentStatuses.DELIVERED.equals(targetStatus)) {
            dispatch.setPackedAt(dispatch.getPackedAt() == null ? now : dispatch.getPackedAt());
            dispatch.setDispatchedAt(dispatch.getDispatchedAt() == null ? now : dispatch.getDispatchedAt());
            dispatch.setDeliveredAt(now);
        } else if (ErpDocumentStatuses.CANCELLED.equals(targetStatus)) {
            dispatch.setCancelledAt(now);
            dispatch.setCancelReason(firstNonBlank(request.remarks(), dispatch.getCancelReason(), "Cancelled by user"));
        }

        dispatch = salesDispatchRepository.save(dispatch);
        auditEventWriter.write(
                dispatch.getOrganizationId(),
                dispatch.getBranchId(),
                "SALES_DISPATCH_STATUS_UPDATED",
                "sales_dispatch",
                dispatch.getId(),
                dispatch.getDispatchNumber(),
                "STATUS_CHANGE",
                dispatch.getWarehouseId(),
                dispatch.getCustomerId(),
                null,
                "Sales dispatch status updated",
                ErpJsonPayloads.of("status", targetStatus)
        );
        return toDispatchResponse(dispatch);
    }

    public ErpSalesResponses.SalesDispatchResponse pickDispatch(Long dispatchId, ErpSalesDtos.PickSalesDispatchRequest request) {
        SalesDispatch dispatch = salesDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales dispatch not found: " + dispatchId));
        accessGuard.assertBranchAccess(dispatch.getOrganizationId(), dispatch.getBranchId());
        subscriptionAccessService.assertFeature(dispatch.getOrganizationId(), "sales");
        if (List.of(ErpDocumentStatuses.DISPATCHED, ErpDocumentStatuses.DELIVERED, ErpDocumentStatuses.CANCELLED)
                .contains(dispatch.getStatus())) {
            throw new BusinessException("Dispatch is no longer open for picking");
        }

        Map<Long, SalesDispatchLine> linesById = salesDispatchLineRepository.findBySalesDispatchIdOrderByIdAsc(dispatchId).stream()
                .collect(LinkedHashMap::new, (map, line) -> map.put(line.getId(), line), LinkedHashMap::putAll);
        for (ErpSalesDtos.PickSalesDispatchLineRequest requestLine : request.lines()) {
            SalesDispatchLine line = linesById.get(requestLine.salesDispatchLineId());
            if (line == null) {
                throw new BusinessException("Dispatch line does not belong to dispatch " + dispatch.getDispatchNumber());
            }
            validateDispatchLineQuantity(null, requestLine.pickedQuantity(), requestLine.pickedBaseQuantity(), line.getQuantity(), line.getBaseQuantity());
            if (requestLine.pickedBinLocationId() != null) {
                inventoryBinService.requireActiveBin(dispatch.getOrganizationId(), dispatch.getWarehouseId(), requestLine.pickedBinLocationId());
            }
            line.setPickedQuantity(requestLine.pickedQuantity());
            line.setPickedBaseQuantity(requestLine.pickedBaseQuantity());
            line.setPickedBinLocationId(requestLine.pickedBinLocationId());
            salesDispatchLineRepository.save(line);
        }
        dispatch.setPickedAt(LocalDateTime.now());
        dispatch.setStatus(resolvePickingStatus(dispatchId));
        dispatch = salesDispatchRepository.save(dispatch);
        return toDispatchResponse(dispatch);
    }

    public ErpSalesResponses.SalesDispatchResponse packDispatch(Long dispatchId, ErpSalesDtos.PackSalesDispatchRequest request) {
        SalesDispatch dispatch = salesDispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales dispatch not found: " + dispatchId));
        accessGuard.assertBranchAccess(dispatch.getOrganizationId(), dispatch.getBranchId());
        subscriptionAccessService.assertFeature(dispatch.getOrganizationId(), "sales");
        if (List.of(ErpDocumentStatuses.DISPATCHED, ErpDocumentStatuses.DELIVERED, ErpDocumentStatuses.CANCELLED)
                .contains(dispatch.getStatus())) {
            throw new BusinessException("Dispatch is no longer open for packing");
        }

        Map<Long, SalesDispatchLine> linesById = salesDispatchLineRepository.findBySalesDispatchIdOrderByIdAsc(dispatchId).stream()
                .collect(LinkedHashMap::new, (map, line) -> map.put(line.getId(), line), LinkedHashMap::putAll);
        for (ErpSalesDtos.PackSalesDispatchLineRequest requestLine : request.lines()) {
            SalesDispatchLine line = linesById.get(requestLine.salesDispatchLineId());
            if (line == null) {
                throw new BusinessException("Dispatch line does not belong to dispatch " + dispatch.getDispatchNumber());
            }
            BigDecimal maxQuantity = line.getPickedQuantity() != null ? line.getPickedQuantity() : line.getQuantity();
            BigDecimal maxBaseQuantity = line.getPickedBaseQuantity() != null ? line.getPickedBaseQuantity() : line.getBaseQuantity();
            validateDispatchLineQuantity(null, requestLine.packedQuantity(), requestLine.packedBaseQuantity(), maxQuantity, maxBaseQuantity);
            line.setPackedQuantity(requestLine.packedQuantity());
            line.setPackedBaseQuantity(requestLine.packedBaseQuantity());
            salesDispatchLineRepository.save(line);
        }
        dispatch.setPackedAt(LocalDateTime.now());
        dispatch.setStatus(resolvePackingStatus(dispatchId));
        dispatch = salesDispatchRepository.save(dispatch);
        return toDispatchResponse(dispatch);
    }

    @Transactional(readOnly = true)
    public ErpSalesResponses.SalesInvoiceDispatchSummaryResponse buildInvoiceDispatchSummary(Long salesInvoiceId) {
        SalesInvoice invoice = salesInvoiceRepository.findById(salesInvoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + salesInvoiceId));
        List<SalesInvoiceLine> invoiceLines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
        BigDecimal totalInvoiceBaseQuantity = invoiceLines.stream()
                .map(SalesInvoiceLine::getBaseQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalesDispatch> dispatches = salesDispatchRepository.findBySalesInvoiceIdOrderByDispatchDateDescIdDesc(salesInvoiceId);
        List<SalesDispatch> activeDispatches = dispatches.stream()
                .filter(dispatch -> !ErpDocumentStatuses.CANCELLED.equals(dispatch.getStatus()))
                .toList();
        Map<Long, List<SalesDispatchLine>> linesByDispatch = loadLinesByDispatch(activeDispatches);

        BigDecimal dispatchedBaseQuantity = BigDecimal.ZERO;
        BigDecimal deliveredBaseQuantity = BigDecimal.ZERO;
        for (SalesDispatch dispatch : activeDispatches) {
            BigDecimal dispatchBaseQuantity = linesByDispatch.getOrDefault(dispatch.getId(), List.of()).stream()
                    .map(SalesDispatchLine::getBaseQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dispatchedBaseQuantity = dispatchedBaseQuantity.add(dispatchBaseQuantity);
            if (ErpDocumentStatuses.DELIVERED.equals(dispatch.getStatus())) {
                deliveredBaseQuantity = deliveredBaseQuantity.add(dispatchBaseQuantity);
            }
        }

        BigDecimal pendingBaseQuantity = totalInvoiceBaseQuantity.subtract(dispatchedBaseQuantity).max(BigDecimal.ZERO);
        String status;
        if (dispatchedBaseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            status = ErpDocumentStatuses.NOT_DISPATCHED;
        } else if (deliveredBaseQuantity.compareTo(totalInvoiceBaseQuantity) >= 0 && totalInvoiceBaseQuantity.compareTo(BigDecimal.ZERO) > 0) {
            status = ErpDocumentStatuses.DELIVERED;
        } else if (dispatchedBaseQuantity.compareTo(totalInvoiceBaseQuantity) >= 0 && totalInvoiceBaseQuantity.compareTo(BigDecimal.ZERO) > 0) {
            status = ErpDocumentStatuses.DISPATCHED;
        } else {
            status = ErpDocumentStatuses.PARTIALLY_DISPATCHED;
        }

        LocalDate lastDispatchDate = activeDispatches.stream()
                .map(SalesDispatch::getDispatchDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ErpSalesResponses.SalesInvoiceDispatchSummaryResponse(
                status,
                activeDispatches.size(),
                dispatchedBaseQuantity,
                deliveredBaseQuantity,
                pendingBaseQuantity,
                lastDispatchDate
        );
    }

    private ErpSalesResponses.SalesDispatchResponse toDispatchResponse(SalesDispatch dispatch) {
        SalesInvoice invoice = salesInvoiceRepository.findById(dispatch.getSalesInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + dispatch.getSalesInvoiceId()));
        List<SalesDispatchLine> lines = salesDispatchLineRepository.findBySalesDispatchIdOrderByIdAsc(dispatch.getId());
        return new ErpSalesResponses.SalesDispatchResponse(
                dispatch.getId(),
                dispatch.getOrganizationId(),
                dispatch.getBranchId(),
                dispatch.getSalesInvoiceId(),
                invoice.getInvoiceNumber(),
                dispatch.getWarehouseId(),
                dispatch.getCustomerId(),
                dispatch.getDispatchNumber(),
                dispatch.getDispatchDate(),
                dispatch.getExpectedDeliveryDate(),
                dispatch.getStatus(),
                dispatch.getTransporterName(),
                dispatch.getTransporterId(),
                dispatch.getVehicleNumber(),
                dispatch.getTrackingNumber(),
                dispatch.getDeliveryAddress(),
                dispatch.getRemarks(),
                dispatch.getPickedAt(),
                dispatch.getPackedAt(),
                dispatch.getDispatchedAt(),
                dispatch.getDeliveredAt(),
                dispatch.getCancelledAt(),
                dispatch.getCancelReason(),
                lines.stream()
                        .map(line -> new ErpSalesResponses.SalesDispatchLineResponse(
                                line.getId(),
                                line.getSalesInvoiceLineId(),
                                line.getProductId(),
                                line.getUomId(),
                                line.getQuantity(),
                                line.getBaseQuantity(),
                                line.getPickedQuantity(),
                                line.getPickedBaseQuantity(),
                                line.getPickedBinLocationId(),
                                line.getPackedQuantity(),
                                line.getPackedBaseQuantity(),
                                line.getRemarks()
                        ))
                        .toList()
        );
    }

    private String resolvePickingStatus(Long dispatchId) {
        List<SalesDispatchLine> lines = salesDispatchLineRepository.findBySalesDispatchIdOrderByIdAsc(dispatchId);
        boolean anyPicked = lines.stream().anyMatch(line -> line.getPickedBaseQuantity() != null && line.getPickedBaseQuantity().compareTo(BigDecimal.ZERO) > 0);
        boolean allPicked = lines.stream().allMatch(line -> line.getPickedBaseQuantity() != null && line.getPickedBaseQuantity().compareTo(line.getBaseQuantity()) >= 0);
        if (!anyPicked) {
            return ErpDocumentStatuses.PICK_PENDING;
        }
        return allPicked ? ErpDocumentStatuses.PICKED : ErpDocumentStatuses.PARTIALLY_PICKED;
    }

    private String resolvePackingStatus(Long dispatchId) {
        List<SalesDispatchLine> lines = salesDispatchLineRepository.findBySalesDispatchIdOrderByIdAsc(dispatchId);
        boolean anyPacked = lines.stream().anyMatch(line -> line.getPackedBaseQuantity() != null && line.getPackedBaseQuantity().compareTo(BigDecimal.ZERO) > 0);
        boolean allPacked = lines.stream().allMatch(line -> line.getPackedBaseQuantity() != null && line.getPackedBaseQuantity().compareTo(line.getBaseQuantity()) >= 0);
        if (!anyPacked) {
            return resolvePickingStatus(dispatchId);
        }
        return allPacked ? ErpDocumentStatuses.PACKED : ErpDocumentStatuses.PARTIALLY_PACKED;
    }

    private void validateDispatchLineQuantity(SalesInvoiceLine ignoredInvoiceLine,
                                              BigDecimal quantity,
                                              BigDecimal baseQuantity,
                                              BigDecimal allowedQuantity,
                                              BigDecimal allowedBaseQuantity) {
        if (quantity == null || baseQuantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0 || baseQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Dispatch quantities must be greater than zero");
        }
        if (allowedQuantity != null && quantity.compareTo(allowedQuantity) > 0) {
            throw new BusinessException("Dispatch quantity exceeds allowed line quantity");
        }
        if (allowedBaseQuantity != null && baseQuantity.compareTo(allowedBaseQuantity) > 0) {
            throw new BusinessException("Dispatch base quantity exceeds allowed line quantity");
        }
    }

    private Map<Long, BigDecimal> activeDispatchedBaseByInvoiceLine(Long salesInvoiceId, Long excludeDispatchId) {
        List<SalesDispatch> dispatches = salesDispatchRepository.findBySalesInvoiceIdOrderByDispatchDateDescIdDesc(salesInvoiceId).stream()
                .filter(dispatch -> !ErpDocumentStatuses.CANCELLED.equals(dispatch.getStatus()))
                .filter(dispatch -> excludeDispatchId == null || !excludeDispatchId.equals(dispatch.getId()))
                .toList();
        Map<Long, BigDecimal> result = new HashMap<>();
        Map<Long, List<SalesDispatchLine>> linesByDispatch = loadLinesByDispatch(dispatches);
        for (SalesDispatch dispatch : dispatches) {
            for (SalesDispatchLine line : linesByDispatch.getOrDefault(dispatch.getId(), List.of())) {
                result.merge(line.getSalesInvoiceLineId(), line.getBaseQuantity(), BigDecimal::add);
            }
        }
        return result;
    }

    private Map<Long, List<SalesDispatchLine>> loadLinesByDispatch(List<SalesDispatch> dispatches) {
        Map<Long, List<SalesDispatchLine>> result = new HashMap<>();
        if (dispatches.isEmpty()) {
            return result;
        }
        List<SalesDispatchLine> dispatchLines = salesDispatchLineRepository.findBySalesDispatchIdIn(
                dispatches.stream().map(SalesDispatch::getId).toList()
        );
        for (SalesDispatchLine dispatchLine : dispatchLines) {
            result.computeIfAbsent(dispatchLine.getSalesDispatchId(), ignored -> new ArrayList<>()).add(dispatchLine);
        }
        return result;
    }

    private void validateDispatchableInvoice(SalesInvoice invoice) {
        if (!DISPATCH_CREATABLE_INVOICE_STATUSES.contains(invoice.getStatus())) {
            throw new BusinessException("Dispatch can be created only for posted or financially active invoices");
        }
    }

    private void validateDispatchLineQuantity(SalesInvoiceLine invoiceLine,
                                              BigDecimal quantity,
                                              BigDecimal baseQuantity) {
        if (invoiceLine.getBaseQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Invoice line " + invoiceLine.getId() + " has invalid base quantity");
        }
        BigDecimal expectedQuantity = invoiceLine.getQuantity()
                .multiply(baseQuantity)
                .divide(invoiceLine.getBaseQuantity(), 6, RoundingMode.HALF_UP);
        if (expectedQuantity.compareTo(quantity.setScale(6, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessException("Dispatch quantity must match invoice line conversion for line " + invoiceLine.getId());
        }
    }

    private void validateStatusTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equals(targetStatus)) {
            return;
        }
        List<String> allowedTargets = switch (currentStatus) {
            case ErpDocumentStatuses.PICK_PENDING, ErpDocumentStatuses.PARTIALLY_PICKED, ErpDocumentStatuses.PICKED,
                 ErpDocumentStatuses.PARTIALLY_PACKED -> List.of(ErpDocumentStatuses.PACKED, ErpDocumentStatuses.DISPATCHED, ErpDocumentStatuses.CANCELLED);
            case ErpDocumentStatuses.PACKED -> List.of(ErpDocumentStatuses.DISPATCHED, ErpDocumentStatuses.CANCELLED);
            case ErpDocumentStatuses.DISPATCHED -> List.of(ErpDocumentStatuses.DELIVERED, ErpDocumentStatuses.CANCELLED);
            case ErpDocumentStatuses.DELIVERED, ErpDocumentStatuses.CANCELLED -> List.of();
            default -> List.of(ErpDocumentStatuses.PACKED, ErpDocumentStatuses.DISPATCHED, ErpDocumentStatuses.CANCELLED);
        };
        if (!allowedTargets.contains(targetStatus)) {
            throw new BusinessException("Invalid dispatch status transition from " + currentStatus + " to " + targetStatus);
        }
    }

    private String nextDispatchNumber() {
        return "DSP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of(
                ErpDocumentStatuses.PICK_PENDING,
                ErpDocumentStatuses.PICKED,
                ErpDocumentStatuses.PARTIALLY_PICKED,
                ErpDocumentStatuses.PARTIALLY_PACKED,
                ErpDocumentStatuses.PACKED,
                ErpDocumentStatuses.DISPATCHED,
                ErpDocumentStatuses.DELIVERED,
                ErpDocumentStatuses.CANCELLED
        ).contains(normalized)) {
            throw new BusinessException("Dispatch status must be pick/pick-partial/pack/dispatch/deliver/cancel state");
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
