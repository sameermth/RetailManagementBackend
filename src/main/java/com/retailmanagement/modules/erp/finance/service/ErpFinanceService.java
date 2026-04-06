package com.retailmanagement.modules.erp.finance.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.erp.audit.service.AuditEventWriter;
import com.retailmanagement.modules.erp.common.ErpSecurityUtils;
import com.retailmanagement.modules.erp.expense.repository.ExpenseRepository;
import com.retailmanagement.modules.erp.expense.repository.ExpenseCategoryRepository;
import com.retailmanagement.modules.erp.finance.dto.ErpFinanceDtos;
import com.retailmanagement.modules.erp.finance.entity.Account;
import com.retailmanagement.modules.erp.finance.entity.LedgerEntry;
import com.retailmanagement.modules.erp.finance.entity.Voucher;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.repository.BankStatementEntryRepository;
import com.retailmanagement.modules.erp.finance.repository.LedgerEntryRepository;
import com.retailmanagement.modules.erp.finance.repository.RecurringJournalLineRepository;
import com.retailmanagement.modules.erp.finance.repository.VoucherRepository;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.purchase.entity.SupplierPayment;
import com.retailmanagement.modules.erp.purchase.repository.PurchaseReceiptRepository;
import com.retailmanagement.modules.erp.purchase.repository.SupplierPaymentAllocationRepository;
import com.retailmanagement.modules.erp.purchase.repository.SupplierPaymentRepository;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.returns.repository.PurchaseReturnRepository;
import com.retailmanagement.modules.erp.returns.repository.SalesReturnRepository;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptAllocationRepository;
import com.retailmanagement.modules.erp.sales.repository.CustomerReceiptRepository;
import com.retailmanagement.modules.erp.sales.repository.SalesInvoiceRepository;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import com.retailmanagement.modules.erp.service.repository.ServiceReplacementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpFinanceService {

    private final AccountRepository accountRepository;
    private final BankStatementEntryRepository bankStatementEntryRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final RecurringJournalLineRepository recurringJournalLineRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final CustomerReceiptAllocationRepository customerReceiptAllocationRepository;
    private final SupplierPaymentAllocationRepository supplierPaymentAllocationRepository;
    private final CustomerReceiptRepository customerReceiptRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final ServiceReplacementRepository serviceReplacementRepository;
    private final AuditEventWriter auditEventWriter;

    @Transactional(readOnly = true)
    public List<Account> listAccounts(Long organizationId, String accountType, boolean includeInactive) {
        if (accountType == null || accountType.isBlank()) {
            return includeInactive
                    ? accountRepository.findByOrganizationIdOrderByCodeAsc(organizationId)
                    : accountRepository.findByOrganizationIdAndIsActiveTrueOrderByCodeAsc(organizationId);
        }
        return includeInactive
                ? accountRepository.findByOrganizationIdAndAccountTypeOrderByCodeAsc(organizationId, accountType)
                : accountRepository.findByOrganizationIdAndAccountTypeAndIsActiveTrueOrderByCodeAsc(organizationId, accountType);
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long organizationId, Long id) {
        return accountRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public Account createAccount(ErpFinanceDtos.CreateAccountRequest request) {
        Long organizationId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);

        accountRepository.findByOrganizationIdAndCode(organizationId, request.code()).ifPresent(existing -> {
            throw new BusinessException("Account code already exists for organization: " + request.code());
        });

        if (!List.of("ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE").contains(request.accountType())) {
            throw new BusinessException("Unsupported account type: " + request.accountType());
        }

        if (request.parentAccountId() != null) {
            accountRepository.findByIdAndOrganizationId(request.parentAccountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found: " + request.parentAccountId()));
        }

        Account account = new Account();
        account.setOrganizationId(organizationId);
        account.setCode(request.code());
        account.setName(request.name());
        account.setAccountType(request.accountType());
        account.setParentAccountId(request.parentAccountId());
        account.setIsSystem(Boolean.TRUE.equals(request.isSystem()));
        account.setIsActive(request.isActive() == null || request.isActive());

        Account saved = accountRepository.save(account);

        auditEventWriter.write(
                organizationId,
                null,
                "FINANCE_ACCOUNT",
                "account",
                saved.getId(),
                saved.getCode(),
                "CREATE",
                null,
                null,
                null,
                "Finance account created",
                "{\"code\":\"" + escape(saved.getCode()) + "\",\"name\":\"" + escape(saved.getName()) + "\"}"
        );

        return saved;
    }

    public Account updateAccount(Long id, ErpFinanceDtos.UpdateAccountRequest request) {
        Long organizationId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Account account = accountRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (Boolean.TRUE.equals(account.getIsSystem())) {
            throw new BusinessException("System accounts cannot be modified");
        }

        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        String accountType = request.accountType().trim().toUpperCase();
        validateAccountType(accountType);

        if (accountRepository.existsByOrganizationIdAndCodeAndIdNot(organizationId, code, id)) {
            throw new BusinessException("Account code already exists for organization: " + code);
        }

        if (request.parentAccountId() != null) {
            if (request.parentAccountId().equals(id)) {
                throw new BusinessException("Account cannot be its own parent");
            }
            Account parent = accountRepository.findByIdAndOrganizationId(request.parentAccountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent account not found: " + request.parentAccountId()));
            if (Boolean.TRUE.equals(parent.getIsSystem()) && !parent.getAccountType().equals(accountType)) {
                throw new BusinessException("Parent account type does not match the selected account type");
            }
        }

        account.setCode(code);
        account.setName(name);
        account.setAccountType(accountType);
        account.setParentAccountId(request.parentAccountId());
        if (request.isActive() != null) {
            account.setIsActive(request.isActive());
        }

        Account saved = accountRepository.save(account);
        auditEventWriter.write(
                organizationId,
                null,
                "FINANCE_ACCOUNT",
                "account",
                saved.getId(),
                saved.getCode(),
                "UPDATE",
                null,
                null,
                null,
                "Finance account updated",
                "{\"code\":\"" + escape(saved.getCode()) + "\",\"name\":\"" + escape(saved.getName()) + "\",\"active\":" + saved.getIsActive() + "}"
        );
        return saved;
    }

    public void deleteAccount(Long organizationId, Long id) {
        Account account = accountRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (Boolean.TRUE.equals(account.getIsSystem())) {
            throw new BusinessException("System accounts cannot be deleted");
        }
        if (accountRepository.existsByOrganizationIdAndParentAccountId(organizationId, id)) {
            throw new BusinessException("Account cannot be deleted while child accounts still reference it");
        }
        if (ledgerEntryRepository.existsByOrganizationIdAndAccountId(organizationId, id)) {
            throw new BusinessException("Account cannot be deleted because ledger entries already reference it");
        }
        if (bankStatementEntryRepository.existsByOrganizationIdAndAccountId(organizationId, id)) {
            throw new BusinessException("Account cannot be deleted because bank reconciliation entries already reference it");
        }
        if (expenseCategoryRepository.existsByOrganizationIdAndExpenseAccountId(organizationId, id)) {
            throw new BusinessException("Account cannot be deleted because expense categories reference it");
        }
        if (recurringJournalLineRepository.existsByAccountId(id)) {
            throw new BusinessException("Account cannot be deleted because recurring journals reference it");
        }

        accountRepository.delete(account);
        auditEventWriter.write(
                organizationId,
                null,
                "FINANCE_ACCOUNT",
                "account",
                account.getId(),
                account.getCode(),
                "DELETE",
                null,
                null,
                null,
                "Finance account deleted",
                "{\"code\":\"" + escape(account.getCode()) + "\",\"name\":\"" + escape(account.getName()) + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public List<Voucher> listVouchers(Long organizationId) {
        return voucherRepository.findTop100ByOrganizationIdOrderByVoucherDateDescIdDesc(organizationId);
    }

    @Transactional(readOnly = true)
    public VoucherDetails getVoucher(Long organizationId, Long id) {
        Voucher voucher = voucherRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));
        List<LedgerEntry> lines = ledgerEntryRepository.findByOrganizationIdAndVoucherIdOrderByIdAsc(organizationId, id);
        return new VoucherDetails(voucher, lines);
    }

    public Voucher createVoucher(ErpFinanceDtos.CreateVoucherRequest request) {
        Long organizationId = request.organizationId() != null ? request.organizationId() : ErpSecurityUtils.currentOrganizationId().orElse(1L);
        Long branchId = request.branchId() != null ? request.branchId() : ErpSecurityUtils.currentBranchId().orElse(1L);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ErpFinanceDtos.CreateVoucherLineRequest line : request.lines()) {
            if ((line.debitAmount() == null || line.debitAmount().compareTo(BigDecimal.ZERO) == 0)
                    && (line.creditAmount() == null || line.creditAmount().compareTo(BigDecimal.ZERO) == 0)) {
                throw new BusinessException("Each voucher line must have debit or credit amount");
            }
            if (line.debitAmount() != null && line.creditAmount() != null
                    && line.debitAmount().compareTo(BigDecimal.ZERO) > 0
                    && line.creditAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException("Voucher line cannot have both debit and credit");
            }

            accountRepository.findByIdAndOrganizationId(line.accountId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + line.accountId()));

            if (line.customerId() != null) {
                customerRepository.findByIdAndOrganizationId(line.customerId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + line.customerId()));
            }
            if (line.supplierId() != null) {
                supplierRepository.findByIdAndOrganizationId(line.supplierId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + line.supplierId()));
            }

            totalDebit = totalDebit.add(zero(line.debitAmount()));
            totalCredit = totalCredit.add(zero(line.creditAmount()));
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException("Voucher is not balanced. Debit=" + totalDebit + ", Credit=" + totalCredit);
        }

        Voucher voucher = new Voucher();
        voucher.setOrganizationId(organizationId);
        voucher.setBranchId(branchId);
        voucher.setVoucherNumber(generateVoucherNumber(organizationId, request.voucherType()));
        voucher.setVoucherDate(request.voucherDate() != null ? request.voucherDate() : LocalDate.now());
        voucher.setVoucherType(request.voucherType());
        voucher.setReferenceType(request.referenceType());
        voucher.setReferenceId(request.referenceId());
        voucher.setRemarks(request.remarks());
        voucher.setStatus("POSTED");

        Voucher savedVoucher = voucherRepository.save(voucher);

        for (ErpFinanceDtos.CreateVoucherLineRequest line : request.lines()) {
            LedgerEntry entry = new LedgerEntry();
            entry.setOrganizationId(organizationId);
            entry.setBranchId(branchId);
            entry.setVoucherId(savedVoucher.getId());
            entry.setAccountId(line.accountId());
            entry.setEntryDate(savedVoucher.getVoucherDate());
            entry.setDebitAmount(zero(line.debitAmount()));
            entry.setCreditAmount(zero(line.creditAmount()));
            entry.setNarrative(line.narrative() != null ? line.narrative() : request.remarks());
            entry.setCustomerId(line.customerId());
            entry.setSupplierId(line.supplierId());
            entry.setSalesInvoiceId(line.salesInvoiceId());
            entry.setPurchaseReceiptId(line.purchaseReceiptId());
            ledgerEntryRepository.save(entry);
        }

        auditEventWriter.write(
                organizationId,
                branchId,
                "FINANCE_VOUCHER",
                "voucher",
                savedVoucher.getId(),
                savedVoucher.getVoucherNumber(),
                "POST",
                null,
                null,
                null,
                "Finance voucher posted",
                "{\"voucherType\":\"" + escape(savedVoucher.getVoucherType()) + "\",\"totalDebit\":\"" + totalDebit + "\",\"totalCredit\":\"" + totalCredit + "\"}"
        );

        return savedVoucher;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> daybook(Long organizationId, LocalDate fromDate, LocalDate toDate) {
        return ledgerEntryRepository.findByOrganizationIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                organizationId,
                fromDate != null ? fromDate : LocalDate.now().minusDays(7),
                toDate != null ? toDate : LocalDate.now()
        );
    }

    @Transactional(readOnly = true)
    public PartyLedgerDetails partyLedger(Long organizationId, ErpFinanceDtos.PartyLedgerQuery query) {
        List<LedgerEntry> entries;
        if ("CUSTOMER".equalsIgnoreCase(query.partyType())) {
            customerRepository.findByIdAndOrganizationId(query.partyId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + query.partyId()));
            entries = ledgerEntryRepository.findByOrganizationIdAndCustomerIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                    organizationId, query.partyId(), query.fromDate(), query.toDate());
        } else if ("SUPPLIER".equalsIgnoreCase(query.partyType())) {
            supplierRepository.findByIdAndOrganizationId(query.partyId(), organizationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + query.partyId()));
            entries = ledgerEntryRepository.findByOrganizationIdAndSupplierIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                    organizationId, query.partyId(), query.fromDate(), query.toDate());
        } else {
            throw new BusinessException("Unsupported party type: " + query.partyType());
        }

        BigDecimal debit = entries.stream().map(LedgerEntry::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(LedgerEntry::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PartyLedgerDetails(query.partyType().toUpperCase(), query.partyId(), entries, debit, credit);
    }

    @Transactional(readOnly = true)
    public AccountLedgerDetails accountLedger(Long organizationId, ErpFinanceDtos.AccountLedgerQuery query) {
        if (query.fromDate().isAfter(query.toDate())) {
            throw new BusinessException("fromDate cannot be after toDate");
        }
        Account account = accountRepository.findByIdAndOrganizationId(query.accountId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + query.accountId()));
        List<LedgerEntry> entries = ledgerEntryRepository.findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                organizationId, query.accountId(), query.fromDate(), query.toDate());
        BigDecimal debit = entries.stream().map(LedgerEntry::getDebitAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = entries.stream().map(LedgerEntry::getCreditAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AccountLedgerDetails(account, entries, debit, credit, debit.subtract(credit));
    }

    @Transactional(readOnly = true)
    public OutstandingSummary outstanding(Long organizationId, ErpFinanceDtos.OutstandingQuery query) {
        LocalDate asOfDate = query.asOfDate() != null ? query.asOfDate() : LocalDate.now();
        String partyType = query.partyType().toUpperCase();
        if ("CUSTOMER".equals(partyType)) {
            if (query.partyId() != null) {
                customerRepository.findByIdAndOrganizationId(query.partyId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + query.partyId()));
            }
            List<DocumentOutstanding> documents = new ArrayList<>();
            for (SalesInvoice invoice : salesInvoiceRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId)) {
                if (query.partyId() != null && !query.partyId().equals(invoice.getCustomerId())) {
                    continue;
                }
                if (invoice.getInvoiceDate().isAfter(asOfDate)) {
                    continue;
                }
                BigDecimal allocated = allocatedForSalesInvoice(organizationId, invoice.getId(), asOfDate);
                BigDecimal returned = salesReturnRepository
                        .findByOrganizationIdAndOriginalSalesInvoiceIdAndStatusAndReturnDateLessThanEqual(organizationId, invoice.getId(), "POSTED", asOfDate)
                        .stream()
                        .map(r -> safe(r.getTotalAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal outstanding = safe(invoice.getTotalAmount()).subtract(allocated).subtract(returned).max(BigDecimal.ZERO);
                if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                documents.add(new DocumentOutstanding(
                        "CUSTOMER",
                        invoice.getCustomerId(),
                        invoice.getId(),
                        invoice.getInvoiceNumber(),
                        invoice.getInvoiceDate(),
                        invoice.getDueDate(),
                        safe(invoice.getTotalAmount()),
                        allocated.add(returned),
                        outstanding,
                        bucket(invoice.getDueDate(), asOfDate),
                        customerAllocations(organizationId, invoice.getId(), asOfDate)
                ));
            }
            return summarize(partyType, query.partyId(), asOfDate, documents);
        }
        if ("SUPPLIER".equals(partyType)) {
            if (query.partyId() != null) {
                supplierRepository.findByIdAndOrganizationId(query.partyId(), organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + query.partyId()));
            }
            List<DocumentOutstanding> documents = new ArrayList<>();
            for (PurchaseReceipt receipt : purchaseReceiptRepository.findByOrganizationIdOrderByDueDateAscIdAsc(organizationId)) {
                if (query.partyId() != null && !query.partyId().equals(receipt.getSupplierId())) {
                    continue;
                }
                if (receipt.getReceiptDate().isAfter(asOfDate)) {
                    continue;
                }
                BigDecimal allocated = allocatedForPurchaseReceipt(organizationId, receipt.getId(), asOfDate);
                BigDecimal returned = purchaseReturnRepository
                        .findByOrganizationIdAndOriginalPurchaseReceiptIdAndStatusAndReturnDateLessThanEqual(organizationId, receipt.getId(), "POSTED", asOfDate)
                        .stream()
                        .map(r -> safe(r.getTotalAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal outstanding = safe(receipt.getTotalAmount()).subtract(allocated).subtract(returned).max(BigDecimal.ZERO);
                if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                documents.add(new DocumentOutstanding(
                        "SUPPLIER",
                        receipt.getSupplierId(),
                        receipt.getId(),
                        receipt.getReceiptNumber(),
                        receipt.getReceiptDate(),
                        receipt.getDueDate(),
                        safe(receipt.getTotalAmount()),
                        allocated.add(returned),
                        outstanding,
                        bucket(receipt.getDueDate(), asOfDate),
                        supplierAllocations(organizationId, receipt.getId(), asOfDate)
                ));
            }
            return summarize(partyType, query.partyId(), asOfDate, documents);
        }
        throw new BusinessException("Unsupported party type: " + query.partyType());
    }

    @Transactional(readOnly = true)
    public AdjustmentReviewSummary adjustmentReview(Long organizationId, ErpFinanceDtos.AdjustmentReviewQuery query) {
        LocalDate toDate = query.toDate() != null ? query.toDate() : LocalDate.now();
        LocalDate fromDate = query.fromDate() != null ? query.fromDate() : toDate.minusDays(30);
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        List<SalesReturn> salesReturns = salesReturnRepository
                .findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(organizationId, fromDate, toDate);
        List<PurchaseReturn> purchaseReturns = purchaseReturnRepository
                .findByOrganizationIdAndReturnDateBetweenOrderByReturnDateDescIdDesc(organizationId, fromDate, toDate);
        List<ServiceReplacement> replacements = serviceReplacementRepository
                .findByOrganizationIdAndIssuedOnBetweenOrderByIssuedOnDescIdDesc(organizationId, fromDate, toDate);

        Map<String, Voucher> voucherByReference = new HashMap<>();
        for (Voucher voucher : voucherRepository.findByOrganizationIdAndVoucherDateBetweenOrderByVoucherDateAscIdAsc(organizationId, fromDate, toDate)) {
            if (voucher.getReferenceType() == null || voucher.getReferenceId() == null) {
                continue;
            }
            voucherByReference.put(voucherKey(voucher.getReferenceType(), voucher.getReferenceId()), voucher);
        }

        List<AdjustmentReviewDocument> documents = new ArrayList<>();
        for (SalesReturn salesReturn : salesReturns) {
            Voucher voucher = voucherByReference.get(voucherKey("SALES_RETURN", salesReturn.getId()));
            documents.add(new AdjustmentReviewDocument(
                    "SALES_RETURN",
                    salesReturn.getId(),
                    salesReturn.getReturnNumber(),
                    salesReturn.getReturnDate(),
                    salesReturn.getStatus(),
                    "CUSTOMER",
                    salesReturn.getCustomerId(),
                    "SALES_INVOICE",
                    salesReturn.getOriginalSalesInvoiceId(),
                    "CREDIT_NOTE",
                    safe(salesReturn.getSubtotal()),
                    safe(salesReturn.getTaxAmount()),
                    safe(salesReturn.getTotalAmount()),
                    voucher
            ));
        }
        for (PurchaseReturn purchaseReturn : purchaseReturns) {
            Voucher voucher = voucherByReference.get(voucherKey("PURCHASE_RETURN", purchaseReturn.getId()));
            documents.add(new AdjustmentReviewDocument(
                    "PURCHASE_RETURN",
                    purchaseReturn.getId(),
                    purchaseReturn.getReturnNumber(),
                    purchaseReturn.getReturnDate(),
                    purchaseReturn.getStatus(),
                    "SUPPLIER",
                    purchaseReturn.getSupplierId(),
                    "PURCHASE_RECEIPT",
                    purchaseReturn.getOriginalPurchaseReceiptId(),
                    "DEBIT_NOTE",
                    safe(purchaseReturn.getSubtotal()),
                    safe(purchaseReturn.getTaxAmount()),
                    safe(purchaseReturn.getTotalAmount()),
                    voucher
            ));
        }
        for (ServiceReplacement replacement : replacements) {
            Voucher voucher = voucherByReference.get(voucherKey("SERVICE_REPLACEMENT", replacement.getId()));
            documents.add(new AdjustmentReviewDocument(
                    "SERVICE_REPLACEMENT",
                    replacement.getId(),
                    replacement.getReplacementNumber(),
                    replacement.getIssuedOn(),
                    replacement.getStatus(),
                    "CUSTOMER",
                    replacement.getCustomerId(),
                    linkedReplacementDocumentType(replacement),
                    linkedReplacementDocumentId(replacement),
                    replacement.getReplacementType(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    sumLedgerMovement(organizationId, voucher == null ? null : voucher.getId()),
                    voucher
            ));
        }

        documents.sort(Comparator
                .comparing(AdjustmentReviewDocument::documentDate).reversed()
                .thenComparing(AdjustmentReviewDocument::documentId, Comparator.reverseOrder()));

        BigDecimal salesReturnTotal = salesReturns.stream().map(SalesReturn::getTotalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchaseReturnTotal = purchaseReturns.stream().map(PurchaseReturn::getTotalAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal replacementTotal = replacements.stream()
                .map(r -> {
                    Voucher voucher = voucherByReference.get(voucherKey("SERVICE_REPLACEMENT", r.getId()));
                    return sumLedgerMovement(organizationId, voucher == null ? null : voucher.getId());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AdjustmentReviewSummary(
                fromDate,
                toDate,
                salesReturnTotal,
                purchaseReturnTotal,
                replacementTotal,
                documents
        );
    }

    @Transactional(readOnly = true)
    public CashBankSummary cashBankSummary(Long organizationId, ErpFinanceDtos.CashBankSummaryQuery query) {
        LocalDate toDate = query.toDate() != null ? query.toDate() : LocalDate.now();
        LocalDate fromDate = query.fromDate() != null ? query.fromDate() : toDate.minusDays(30);
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        List<Account> accounts = new ArrayList<>();
        accountRepository.findByOrganizationIdAndCode(organizationId, "CASH").ifPresent(accounts::add);
        accountRepository.findByOrganizationIdAndCode(organizationId, "BANK").ifPresent(accounts::add);

        List<CashBankAccountSummary> summaries = new ArrayList<>();
        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalOutflow = BigDecimal.ZERO;
        for (Account account : accounts) {
            List<LedgerEntry> entries = ledgerEntryRepository.findByOrganizationIdAndAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
                    organizationId, account.getId(), fromDate, toDate);
            BigDecimal debit = entries.stream().map(LedgerEntry::getDebitAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal credit = entries.stream().map(LedgerEntry::getCreditAmount).map(this::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalInflow = totalInflow.add(debit);
            totalOutflow = totalOutflow.add(credit);
            summaries.add(new CashBankAccountSummary(account.getId(), account.getCode(), account.getName(), debit, credit, debit, credit, debit.subtract(credit)));
        }
        return new CashBankSummary(fromDate, toDate, totalInflow, totalOutflow, totalInflow.subtract(totalOutflow), summaries);
    }

    @Transactional(readOnly = true)
    public ExpenseSummary expenseSummary(Long organizationId, ErpFinanceDtos.ExpenseSummaryQuery query) {
        LocalDate toDate = query.toDate() != null ? query.toDate() : LocalDate.now();
        LocalDate fromDate = query.fromDate() != null ? query.fromDate() : toDate.minusDays(30);
        if (fromDate.isAfter(toDate)) {
            throw new BusinessException("fromDate cannot be after toDate");
        }

        BigDecimal totalExpenseAmount = safe(expenseRepository.getTotalExpensesForPeriod(organizationId, fromDate, toDate));
        BigDecimal paidExpenseAmount = safe(expenseRepository.getTotalExpensesForPeriodByStatus(organizationId, fromDate, toDate, "PAID"));
        BigDecimal approvedUnpaidExpenseAmount = safe(expenseRepository.getTotalExpensesForPeriodByStatus(organizationId, fromDate, toDate, "APPROVED"));
        BigDecimal cancelledExpenseAmount = safe(expenseRepository.getTotalExpensesForPeriodByStatus(organizationId, fromDate, toDate, "CANCELLED"));

        List<ExpenseCategorySummary> categories = expenseRepository.getExpensesGroupedByCategory(organizationId, fromDate, toDate).stream()
                .map(row -> new ExpenseCategorySummary(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        safe((BigDecimal) row[3]),
                        row[4] == null ? 0L : ((Number) row[4]).longValue()
                ))
                .toList();

        return new ExpenseSummary(fromDate, toDate, totalExpenseAmount, paidExpenseAmount, approvedUnpaidExpenseAmount, cancelledExpenseAmount, categories);
    }

    private String generateVoucherNumber(Long organizationId, String voucherType) {
        long count = voucherRepository.findTop100ByOrganizationIdOrderByVoucherDateDescIdDesc(organizationId).size() + 1L;
        return (voucherType == null ? "JOURNAL" : voucherType) + "-" + organizationId + "-" + String.format("%05d", count);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void validateAccountType(String accountType) {
        if (!List.of("ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE").contains(accountType)) {
            throw new BusinessException("Unsupported account type: " + accountType);
        }
    }

    private BigDecimal allocatedForSalesInvoice(Long organizationId, Long salesInvoiceId, LocalDate asOfDate) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .filter(allocation -> voucherDateForCustomerReceipt(organizationId, allocation.getCustomerReceiptId(), asOfDate))
                .map(allocation -> zero(allocation.getAllocatedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal allocatedForPurchaseReceipt(Long organizationId, Long purchaseReceiptId, LocalDate asOfDate) {
        return supplierPaymentAllocationRepository.findByPurchaseReceiptId(purchaseReceiptId).stream()
                .filter(allocation -> voucherDateForSupplierPayment(organizationId, allocation.getSupplierPaymentId(), asOfDate))
                .map(allocation -> zero(allocation.getAllocatedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<AllocationReference> customerAllocations(Long organizationId, Long salesInvoiceId, LocalDate asOfDate) {
        return customerReceiptAllocationRepository.findBySalesInvoiceIdOrderByIdAsc(salesInvoiceId).stream()
                .map(allocation -> toCustomerAllocationReference(organizationId, allocation.getCustomerReceiptId(), allocation.getAllocatedAmount()))
                .filter(reference -> reference != null && (reference.allocationDate() == null || !reference.allocationDate().isAfter(asOfDate)))
                .toList();
    }

    private List<AllocationReference> supplierAllocations(Long organizationId, Long purchaseReceiptId, LocalDate asOfDate) {
        return supplierPaymentAllocationRepository.findByPurchaseReceiptId(purchaseReceiptId).stream()
                .map(allocation -> toSupplierAllocationReference(organizationId, allocation.getSupplierPaymentId(), allocation.getAllocatedAmount()))
                .filter(reference -> reference != null && (reference.allocationDate() == null || !reference.allocationDate().isAfter(asOfDate)))
                .toList();
    }

    private AllocationReference toCustomerAllocationReference(Long organizationId, Long customerReceiptId, BigDecimal allocatedAmount) {
        CustomerReceipt receipt = customerReceiptRepository.findById(customerReceiptId).orElse(null);
        if (receipt == null || !organizationId.equals(receipt.getOrganizationId())) {
            return null;
        }
        return new AllocationReference("CUSTOMER_RECEIPT", receipt.getId(), receipt.getReceiptNumber(), receipt.getReceiptDate(), safe(allocatedAmount));
    }

    private AllocationReference toSupplierAllocationReference(Long organizationId, Long supplierPaymentId, BigDecimal allocatedAmount) {
        SupplierPayment payment = supplierPaymentRepository.findById(supplierPaymentId).orElse(null);
        if (payment == null || !organizationId.equals(payment.getOrganizationId())) {
            return null;
        }
        return new AllocationReference("SUPPLIER_PAYMENT", payment.getId(), payment.getPaymentNumber(), payment.getPaymentDate(), safe(allocatedAmount));
    }

    private boolean voucherDateForCustomerReceipt(Long organizationId, Long customerReceiptId, LocalDate asOfDate) {
        return voucherRepository.findByOrganizationIdAndReferenceTypeAndReferenceId(
                        organizationId, "CUSTOMER_RECEIPT", customerReceiptId)
                .map(v -> !v.getVoucherDate().isAfter(asOfDate))
                .orElse(true);
    }

    private boolean voucherDateForSupplierPayment(Long organizationId, Long supplierPaymentId, LocalDate asOfDate) {
        return voucherRepository.findByOrganizationIdAndReferenceTypeAndReferenceId(
                        organizationId, "SUPPLIER_PAYMENT", supplierPaymentId)
                .map(v -> !v.getVoucherDate().isAfter(asOfDate))
                .orElse(true);
    }

    private OutstandingSummary summarize(String partyType, Long partyId, LocalDate asOfDate, List<DocumentOutstanding> documents) {
        BigDecimal totalOutstanding = documents.stream()
                .map(DocumentOutstanding::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        AgingBuckets aging = new AgingBuckets(
                sumByBucket(documents, "CURRENT"),
                sumByBucket(documents, "1_30"),
                sumByBucket(documents, "31_60"),
                sumByBucket(documents, "61_90"),
                sumByBucket(documents, "90_PLUS")
        );
        return new OutstandingSummary(partyType, partyId, asOfDate, totalOutstanding, documents, aging);
    }

    private BigDecimal sumByBucket(List<DocumentOutstanding> documents, String bucket) {
        return documents.stream()
                .filter(document -> bucket.equals(document.agingBucket()))
                .map(DocumentOutstanding::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String bucket(LocalDate dueDate, LocalDate asOfDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, asOfDate);
        if (overdueDays <= 0) {
            return "CURRENT";
        }
        if (overdueDays <= 30) {
            return "1_30";
        }
        if (overdueDays <= 60) {
            return "31_60";
        }
        if (overdueDays <= 90) {
            return "61_90";
        }
        return "90_PLUS";
    }

    private BigDecimal sumLedgerMovement(Long organizationId, Long voucherId) {
        if (voucherId == null) {
            return BigDecimal.ZERO;
        }
        return ledgerEntryRepository.findByOrganizationIdAndVoucherIdOrderByIdAsc(organizationId, voucherId).stream()
                .map(LedgerEntry::getDebitAmount)
                .map(this::safe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String voucherKey(String referenceType, Long referenceId) {
        return referenceType + ":" + referenceId;
    }

    private String linkedReplacementDocumentType(ServiceReplacement replacement) {
        if (replacement.getSalesReturnId() != null) {
            return "SALES_RETURN";
        }
        if (replacement.getWarrantyClaimId() != null) {
            return "WARRANTY_CLAIM";
        }
        if (replacement.getServiceTicketId() != null) {
            return "SERVICE_TICKET";
        }
        return null;
    }

    private Long linkedReplacementDocumentId(ServiceReplacement replacement) {
        if (replacement.getSalesReturnId() != null) {
            return replacement.getSalesReturnId();
        }
        if (replacement.getWarrantyClaimId() != null) {
            return replacement.getWarrantyClaimId();
        }
        if (replacement.getServiceTicketId() != null) {
            return replacement.getServiceTicketId();
        }
        return null;
    }

    public record VoucherDetails(Voucher voucher, List<LedgerEntry> entries) {}
    public record PartyLedgerDetails(String partyType, Long partyId, List<LedgerEntry> entries, BigDecimal totalDebit, BigDecimal totalCredit) {}
    public record AccountLedgerDetails(Account account, List<LedgerEntry> entries, BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal netMovement) {}
    public record DocumentOutstanding(String partyType, Long partyId, Long documentId, String documentNumber, LocalDate documentDate,
                                      LocalDate dueDate, BigDecimal totalAmount, BigDecimal allocatedAmount, BigDecimal outstandingAmount,
                                      String agingBucket, List<AllocationReference> allocations) {}
    public record AllocationReference(String allocationType, Long allocationId, String allocationNumber, LocalDate allocationDate,
                                      BigDecimal allocatedAmount) {}
    public record AgingBuckets(BigDecimal current, BigDecimal bucket1To30, BigDecimal bucket31To60, BigDecimal bucket61To90, BigDecimal bucket90Plus) {}
    public record OutstandingSummary(String partyType, Long partyId, LocalDate asOfDate, BigDecimal totalOutstanding,
                                     List<DocumentOutstanding> documents, AgingBuckets aging) {}
    public record AdjustmentReviewDocument(String documentType, Long documentId, String documentNumber, LocalDate documentDate,
                                           String status, String partyType, Long partyId, String linkedDocumentType,
                                           Long linkedDocumentId, String adjustmentType, BigDecimal subtotal,
                                           BigDecimal taxAmount, BigDecimal totalAmount, Voucher voucher) {}
    public record AdjustmentReviewSummary(LocalDate fromDate, LocalDate toDate, BigDecimal salesReturnTotal,
                                          BigDecimal purchaseReturnTotal, BigDecimal replacementTotal,
                                          List<AdjustmentReviewDocument> documents) {}
    public record CashBankAccountSummary(Long accountId, String accountCode, String accountName, BigDecimal totalDebit,
                                         BigDecimal totalCredit, BigDecimal inflow, BigDecimal outflow, BigDecimal netMovement) {}
    public record CashBankSummary(LocalDate fromDate, LocalDate toDate, BigDecimal totalInflow, BigDecimal totalOutflow,
                                  BigDecimal netMovement, List<CashBankAccountSummary> accounts) {}
    public record ExpenseCategorySummary(Long expenseCategoryId, String expenseCategoryCode, String expenseCategoryName,
                                         BigDecimal totalAmount, Long expenseCount) {}
    public record ExpenseSummary(LocalDate fromDate, LocalDate toDate, BigDecimal totalExpenseAmount,
                                 BigDecimal paidExpenseAmount, BigDecimal approvedUnpaidExpenseAmount,
                                 BigDecimal cancelledExpenseAmount, List<ExpenseCategorySummary> categories) {}
}
