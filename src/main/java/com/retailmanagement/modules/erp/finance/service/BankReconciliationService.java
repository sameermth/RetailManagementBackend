package com.retailmanagement.modules.erp.finance.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import com.retailmanagement.modules.erp.finance.dto.BankReconciliationDtos;
import com.retailmanagement.modules.erp.finance.dto.BankReconciliationResponses;
import com.retailmanagement.modules.erp.finance.entity.Account;
import com.retailmanagement.modules.erp.finance.entity.BankStatementEntry;
import com.retailmanagement.modules.erp.finance.entity.LedgerEntry;
import com.retailmanagement.modules.erp.finance.entity.Voucher;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.repository.BankStatementEntryRepository;
import com.retailmanagement.modules.erp.finance.repository.LedgerEntryRepository;
import com.retailmanagement.modules.erp.finance.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BankReconciliationService {

    private final BankStatementEntryRepository bankStatementEntryRepository;
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final VoucherRepository voucherRepository;
    private final ErpAccessGuard accessGuard;

    public List<BankStatementEntry> importStatement(Long organizationId, Long branchId, BankReconciliationDtos.ImportBankStatementRequest request) {
        accessGuard.assertBranchAccess(organizationId, branchId);
        Account account = requireBankAccount(organizationId, request.accountId());
        List<BankStatementEntry> saved = new ArrayList<>();
        for (BankReconciliationDtos.BankStatementLineRequest line : request.lines()) {
            BigDecimal debit = safe(line.debitAmount());
            BigDecimal credit = safe(line.creditAmount());
            if ((debit.compareTo(BigDecimal.ZERO) > 0) == (credit.compareTo(BigDecimal.ZERO) > 0)) {
                throw new BusinessException("Bank statement line must have either debit or credit amount");
            }
            BankStatementEntry entry = new BankStatementEntry();
            entry.setOrganizationId(organizationId);
            entry.setBranchId(branchId);
            entry.setAccountId(account.getId());
            entry.setEntryDate(line.entryDate());
            entry.setValueDate(line.valueDate());
            entry.setReferenceNumber(trimToNull(line.referenceNumber()));
            entry.setDescription(trimToNull(line.description()));
            entry.setDebitAmount(debit);
            entry.setCreditAmount(credit);
            entry.setRemarks(trimToNull(line.remarks()));
            saved.add(bankStatementEntryRepository.save(entry));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public BankReconciliationResponses.BankReconciliationSummaryResponse summary(Long organizationId,
                                                                                 BankReconciliationDtos.BankReconciliationQuery query) {
        accessGuard.assertOrganizationAccess(organizationId);
        requireBankAccount(organizationId, query.accountId());
        if (query.fromDate().isAfter(query.toDate())) {
            throw new BusinessException("fromDate cannot be after toDate");
        }
        List<BankStatementEntry> entries = bankStatementEntryRepository
                .findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                        organizationId, query.accountId(), query.fromDate(), query.toDate());
        List<LedgerEntry> ledgerEntries = ledgerEntryRepository
                .findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                        organizationId, query.accountId(), query.fromDate(), query.toDate());

        BigDecimal statementBalance = entries.stream().map(this::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reconciledStatementBalance = entries.stream()
                .filter(this::isReconciled)
                .map(this::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bookBalance = ledgerEntries.stream().map(this::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reconciledBookBalance = entries.stream()
                .filter(this::isReconciled)
                .map(BankStatementEntry::getMatchedLedgerEntryId)
                .distinct()
                .map(id -> ledgerEntries.stream().filter(entry -> entry.getId().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(this::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BankReconciliationResponses.BankReconciliationSummaryResponse(
                query.accountId(),
                query.fromDate(),
                query.toDate(),
                statementBalance,
                reconciledStatementBalance,
                bookBalance,
                reconciledBookBalance,
                entries.stream().filter(entry -> !isReconciled(entry)).count(),
                entries.stream().filter(this::isReconciled).count(),
                entries.stream().map(this::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<BankReconciliationResponses.BankReconciliationCandidateResponse> candidates(Long organizationId, Long statementEntryId) {
        accessGuard.assertOrganizationAccess(organizationId);
        BankStatementEntry entry = bankStatementEntryRepository.findByIdAndOrganizationId(statementEntryId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement entry not found: " + statementEntryId));
        LocalDate fromDate = entry.getEntryDate().minusDays(7);
        LocalDate toDate = entry.getEntryDate().plusDays(7);
        BigDecimal target = signedAmount(entry);
        List<LedgerEntry> candidates = ledgerEntryRepository
                .findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                        organizationId, entry.getAccountId(), fromDate, toDate)
                .stream()
                .filter(ledgerEntry -> !bankStatementEntryRepository.existsByMatchedLedgerEntryId(ledgerEntry.getId())
                        || ledgerEntry.getId().equals(entry.getMatchedLedgerEntryId()))
                .toList();
        return candidates.stream()
                .map(ledgerEntry -> {
                    Voucher voucher = voucherRepository.findByIdAndOrganizationId(ledgerEntry.getVoucherId(), organizationId).orElse(null);
                    BigDecimal signedAmount = signedAmount(ledgerEntry);
                    return new BankReconciliationResponses.BankReconciliationCandidateResponse(
                            ledgerEntry.getId(),
                            ledgerEntry.getVoucherId(),
                            voucher == null ? null : voucher.getVoucherNumber(),
                            voucher == null ? null : voucher.getVoucherType(),
                            ledgerEntry.getEntryDate(),
                            ledgerEntry.getDebitAmount(),
                            ledgerEntry.getCreditAmount(),
                            signedAmount,
                            ledgerEntry.getNarrative(),
                            signedAmount.compareTo(target) == 0,
                            Math.abs(ChronoUnit.DAYS.between(entry.getEntryDate(), ledgerEntry.getEntryDate()))
                    );
                })
                .sorted(Comparator
                        .comparing(BankReconciliationResponses.BankReconciliationCandidateResponse::exactAmountMatch).reversed()
                        .thenComparing(BankReconciliationResponses.BankReconciliationCandidateResponse::dayDifference)
                        .thenComparing(BankReconciliationResponses.BankReconciliationCandidateResponse::ledgerEntryId))
                .toList();
    }

    public BankStatementEntry reconcile(Long organizationId, Long statementEntryId, BankReconciliationDtos.ReconcileBankStatementRequest request) {
        accessGuard.assertOrganizationAccess(organizationId);
        BankStatementEntry entry = bankStatementEntryRepository.findByIdAndOrganizationId(statementEntryId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement entry not found: " + statementEntryId));
        LedgerEntry ledgerEntry = ledgerEntryRepository.findById(request.ledgerEntryId())
                .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found: " + request.ledgerEntryId()));
        if (!organizationId.equals(ledgerEntry.getOrganizationId())) {
            throw new BusinessException("Ledger entry does not belong to organization " + organizationId);
        }
        if (!entry.getAccountId().equals(ledgerEntry.getAccountId())) {
            throw new BusinessException("Ledger entry account does not match bank statement account");
        }
        if (bankStatementEntryRepository.existsByMatchedLedgerEntryId(ledgerEntry.getId())
                && !ledgerEntry.getId().equals(entry.getMatchedLedgerEntryId())) {
            throw new BusinessException("Ledger entry is already reconciled to another bank statement entry");
        }
        if (signedAmount(entry).compareTo(signedAmount(ledgerEntry)) != 0) {
            throw new BusinessException("Ledger entry amount does not match the bank statement amount");
        }
        entry.setMatchedLedgerEntryId(ledgerEntry.getId());
        entry.setMatchedOn(LocalDateTime.now());
        entry.setMatchedBy(ErpSecurityUtils.currentUserId().orElse(1L));
        entry.setRemarks(trimToNull(request.remarks()) != null ? request.remarks().trim() : entry.getRemarks());
        entry.setStatus("RECONCILED");
        return bankStatementEntryRepository.save(entry);
    }

    public BankStatementEntry unreconcile(Long organizationId, Long statementEntryId) {
        accessGuard.assertOrganizationAccess(organizationId);
        BankStatementEntry entry = bankStatementEntryRepository.findByIdAndOrganizationId(statementEntryId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank statement entry not found: " + statementEntryId));
        entry.setMatchedLedgerEntryId(null);
        entry.setMatchedOn(null);
        entry.setMatchedBy(null);
        entry.setStatus("UNMATCHED");
        return bankStatementEntryRepository.save(entry);
    }

    private Account requireBankAccount(Long organizationId, Long accountId) {
        Account account = accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        if (!"ASSET".equalsIgnoreCase(account.getAccountType())) {
            throw new BusinessException("Bank reconciliation requires an asset account");
        }
        if (!"BANK".equalsIgnoreCase(account.getCode()) && !account.getName().toUpperCase().contains("BANK")) {
            throw new BusinessException("Account is not recognized as a bank account");
        }
        return account;
    }

    private BankReconciliationResponses.BankStatementEntryResponse toResponse(BankStatementEntry entry) {
        return new BankReconciliationResponses.BankStatementEntryResponse(
                entry.getId(),
                entry.getOrganizationId(),
                entry.getBranchId(),
                entry.getAccountId(),
                entry.getEntryDate(),
                entry.getValueDate(),
                entry.getReferenceNumber(),
                entry.getDescription(),
                entry.getDebitAmount(),
                entry.getCreditAmount(),
                signedAmount(entry),
                entry.getStatus(),
                entry.getMatchedLedgerEntryId(),
                entry.getMatchedOn(),
                entry.getMatchedBy(),
                entry.getRemarks()
        );
    }

    private boolean isReconciled(BankStatementEntry entry) {
        return "RECONCILED".equalsIgnoreCase(entry.getStatus()) && entry.getMatchedLedgerEntryId() != null;
    }

    private BigDecimal signedAmount(BankStatementEntry entry) {
        return safe(entry.getDebitAmount()).subtract(safe(entry.getCreditAmount()));
    }

    private BigDecimal signedAmount(LedgerEntry entry) {
        return safe(entry.getDebitAmount()).subtract(safe(entry.getCreditAmount()));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
