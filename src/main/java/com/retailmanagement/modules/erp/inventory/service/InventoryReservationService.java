package com.retailmanagement.modules.erp.inventory.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.constants.ErpDocumentStatuses;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBatch;
import com.retailmanagement.modules.erp.inventory.entity.InventoryReservation;
import com.retailmanagement.modules.erp.inventory.entity.SerialNumber;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBatchRepository;
import com.retailmanagement.modules.erp.inventory.repository.InventoryReservationRepository;
import com.retailmanagement.modules.erp.inventory.repository.SerialNumberRepository;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoiceLine;
import com.retailmanagement.modules.erp.sales.entity.SalesLineBatch;
import com.retailmanagement.modules.erp.sales.entity.SalesLineSerial;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceLineRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineBatchRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesLineSerialRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InventoryReservationService {

    private static final String SOURCE_TYPE_SALES_INVOICE = "sales_invoice";
    private static final long DEFAULT_RESERVATION_TTL_HOURS = 2L;

    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final SerialNumberRepository serialNumberRepository;
    private final SalesInvoiceLineRepository salesInvoiceLineRepository;
    private final SalesLineSerialRepository salesLineSerialRepository;
    private final SalesLineBatchRepository salesLineBatchRepository;

    @Transactional(readOnly = true)
    public List<InventoryReservation> listReservations(Long organizationId, String status) {
        if (status == null || status.isBlank()) {
            return inventoryReservationRepository.findByOrganizationIdOrderByIdDesc(organizationId);
        }
        return inventoryReservationRepository.findByOrganizationIdAndStatusOrderByIdDesc(organizationId, status.toUpperCase());
    }

    public int expireReservations(LocalDateTime asOfTime) {
        LocalDateTime effectiveTime = asOfTime == null ? LocalDateTime.now() : asOfTime;
        int expired = 0;
        for (InventoryReservation reservation : inventoryReservationRepository
                .findByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAscIdAsc("ACTIVE", effectiveTime)) {
            releaseReservation(reservation, "EXPIRED", effectiveTime);
            expired++;
        }
        return expired;
    }

    public void releaseReservation(Long organizationId, Long reservationId, String releaseReason) {
        InventoryReservation reservation = inventoryReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory reservation not found: " + reservationId));
        if (!organizationId.equals(reservation.getOrganizationId())) {
            throw new BusinessException("Inventory reservation does not belong to organization " + organizationId);
        }
        if (!"ACTIVE".equalsIgnoreCase(reservation.getStatus())) {
            return;
        }
        releaseReservation(reservation, releaseReason == null || releaseReason.isBlank() ? "MANUAL_RELEASE" : releaseReason, LocalDateTime.now());
    }

    @Scheduled(fixedDelayString = "${erp.inventory.reservation.expiry-scan-ms:300000}")
    public void expireReservationsScheduled() {
        try {
            int expired = expireReservations(LocalDateTime.now());
            if (expired > 0) {
                log.info("Expired {} inventory reservations", expired);
            }
        } catch (Exception ex) {
            log.warn("Inventory reservation expiry scan failed: {}", ex.getMessage());
        }
    }

    public void reserveSalesInvoice(SalesInvoice invoice) {
        List<SalesInvoiceLine> lines = salesInvoiceLineRepository.findBySalesInvoiceIdOrderByIdAsc(invoice.getId());
        for (SalesInvoiceLine line : lines) {
            List<SalesLineSerial> serialLinks = salesLineSerialRepository.findBySalesInvoiceLineId(line.getId());
            if (!serialLinks.isEmpty()) {
                reserveSerialLine(invoice, line, serialLinks);
                continue;
            }

            List<SalesLineBatch> batchLinks = salesLineBatchRepository.findBySalesInvoiceLineId(line.getId());
            if (!batchLinks.isEmpty()) {
                reserveBatchLine(invoice, line, batchLinks);
                continue;
            }

            reserveQuantity(invoice.getOrganizationId(), invoice.getBranchId(), invoice.getWarehouseId(), line.getProductId(), null, line.getBaseQuantity());
            saveReservation(invoice, line, null, null, line.getBaseQuantity());
        }
    }

    public void consumeSalesInvoiceReservations(SalesInvoice invoice) {
        List<InventoryReservation> reservations = inventoryReservationRepository
                .findByOrganizationIdAndSourceDocumentTypeAndSourceDocumentIdAndStatusOrderByIdAsc(
                        invoice.getOrganizationId(),
                        SOURCE_TYPE_SALES_INVOICE,
                        invoice.getId(),
                        "ACTIVE"
                );
        for (InventoryReservation reservation : reservations) {
            boolean inventoryAdjusted = releaseReservedQuantity(reservation);
            if (reservation.getSerialNumberId() != null) {
                SerialNumber serial = serialNumberRepository.findById(reservation.getSerialNumberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + reservation.getSerialNumberId()));
                if (!ErpDocumentStatuses.ALLOCATED.equals(serial.getStatus()) && !ErpDocumentStatuses.SOLD.equals(serial.getStatus())) {
                    throw new BusinessException("Reserved serial " + serial.getSerialNumber() + " is no longer allocated");
                }
                SalesInvoiceLine line = salesInvoiceLineRepository.findById(reservation.getSourceDocumentLineId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sales invoice line not found: " + reservation.getSourceDocumentLineId()));
                serial.setStatus(ErpDocumentStatuses.SOLD);
                serial.setCurrentWarehouseId(null);
                serial.setCurrentCustomerId(invoice.getCustomerId());
                if (serial.getWarrantyStartDate() == null) {
                    serial.setWarrantyStartDate(invoice.getInvoiceDate());
                }
                Integer warrantyMonths = line.getWarrantyMonths();
                if (warrantyMonths != null && warrantyMonths > 0) {
                    serial.setWarrantyEndDate(invoice.getInvoiceDate().plusMonths(warrantyMonths));
                }
                serialNumberRepository.save(serial);
            }
            reservation.setStatus("CONSUMED");
            reservation.setReleasedAt(LocalDateTime.now());
            reservation.setReleaseReason(inventoryAdjusted ? "CONSUMED" : "CONSUMED_STALE_BALANCE");
            inventoryReservationRepository.save(reservation);
        }
    }

    public void releaseSalesInvoiceReservations(Long organizationId, Long salesInvoiceId) {
        List<InventoryReservation> reservations = inventoryReservationRepository
                .findByOrganizationIdAndSourceDocumentTypeAndSourceDocumentIdAndStatusOrderByIdAsc(
                        organizationId,
                        SOURCE_TYPE_SALES_INVOICE,
                        salesInvoiceId,
                        "ACTIVE"
                );
        for (InventoryReservation reservation : reservations) {
            releaseReservation(reservation, "SOURCE_RELEASED", LocalDateTime.now());
        }
    }

    private void reserveSerialLine(SalesInvoice invoice, SalesInvoiceLine line, List<SalesLineSerial> serialLinks) {
        for (SalesLineSerial serialLink : serialLinks) {
            SerialNumber serial = serialNumberRepository.findById(serialLink.getSerialNumberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Serial number not found: " + serialLink.getSerialNumberId()));
            if (!invoice.getOrganizationId().equals(serial.getOrganizationId())) {
                throw new BusinessException("Serial " + serial.getId() + " does not belong to organization " + invoice.getOrganizationId());
            }
            if (!line.getProductId().equals(serial.getProductId())) {
                throw new BusinessException("Serial " + serial.getSerialNumber() + " does not belong to product " + line.getProductId());
            }
            if (!ErpDocumentStatuses.IN_STOCK.equals(serial.getStatus())) {
                throw new BusinessException("Serial " + serial.getSerialNumber() + " is not available for reservation");
            }
            if (serial.getCurrentWarehouseId() == null || !invoice.getWarehouseId().equals(serial.getCurrentWarehouseId())) {
                throw new BusinessException("Serial " + serial.getSerialNumber() + " is not in warehouse " + invoice.getWarehouseId());
            }
            reserveQuantity(invoice.getOrganizationId(), invoice.getBranchId(), invoice.getWarehouseId(), line.getProductId(), null, BigDecimal.ONE);
            serial.setStatus(ErpDocumentStatuses.ALLOCATED);
            serialNumberRepository.save(serial);
            saveReservation(invoice, line, null, serial.getId(), BigDecimal.ONE);
        }
    }

    private void reserveBatchLine(SalesInvoice invoice, SalesInvoiceLine line, List<SalesLineBatch> batchLinks) {
        for (SalesLineBatch batchLink : batchLinks) {
            InventoryBatch batch = inventoryBatchRepository.findById(batchLink.getBatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchLink.getBatchId()));
            if (!invoice.getOrganizationId().equals(batch.getOrganizationId())) {
                throw new BusinessException("Batch " + batch.getId() + " does not belong to organization " + invoice.getOrganizationId());
            }
            if (!line.getProductId().equals(batch.getProductId())) {
                throw new BusinessException("Batch " + batch.getId() + " does not belong to product " + line.getProductId());
            }
            reserveQuantity(invoice.getOrganizationId(), invoice.getBranchId(), invoice.getWarehouseId(), line.getProductId(), batch.getId(), batchLink.getBaseQuantity());
            saveReservation(invoice, line, batch.getId(), null, batchLink.getBaseQuantity());
        }
    }

    private void reserveQuantity(Long organizationId, Long branchId, Long warehouseId, Long productId, Long batchId, BigDecimal baseQuantity) {
        InventoryBalance balance = inventoryBalanceRepository
                .findByOrganizationIdAndBranchIdAndWarehouseIdAndProductIdAndBatchId(
                        organizationId,
                        branchId,
                        warehouseId,
                        productId,
                        batchId
                )
                .orElseThrow(() -> new BusinessException("Inventory balance not found for product " + productId + " in warehouse " + warehouseId));

        BigDecimal available = defaultQuantity(balance.getAvailableBaseQuantity());
        if (available.compareTo(baseQuantity) < 0) {
            throw new BusinessException("Insufficient available stock for product " + productId + " in warehouse " + warehouseId);
        }

        balance.setReservedBaseQuantity(defaultQuantity(balance.getReservedBaseQuantity()).add(baseQuantity));
        balance.setAvailableBaseQuantity(available.subtract(baseQuantity));
        inventoryBalanceRepository.save(balance);
    }

    private boolean releaseReservedQuantity(InventoryReservation reservation) {
        InventoryBalance balance = inventoryBalanceRepository
                .findByOrganizationIdAndBranchIdAndWarehouseIdAndProductIdAndBatchId(
                        reservation.getOrganizationId(),
                        reservation.getBranchId(),
                        reservation.getWarehouseId(),
                        reservation.getProductId(),
                        reservation.getBatchId()
                )
                .orElse(null);

        if (balance == null) {
            log.warn("Skipping inventory release for reservation {} because no inventory balance was found", reservation.getId());
            return false;
        }

        BigDecimal reserved = defaultQuantity(balance.getReservedBaseQuantity());
        if (reserved.compareTo(reservation.getReservedBaseQuantity()) < 0) {
            log.warn(
                    "Skipping inventory release for reservation {} because reserved quantity {} is less than reserved_base_quantity {}",
                    reservation.getId(),
                    reserved,
                    reservation.getReservedBaseQuantity()
            );
            return false;
        }
        balance.setReservedBaseQuantity(reserved.subtract(reservation.getReservedBaseQuantity()));
        balance.setAvailableBaseQuantity(defaultQuantity(balance.getAvailableBaseQuantity()).add(reservation.getReservedBaseQuantity()));
        inventoryBalanceRepository.save(balance);
        return true;
    }

    private void saveReservation(SalesInvoice invoice, SalesInvoiceLine line, Long batchId, Long serialNumberId, BigDecimal reservedBaseQuantity) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setOrganizationId(invoice.getOrganizationId());
        reservation.setBranchId(invoice.getBranchId());
        reservation.setWarehouseId(invoice.getWarehouseId());
        reservation.setProductId(line.getProductId());
        reservation.setBatchId(batchId);
        reservation.setSerialNumberId(serialNumberId);
        reservation.setSourceDocumentType(SOURCE_TYPE_SALES_INVOICE);
        reservation.setSourceDocumentId(invoice.getId());
        reservation.setSourceDocumentLineId(line.getId());
        reservation.setReservedBaseQuantity(reservedBaseQuantity);
        reservation.setExpiresAt(LocalDateTime.now().plusHours(DEFAULT_RESERVATION_TTL_HOURS));
        reservation.setStatus("ACTIVE");
        inventoryReservationRepository.save(reservation);
    }

    private void releaseReservation(InventoryReservation reservation, String releaseReason, LocalDateTime releasedAt) {
        boolean inventoryAdjusted = releaseReservedQuantity(reservation);
        if (reservation.getSerialNumberId() != null) {
            serialNumberRepository.findById(reservation.getSerialNumberId()).ifPresent(serial -> {
                if (ErpDocumentStatuses.ALLOCATED.equals(serial.getStatus())) {
                    serial.setStatus(ErpDocumentStatuses.IN_STOCK);
                    serialNumberRepository.save(serial);
                }
            });
        }
        reservation.setStatus("RELEASED");
        reservation.setReleasedAt(releasedAt);
        reservation.setReleaseReason(inventoryAdjusted ? releaseReason : releaseReason + "_STALE_BALANCE");
        inventoryReservationRepository.save(reservation);
    }

    private BigDecimal defaultQuantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
