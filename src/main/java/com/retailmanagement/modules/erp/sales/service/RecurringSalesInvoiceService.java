package com.retailmanagement.modules.erp.sales.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.catalog.repository.StoreProductRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.util.RecurringScheduleSupport;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.ErpSalesResponses;
import com.retailmanagement.modules.erp.sales.dto.RecurringSalesDtos;
import com.retailmanagement.modules.erp.sales.dto.RecurringSalesResponses;
import com.retailmanagement.modules.erp.sales.entity.RecurringSalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.RecurringSalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.RecurringSalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.RecurringSalesInvoiceRepository;
import com.retailmanagement.modules.erp.subscription.service.SubscriptionAccessService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecurringSalesInvoiceService {

    private final RecurringSalesInvoiceRepository recurringSalesInvoiceRepository;
    private final RecurringSalesInvoiceLineRepository recurringSalesInvoiceLineRepository;
    private final CustomerRepository customerRepository;
    private final StoreProductRepository storeProductRepository;
    private final UomRepository uomRepository;
    private final ErpAccessGuard accessGuard;
    private final SubscriptionAccessService subscriptionAccessService;
    private final ErpSalesService erpSalesService;

    @Transactional(readOnly = true)
    public List<RecurringSalesInvoice> list(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        return recurringSalesInvoiceRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public RecurringSalesResponses.RecurringSalesInvoiceResponse get(Long organizationId, Long id) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        RecurringSalesInvoice template = recurringSalesInvoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring sales invoice not found: " + id));
        return toResponse(template, recurringSalesInvoiceLineRepository.findByRecurringSalesInvoiceIdOrderByIdAsc(id));
    }

    public RecurringSalesResponses.RecurringSalesInvoiceResponse create(Long organizationId, Long branchId,
                                                                        RecurringSalesDtos.CreateRecurringSalesInvoiceRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        customerRepository.findByIdAndOrganizationId(request.customerId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        String frequency = RecurringScheduleSupport.normalizeFrequency(request.frequency());
        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        LocalDate nextRunDate = request.nextRunDate() == null ? startDate : request.nextRunDate();
        if (request.endDate() != null && request.endDate().isBefore(nextRunDate)) {
            throw new BusinessException("End date cannot be before next run date");
        }

        RecurringSalesInvoice template = new RecurringSalesInvoice();
        template.setOrganizationId(organizationId);
        template.setBranchId(branchId);
        template.setWarehouseId(request.warehouseId());
        template.setCustomerId(request.customerId());
        template.setPriceListId(request.priceListId());
        template.setTemplateNumber(generateTemplateNumber());
        template.setFrequency(frequency);
        template.setStartDate(startDate);
        template.setNextRunDate(nextRunDate);
        template.setEndDate(request.endDate());
        template.setDueDays(request.dueDays());
        template.setPlaceOfSupplyStateCode(request.placeOfSupplyStateCode());
        template.setRemarks(request.remarks());
        template.setIsActive(request.isActive() == null || request.isActive());
        template = recurringSalesInvoiceRepository.save(template);

        List<RecurringSalesInvoiceLine> lines = new ArrayList<>();
        for (RecurringSalesDtos.CreateRecurringSalesInvoiceLineRequest requestLine : request.lines()) {
            var storeProduct = storeProductRepository.findById(requestLine.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Store product not found: " + requestLine.productId()));
            uomRepository.findById(requestLine.uomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UOM not found: " + requestLine.uomId()));
            RecurringSalesInvoiceLine line = new RecurringSalesInvoiceLine();
            line.setRecurringSalesInvoiceId(template.getId());
            line.setProductId(requestLine.productId());
            line.setUomId(requestLine.uomId());
            line.setQuantity(requestLine.quantity());
            line.setBaseQuantity(requestLine.baseQuantity());
            line.setUnitPrice(requestLine.unitPrice());
            line.setDiscountAmount(requestLine.discountAmount() == null ? java.math.BigDecimal.ZERO : requestLine.discountAmount());
            line.setWarrantyMonths(requestLine.warrantyMonths() != null ? requestLine.warrantyMonths() : storeProduct.getDefaultWarrantyMonths());
            line.setRemarks(requestLine.remarks());
            lines.add(recurringSalesInvoiceLineRepository.save(line));
        }
        return toResponse(template, lines);
    }

    public ErpSalesResponses.SalesInvoiceResponse run(Long organizationId, Long id, LocalDate requestedRunDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        subscriptionAccessService.assertFeature(organizationId, "sales");
        RecurringSalesInvoice template = recurringSalesInvoiceRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring sales invoice not found: " + id));
        return generateInvoice(template, requestedRunDate, true);
    }

    @Scheduled(fixedDelayString = "${erp.recurring.sales.scan-ms:3600000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runDueTemplates() {
        LocalDate today = LocalDate.now();
        for (RecurringSalesInvoice template : recurringSalesInvoiceRepository
                .findByIsActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(today)) {
            if (Boolean.FALSE.equals(template.getIsActive())) {
                continue;
            }
            if (template.getEndDate() != null && template.getNextRunDate().isAfter(template.getEndDate())) {
                template.setIsActive(false);
                recurringSalesInvoiceRepository.save(template);
                continue;
            }
            try {
                generateInvoice(template, template.getNextRunDate(), false);
            } catch (RuntimeException ex) {
                log.warn("Recurring sales invoice template {} failed during scheduled run: {}",
                        template.getId(), ex.getMessage());
            }
        }
    }

    private ErpSalesResponses.SalesInvoiceResponse generateInvoice(RecurringSalesInvoice template, LocalDate runDate, boolean enforceUserContext) {
        LocalDate effectiveRunDate = runDate == null ? template.getNextRunDate() : runDate;
        if (template.getEndDate() != null && effectiveRunDate.isAfter(template.getEndDate())) {
            throw new BusinessException("Recurring sales invoice is past its end date");
        }
        List<RecurringSalesInvoiceLine> lines = recurringSalesInvoiceLineRepository.findByRecurringSalesInvoiceIdOrderByIdAsc(template.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("Recurring sales invoice has no lines");
        }
        List<ErpSalesDtos.CreateSalesInvoiceLineRequest> invoiceLines = lines.stream()
                .map(line -> new ErpSalesDtos.CreateSalesInvoiceLineRequest(
                        line.getProductId(),
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        line.getUnitPrice(),
                        null,
                        null,
                        line.getDiscountAmount(),
                        null,
                        null,
                        line.getWarrantyMonths()
                )).toList();
        LocalDate dueDate = template.getDueDays() == null ? effectiveRunDate : effectiveRunDate.plusDays(template.getDueDays());
        ErpSalesDtos.CreateSalesInvoiceRequest request = new ErpSalesDtos.CreateSalesInvoiceRequest(
                template.getOrganizationId(),
                template.getBranchId(),
                template.getWarehouseId(),
                template.getCustomerId(),
                template.getPriceListId(),
                effectiveRunDate,
                dueDate,
                template.getPlaceOfSupplyStateCode(),
                template.getRemarks(),
                invoiceLines
        );
        ErpSalesResponses.SalesInvoiceResponse response = enforceUserContext
                ? erpSalesService.createInvoice(template.getOrganizationId(), template.getBranchId(), request)
                : erpSalesService.createInvoiceSystemGenerated(template.getOrganizationId(), template.getBranchId(), request);
        template.setLastRunAt(LocalDateTime.now());
        template.setLastSalesInvoiceId(response.id());
        LocalDate nextRun = RecurringScheduleSupport.nextRunDate(template.getFrequency(), effectiveRunDate);
        template.setNextRunDate(nextRun);
        if (template.getEndDate() != null && nextRun.isAfter(template.getEndDate())) {
            template.setIsActive(false);
        }
        recurringSalesInvoiceRepository.save(template);
        return response;
    }

    private RecurringSalesResponses.RecurringSalesInvoiceResponse toResponse(RecurringSalesInvoice template, List<RecurringSalesInvoiceLine> lines) {
        return new RecurringSalesResponses.RecurringSalesInvoiceResponse(
                template.getId(),
                template.getOrganizationId(),
                template.getBranchId(),
                template.getWarehouseId(),
                template.getCustomerId(),
                template.getPriceListId(),
                template.getTemplateNumber(),
                template.getFrequency(),
                template.getStartDate(),
                template.getNextRunDate(),
                template.getEndDate(),
                template.getDueDays(),
                template.getPlaceOfSupplyStateCode(),
                template.getRemarks(),
                template.getIsActive(),
                template.getLastRunAt(),
                template.getLastSalesInvoiceId(),
                lines.stream().map(line -> new RecurringSalesResponses.RecurringSalesInvoiceLineResponse(
                        line.getId(),
                        line.getProductId(),
                        line.getUomId(),
                        line.getQuantity(),
                        line.getBaseQuantity(),
                        line.getUnitPrice(),
                        line.getDiscountAmount(),
                        line.getWarrantyMonths(),
                        line.getRemarks()
                )).toList()
        );
    }

    private String generateTemplateNumber() {
        return "RSI-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }
}
