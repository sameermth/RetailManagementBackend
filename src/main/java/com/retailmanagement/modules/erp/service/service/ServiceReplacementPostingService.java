package com.retailmanagement.modules.erp.service.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.common.constants.ErpInventoryMovementTypes;
import com.retailmanagement.modules.erp.common.util.ErpJsonPayloads;
import com.retailmanagement.modules.erp.finance.service.ErpAccountingPostingService;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.inventory.service.InventoryPostingService;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.ProductOwnership;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.repository.ProductOwnershipRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import com.retailmanagement.modules.erp.service.entity.ServiceTicket;
import com.retailmanagement.modules.erp.service.entity.ServiceTicketItem;
import com.retailmanagement.modules.erp.service.entity.WarrantyClaim;
import com.retailmanagement.modules.erp.service.repository.ServiceReplacementRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceTicketItemRepository;
import com.retailmanagement.modules.erp.service.repository.ServiceTicketRepository;
import com.retailmanagement.modules.erp.service.repository.WarrantyClaimRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ServiceReplacementPostingService {

    private final ServiceReplacementRepository serviceReplacementRepository;
    private final ProductOwnershipRepository productOwnershipRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final InventoryPostingService inventoryPostingService;
    private final ErpAccountingPostingService accountingPostingService;
    private final SalesReturnRepository salesReturnRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketItemRepository serviceTicketItemRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final AuditEventWriter auditEventWriter;

    public ServiceReplacement finalizeApprovedReplacement(Long replacementId) {
        ServiceReplacement replacement = serviceReplacementRepository.findById(replacementId)
                .orElseThrow(() -> new ResourceNotFoundException("Service replacement not found: " + replacementId));
        if (ErpDocumentStatuses.ISSUED.equals(replacement.getStatus())) {
            return replacement;
        }
        final Long originalOwnershipId = replacement.getOriginalProductOwnershipId();
        final Long originalSerialId = replacement.getOriginalSerialNumberId();
        final Long replacementSerialId = replacement.getReplacementSerialNumberId();
        final Long salesReturnId = replacement.getSalesReturnId();
        final Long serviceTicketId = replacement.getServiceTicketId();
        final Long warrantyClaimId = replacement.getWarrantyClaimId();
        final Long originalProductId = replacement.getOriginalProductId();
        final Long replacementProductId = replacement.getReplacementProductId();

        ProductOwnership originalOwnership = originalOwnershipId == null ? null
                : productOwnershipRepository.findById(originalOwnershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Product ownership not found: " + originalOwnershipId));
        SerialNumber originalSerial = originalSerialId == null ? null
                : serialNumberRepository.findById(originalSerialId)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + originalSerialId));
        SerialNumber replacementSerial = replacementSerialId == null ? null
                : serialNumberRepository.findById(replacementSerialId)
                .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + replacementSerialId));
        SalesReturn salesReturn = salesReturnId == null ? null
                : salesReturnRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), salesReturnId)
                .orElseThrow(() -> new ResourceNotFoundException("Sales return not found: " + salesReturnId));
        ServiceTicket ticket = serviceTicketId == null ? null
                : serviceTicketRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), serviceTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Service ticket not found: " + serviceTicketId));
        WarrantyClaim claim = warrantyClaimId == null ? null
                : warrantyClaimRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), warrantyClaimId)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty claim not found: " + warrantyClaimId));

        BigDecimal replacementCost = accountingPostingService.estimateSalesCost(
                replacement.getOrganizationId(),
                replacement.getWarehouseId(),
                replacement.getReplacementProductId(),
                replacement.getReplacementBaseQuantity()
        );

        inventoryPostingService.postMovement(
                replacement.getOrganizationId(),
                replacement.getBranchId(),
                replacement.getWarehouseId(),
                replacement.getReplacementProductId(),
                replacementSerial == null ? null : replacementSerial.getBatchId(),
                replacement.getReplacementUomId(),
                replacement.getReplacementQuantity(),
                replacement.getReplacementBaseQuantity(),
                "OUT",
                ErpInventoryMovementTypes.SERVICE_REPLACEMENT,
                "service_replacement",
                replacement.getId(),
                replacement.getReplacementNumber(),
                null,
                ErpJsonPayloads.of(
                        "replacementType", replacement.getReplacementType(),
                        "stockSourceBucket", replacement.getStockSourceBucket(),
                        "customerId", replacement.getCustomerId()
                )
        );
        accountingPostingService.postServiceReplacement(replacement, replacementCost, salesReturn);

        if (originalOwnership != null && !ErpDocumentStatuses.REPLACED.equals(originalOwnership.getStatus())) {
            originalOwnership.setStatus(ErpDocumentStatuses.REPLACED);
            productOwnershipRepository.save(originalOwnership);
        }
        if (originalSerial != null && !ErpDocumentStatuses.REPLACED.equals(originalSerial.getStatus())) {
            originalSerial.setStatus(ErpDocumentStatuses.REPLACED);
            originalSerial.setCurrentCustomerId(null);
            serialNumberRepository.save(originalSerial);
        }
        if (replacementSerial != null && !ErpDocumentStatuses.SOLD.equals(replacementSerial.getStatus())) {
            replacementSerial.setStatus(ErpDocumentStatuses.SOLD);
            replacementSerial.setCurrentWarehouseId(null);
            replacementSerial.setCurrentCustomerId(replacement.getCustomerId());
            replacementSerial.setWarrantyStartDate(replacement.getWarrantyStartDate());
            replacementSerial.setWarrantyEndDate(replacement.getWarrantyEndDate());
            serialNumberRepository.save(replacementSerial);
        }

        SalesInvoice invoice = resolveReplacementSalesInvoice(replacement, originalOwnership, claim, ticket, salesReturn);
        if (invoice != null) {
            Long salesInvoiceLineId = originalOwnership == null ? null : originalOwnership.getSalesInvoiceLineId();
            if (salesInvoiceLineId == null) {
                List<SalesInvoiceLine> invoiceLines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
                salesInvoiceLineId = invoiceLines.stream()
                        .filter(line -> originalProductId.equals(line.getProductId()) || replacementProductId.equals(line.getProductId()))
                        .map(SalesInvoiceLine::getId)
                        .findFirst()
                        .orElse(null);
            }
            if (salesInvoiceLineId != null && productOwnershipRepository.findBySalesInvoiceLineId(salesInvoiceLineId).stream()
                    .noneMatch(o -> replacementSerialId != null && replacementSerialId.equals(o.getSerialNumberId()))) {
                ProductOwnership replacementOwnership = new ProductOwnership();
                replacementOwnership.setOrganizationId(replacement.getOrganizationId());
                replacementOwnership.setCustomerId(replacement.getCustomerId());
                replacementOwnership.setProductId(replacement.getReplacementProductId());
                replacementOwnership.setSerialNumberId(replacement.getReplacementSerialNumberId());
                replacementOwnership.setSalesInvoiceId(invoice.getId());
                replacementOwnership.setSalesInvoiceLineId(salesInvoiceLineId);
                replacementOwnership.setOwnershipStartDate(replacement.getIssuedOn());
                replacementOwnership.setWarrantyStartDate(replacement.getWarrantyStartDate());
                replacementOwnership.setWarrantyEndDate(replacement.getWarrantyEndDate());
                replacementOwnership.setStatus(ErpDocumentStatuses.ACTIVE);
                productOwnershipRepository.save(replacementOwnership);
            }
        }

        if (ticket != null) {
            ticket.setStatus(ErpDocumentStatuses.RESOLVED);
            serviceTicketRepository.save(ticket);
            List<ServiceTicketItem> ticketItems = serviceTicketItemRepository.findByServiceTicketIdOrderByIdAsc(ticket.getId());
            for (ServiceTicketItem item : ticketItems) {
                boolean ownershipMatch = originalOwnership != null && item.getProductOwnershipId() != null
                        && item.getProductOwnershipId().equals(originalOwnership.getId());
                boolean serialMatch = originalSerial != null && item.getSerialNumberId() != null
                        && item.getSerialNumberId().equals(originalSerial.getId());
                boolean productMatch = item.getProductId() != null && item.getProductId().equals(replacement.getOriginalProductId());
                if (ownershipMatch || serialMatch || productMatch) {
                    item.setResolutionStatus("REPLACED");
                    serviceTicketItemRepository.save(item);
                }
            }
        }
        if (claim != null) {
            claim.setStatus("SETTLED");
            if (claim.getApprovedOn() == null) {
                claim.setApprovedOn(replacement.getIssuedOn());
            }
            warrantyClaimRepository.save(claim);
        }

        replacement.setStatus(ErpDocumentStatuses.ISSUED);
        replacement = serviceReplacementRepository.save(replacement);

        auditEventWriter.write(
                replacement.getOrganizationId(),
                replacement.getBranchId(),
                "SERVICE_REPLACEMENT_ISSUED",
                "service_replacement",
                replacement.getId(),
                replacement.getReplacementNumber(),
                "ISSUE",
                replacement.getWarehouseId(),
                replacement.getCustomerId(),
                claim == null ? null : claim.getSupplierId(),
                "Service replacement issued",
                ErpJsonPayloads.of(
                        "replacementType", replacement.getReplacementType(),
                        "stockSourceBucket", replacement.getStockSourceBucket(),
                        "originalProductId", replacement.getOriginalProductId(),
                        "replacementProductId", replacement.getReplacementProductId(),
                        "warrantyClaimId", replacement.getWarrantyClaimId(),
                        "salesReturnId", replacement.getSalesReturnId()
                )
        );

        return replacement;
    }

    private SalesInvoice resolveReplacementSalesInvoice(ServiceReplacement replacement,
                                                        ProductOwnership originalOwnership,
                                                        WarrantyClaim claim,
                                                        ServiceTicket ticket,
                                                        SalesReturn salesReturn) {
        if (originalOwnership != null) {
            return salesInvoiceRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), originalOwnership.getSalesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + originalOwnership.getSalesInvoiceId()));
        }
        if (claim != null && claim.getSalesInvoiceId() != null) {
            return salesInvoiceRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), claim.getSalesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + claim.getSalesInvoiceId()));
        }
        if (ticket != null && ticket.getSalesInvoiceId() != null) {
            return salesInvoiceRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), ticket.getSalesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + ticket.getSalesInvoiceId()));
        }
        if (salesReturn != null) {
            return salesInvoiceRepository.findByOrganizationIdAndId(replacement.getOrganizationId(), salesReturn.getOriginalSalesInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sales invoice not found: " + salesReturn.getOriginalSalesInvoiceId()));
        }
        return null;
    }
}
