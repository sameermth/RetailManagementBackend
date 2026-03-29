package com.retailmanagement.modules.erp.finance.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.common.util.RecurringScheduleSupport;
import com.retailmanagement.modules.erp.finance.dto.ErpFinanceDtos;
import com.retailmanagement.modules.erp.finance.dto.RecurringFinanceDtos;
import com.retailmanagement.modules.erp.finance.dto.RecurringFinanceResponses;
import com.retailmanagement.modules.erp.finance.entity.RecurringJournal;
import com.retailmanagement.modules.erp.finance.entity.RecurringJournalLine;
import com.retailmanagement.modules.erp.finance.entity.Voucher;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.repository.RecurringJournalLineRepository;
import com.retailmanagement.modules.erp.finance.repository.RecurringJournalRepository;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RecurringJournalService {

    private final RecurringJournalRepository recurringJournalRepository;
    private final RecurringJournalLineRepository recurringJournalLineRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final ErpAccessGuard accessGuard;
    private final ErpFinanceService erpFinanceService;

    @Transactional(readOnly = true)
    public List<RecurringJournal> list(Long organizationId) {
        accessGuard.assertOrganizationAccess(organizationId);
        return recurringJournalRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public RecurringFinanceResponses.RecurringJournalResponse get(Long organizationId, Long id) {
        accessGuard.assertOrganizationAccess(organizationId);
        RecurringJournal journal = recurringJournalRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring journal not found: " + id));
        return toResponse(journal, recurringJournalLineRepository.findByRecurringJournalIdOrderByIdAsc(id));
    }

    public RecurringFinanceResponses.RecurringJournalResponse create(Long organizationId, Long branchId,
                                                                     RecurringFinanceDtos.CreateRecurringJournalRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        String frequency = RecurringScheduleSupport.normalizeFrequency(request.frequency());
        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        LocalDate nextRunDate = request.nextRunDate() == null ? startDate : request.nextRunDate();
        if (request.endDate() != null && request.endDate().isBefore(nextRunDate)) {
            throw new BusinessException("End date cannot be before next run date");
        }

        RecurringJournal journal = new RecurringJournal();
        journal.setOrganizationId(organizationId);
        journal.setBranchId(branchId);
        journal.setTemplateNumber(generateTemplateNumber());
        journal.setVoucherType(request.voucherType().trim().toUpperCase());
        journal.setFrequency(frequency);
        journal.setStartDate(startDate);
        journal.setNextRunDate(nextRunDate);
        journal.setEndDate(request.endDate());
        journal.setRemarks(request.remarks());
        journal.setIsActive(request.isActive() == null || request.isActive());
        journal = recurringJournalRepository.save(journal);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<RecurringJournalLine> lines = new ArrayList<>();
        for (RecurringFinanceDtos.CreateRecurringJournalLineRequest requestLine : request.lines()) {
            accountRepository.findByIdAndOrganizationId(requestLine.accountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + requestLine.accountId()));
            if (requestLine.customerId() != null) {
                customerRepository.findByIdAndOrganizationId(requestLine.customerId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + requestLine.customerId()));
            }
            if (requestLine.supplierId() != null) {
                supplierRepository.findByIdAndOrganizationId(requestLine.supplierId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + requestLine.supplierId()));
            }
            BigDecimal debit = safe(requestLine.debitAmount());
            BigDecimal credit = safe(requestLine.creditAmount());
            if ((debit.compareTo(BigDecimal.ZERO) > 0) == (credit.compareTo(BigDecimal.ZERO) > 0)) {
                throw new BusinessException("Recurring journal line must have either debit or credit");
            }
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            RecurringJournalLine line = new RecurringJournalLine();
            line.setRecurringJournalId(journal.getId());
            line.setAccountId(requestLine.accountId());
            line.setDebitAmount(debit);
            line.setCreditAmount(credit);
            line.setNarrative(requestLine.narrative());
            line.setCustomerId(requestLine.customerId());
            line.setSupplierId(requestLine.supplierId());
            lines.add(recurringJournalLineRepository.save(line));
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("Recurring journal template is not balanced");
        }
        return toResponse(journal, lines);
    }

    public ErpFinanceDtos.VoucherResponse run(Long organizationId, Long id, LocalDate requestedRunDate) {
        accessGuard.assertOrganizationAccess(organizationId);
        RecurringJournal journal = recurringJournalRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring journal not found: " + id));
        Voucher voucher = generateVoucher(journal, requestedRunDate);
        return new ErpFinanceDtos.VoucherResponse(voucher.getId(), voucher.getOrganizationId(), voucher.getBranchId(),
                voucher.getVoucherNumber(), voucher.getVoucherDate(), voucher.getVoucherType(), voucher.getReferenceType(),
                voucher.getReferenceId(), voucher.getRemarks(), voucher.getStatus(), voucher.getCreatedAt(), voucher.getUpdatedAt());
    }

    @Scheduled(fixedDelayString = "${erp.recurring.journal.scan-ms:3600000}")
    public void runDueTemplates() {
        LocalDate today = LocalDate.now();
        for (RecurringJournal journal : recurringJournalRepository
                .findByIsActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAscIdAsc(today)) {
            if (journal.getEndDate() != null && journal.getNextRunDate().isAfter(journal.getEndDate())) {
                journal.setIsActive(false);
                recurringJournalRepository.save(journal);
                continue;
            }
            try {
                generateVoucher(journal, journal.getNextRunDate());
            } catch (RuntimeException ignored) {
                // Keep template active for manual rerun.
            }
        }
    }

    private Voucher generateVoucher(RecurringJournal journal, LocalDate requestedRunDate) {
        LocalDate effectiveRunDate = requestedRunDate == null ? journal.getNextRunDate() : requestedRunDate;
        if (journal.getEndDate() != null && effectiveRunDate.isAfter(journal.getEndDate())) {
            throw new BusinessException("Recurring journal is past its end date");
        }
        List<RecurringJournalLine> lines = recurringJournalLineRepository.findByRecurringJournalIdOrderByIdAsc(journal.getId());
        if (lines.isEmpty()) {
            throw new BusinessException("Recurring journal has no lines");
        }
        List<ErpFinanceDtos.CreateVoucherLineRequest> voucherLines = lines.stream()
                .map(line -> new ErpFinanceDtos.CreateVoucherLineRequest(
                        line.getAccountId(),
                        line.getDebitAmount(),
                        line.getCreditAmount(),
                        line.getNarrative(),
                        line.getCustomerId(),
                        line.getSupplierId(),
                        null,
                        null
                )).toList();
        Voucher voucher = erpFinanceService.createVoucher(new ErpFinanceDtos.CreateVoucherRequest(
                journal.getOrganizationId(),
                journal.getBranchId(),
                effectiveRunDate,
                journal.getVoucherType(),
                "recurring_journal",
                journal.getId(),
                journal.getRemarks(),
                voucherLines
        ));
        journal.setLastRunAt(LocalDateTime.now());
        journal.setLastVoucherId(voucher.getId());
        LocalDate nextRun = RecurringScheduleSupport.nextRunDate(journal.getFrequency(), effectiveRunDate);
        journal.setNextRunDate(nextRun);
        if (journal.getEndDate() != null && nextRun.isAfter(journal.getEndDate())) {
            journal.setIsActive(false);
        }
        recurringJournalRepository.save(journal);
        return voucher;
    }

    private RecurringFinanceResponses.RecurringJournalResponse toResponse(RecurringJournal journal, List<RecurringJournalLine> lines) {
        return new RecurringFinanceResponses.RecurringJournalResponse(
                journal.getId(),
                journal.getOrganizationId(),
                journal.getBranchId(),
                journal.getTemplateNumber(),
                journal.getVoucherType(),
                journal.getFrequency(),
                journal.getStartDate(),
                journal.getNextRunDate(),
                journal.getEndDate(),
                journal.getRemarks(),
                journal.getIsActive(),
                journal.getLastRunAt(),
                journal.getLastVoucherId(),
                lines.stream().map(line -> new RecurringFinanceResponses.RecurringJournalLineResponse(
                        line.getId(),
                        line.getAccountId(),
                        line.getDebitAmount(),
                        line.getCreditAmount(),
                        line.getNarrative(),
                        line.getCustomerId(),
                        line.getSupplierId()
                )).toList()
        );
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateTemplateNumber() {
        return "RJ-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + System.currentTimeMillis();
    }
}
