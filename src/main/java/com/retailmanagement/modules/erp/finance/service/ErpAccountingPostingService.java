package com.retailmanagement.modules.erp.finance.service;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.modules.erp.finance.entity.Account;
import com.retailmanagement.modules.erp.finance.entity.LedgerEntry;
import com.retailmanagement.modules.erp.finance.entity.Voucher;
import com.retailmanagement.modules.erp.finance.repository.AccountRepository;
import com.retailmanagement.modules.erp.finance.repository.LedgerEntryRepository;
import com.retailmanagement.modules.erp.finance.repository.VoucherRepository;
import com.retailmanagement.modules.erp.expense.entity.Expense;
import com.retailmanagement.modules.erp.inventory.entity.InventoryBalance;
import com.retailmanagement.modules.erp.inventory.repository.InventoryBalanceRepository;
import com.retailmanagement.modules.erp.purchase.entity.PurchaseReceipt;
import com.retailmanagement.modules.erp.returns.entity.PurchaseReturn;
import com.retailmanagement.modules.erp.returns.entity.SalesReturn;
import com.retailmanagement.modules.erp.sales.entity.CustomerReceipt;
import com.retailmanagement.modules.erp.sales.entity.SalesInvoice;
import com.retailmanagement.modules.erp.service.entity.ServiceReplacement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ErpAccountingPostingService {

    private static final String SALES_INVOICE_REF = "SALES_INVOICE";
    private static final String CUSTOMER_RECEIPT_REF = "CUSTOMER_RECEIPT";
    private static final String PURCHASE_RECEIPT_REF = "PURCHASE_RECEIPT";
    private static final String SALES_RETURN_REF = "SALES_RETURN";
    private static final String PURCHASE_RETURN_REF = "PURCHASE_RETURN";
    private static final String SUPPLIER_PAYMENT_REF = "SUPPLIER_PAYMENT";
    private static final String EXPENSE_ACCRUAL_REF = "EXPENSE_ACCRUAL";
    private static final String EXPENSE_PAID_REF = "EXPENSE_PAID";
    private static final String EXPENSE_SETTLEMENT_REF = "EXPENSE_SETTLEMENT";
    private static final String SERVICE_REPLACEMENT_REF = "SERVICE_REPLACEMENT";

    private final AccountRepository accountRepository;
    private final VoucherRepository voucherRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;

    public void postSalesInvoice(SalesInvoice invoice, BigDecimal costOfGoodsSold) {
        Voucher voucher = ensureVoucher(invoice.getOrganizationId(), invoice.getBranchId(), "SALES", SALES_INVOICE_REF, invoice.getId(),
                "VCH-" + invoice.getInvoiceNumber(), invoice.getInvoiceDate(), "Auto sales voucher");

        Map<String, Account> accounts = accounts(invoice.getOrganizationId(), List.of("AR", "SALES", "OUTPUT_GST", "COGS", "INVENTORY"));
        BigDecimal revenue = safe(invoice.getSubtotal()).subtract(safe(invoice.getDiscountAmount()));
        BigDecimal tax = safe(invoice.getTaxAmount());
        BigDecimal receivable = safe(invoice.getTotalAmount());
        BigDecimal cogs = safe(costOfGoodsSold);

        upsertEntry(voucher, accounts.get("AR"), receivable, BigDecimal.ZERO, "Sales receivable", invoice.getCustomerId(), null, invoice.getId(), null);
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("SALES"), BigDecimal.ZERO, revenue, "Sales revenue", invoice.getCustomerId(), null, invoice.getId(), null);
        }
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("OUTPUT_GST"), BigDecimal.ZERO, tax, "Output GST", invoice.getCustomerId(), null, invoice.getId(), null);
        }
        if (cogs.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("COGS"), cogs, BigDecimal.ZERO, "Cost of goods sold", invoice.getCustomerId(), null, invoice.getId(), null);
            upsertEntry(voucher, accounts.get("INVENTORY"), BigDecimal.ZERO, cogs, "Inventory reduction on sale", invoice.getCustomerId(), null, invoice.getId(), null);
        }
    }

    public void postCustomerReceipt(CustomerReceipt receipt) {
        Voucher voucher = ensureVoucher(receipt.getOrganizationId(), receipt.getBranchId(), "RECEIPT", CUSTOMER_RECEIPT_REF, receipt.getId(),
                "VCH-" + receipt.getReceiptNumber(), receipt.getReceiptDate(), "Customer receipt voucher");

        String settlementCode = paymentAccountCode(receipt.getPaymentMethod());
        Map<String, Account> accounts = accounts(receipt.getOrganizationId(), List.of(settlementCode, "AR"));
        BigDecimal amount = safe(receipt.getAmount());

        upsertEntry(voucher, accounts.get(settlementCode), amount, BigDecimal.ZERO, "Customer receipt", receipt.getCustomerId(), null, null, null);
        upsertEntry(voucher, accounts.get("AR"), BigDecimal.ZERO, amount, "Customer receipt settlement", receipt.getCustomerId(), null, null, null);
    }

    public void postPurchaseReceipt(PurchaseReceipt receipt) {
        Voucher voucher = ensureVoucher(receipt.getOrganizationId(), receipt.getBranchId(), "PURCHASE", PURCHASE_RECEIPT_REF, receipt.getId(),
                "VCH-" + receipt.getReceiptNumber(), receipt.getReceiptDate(), "Purchase receipt voucher");

        Map<String, Account> accounts = accounts(receipt.getOrganizationId(), List.of("INVENTORY", "INPUT_GST", "AP"));
        BigDecimal base = safe(receipt.getSubtotal());
        BigDecimal tax = safe(receipt.getTaxAmount());
        BigDecimal total = safe(receipt.getTotalAmount());

        if (base.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("INVENTORY"), base, BigDecimal.ZERO, "Inventory received", null, receipt.getSupplierId(), null, receipt.getId());
        }
        if (tax.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("INPUT_GST"), tax, BigDecimal.ZERO, "Input GST on purchase", null, receipt.getSupplierId(), null, receipt.getId());
        }
        upsertEntry(voucher, accounts.get("AP"), BigDecimal.ZERO, total, "Supplier payable", null, receipt.getSupplierId(), null, receipt.getId());
    }

    public void postSalesReturn(SalesReturn salesReturn, BigDecimal inventoryValue) {
        Voucher voucher = ensureVoucher(salesReturn.getOrganizationId(), salesReturn.getBranchId(), "SALES_RETURN", SALES_RETURN_REF, salesReturn.getId(),
                "VCH-" + salesReturn.getReturnNumber(), salesReturn.getReturnDate(), "Sales return voucher");

        Map<String, Account> accounts = accounts(salesReturn.getOrganizationId(), List.of("AR", "SALES", "OUTPUT_GST", "COGS", "INVENTORY"));
        BigDecimal revenueReversal = safe(salesReturn.getSubtotal());
        BigDecimal taxReversal = safe(salesReturn.getTaxAmount());
        BigDecimal receivableReduction = safe(salesReturn.getTotalAmount());
        BigDecimal inventoryReversal = safe(inventoryValue);

        if (revenueReversal.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("SALES"), revenueReversal, BigDecimal.ZERO, "Sales return revenue reversal", salesReturn.getCustomerId(), null, null, null);
        }
        if (taxReversal.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("OUTPUT_GST"), taxReversal, BigDecimal.ZERO, "Sales return GST reversal", salesReturn.getCustomerId(), null, null, null);
        }
        upsertEntry(voucher, accounts.get("AR"), BigDecimal.ZERO, receivableReduction, "Customer credit on sales return", salesReturn.getCustomerId(), null, null, null);
        if (inventoryReversal.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("INVENTORY"), inventoryReversal, BigDecimal.ZERO, "Inventory added back on sales return", salesReturn.getCustomerId(), null, null, null);
            upsertEntry(voucher, accounts.get("COGS"), BigDecimal.ZERO, inventoryReversal, "COGS reversal on sales return", salesReturn.getCustomerId(), null, null, null);
        }
    }

    public void postPurchaseReturn(PurchaseReturn purchaseReturn) {
        Voucher voucher = ensureVoucher(purchaseReturn.getOrganizationId(), purchaseReturn.getBranchId(), "PURCHASE_RETURN", PURCHASE_RETURN_REF, purchaseReturn.getId(),
                "VCH-" + purchaseReturn.getReturnNumber(), purchaseReturn.getReturnDate(), "Purchase return voucher");

        Map<String, Account> accounts = accounts(purchaseReturn.getOrganizationId(), List.of("INVENTORY", "INPUT_GST", "AP"));
        BigDecimal baseReversal = safe(purchaseReturn.getSubtotal());
        BigDecimal taxReversal = safe(purchaseReturn.getTaxAmount());
        BigDecimal payableReduction = safe(purchaseReturn.getTotalAmount());

        upsertEntry(voucher, accounts.get("AP"), payableReduction, BigDecimal.ZERO, "Purchase return payable reduction", null, purchaseReturn.getSupplierId(), null, null);
        if (baseReversal.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("INVENTORY"), BigDecimal.ZERO, baseReversal, "Inventory reversal on purchase return", null, purchaseReturn.getSupplierId(), null, null);
        }
        if (taxReversal.compareTo(BigDecimal.ZERO) > 0) {
            upsertEntry(voucher, accounts.get("INPUT_GST"), BigDecimal.ZERO, taxReversal, "Input GST reversal on purchase return", null, purchaseReturn.getSupplierId(), null, null);
        }
    }

    public void postSupplierPayment(com.retailmanagement.modules.erp.purchase.entity.SupplierPayment payment) {
        Voucher voucher = ensureVoucher(payment.getOrganizationId(), payment.getBranchId(), "PAYMENT", SUPPLIER_PAYMENT_REF, payment.getId(),
                "VCH-" + payment.getPaymentNumber(), payment.getPaymentDate(), "Supplier payment voucher");

        String settlementCode = paymentAccountCode(payment.getPaymentMethod());
        Map<String, Account> accounts = accounts(payment.getOrganizationId(), List.of(settlementCode, "AP"));
        BigDecimal amount = safe(payment.getAmount());

        upsertEntry(voucher, accounts.get("AP"), amount, BigDecimal.ZERO, "Supplier payment settlement", null, payment.getSupplierId(), null, null);
        upsertEntry(voucher, accounts.get(settlementCode), BigDecimal.ZERO, amount, "Supplier payment", null, payment.getSupplierId(), null, null);
    }

    public void postExpenseAccrual(Expense expense, Long expenseAccountId) {
        Voucher voucher = ensureVoucher(expense.getOrganizationId(), expense.getBranchId(), "EXPENSE", EXPENSE_ACCRUAL_REF, expense.getId(),
                "VCH-" + expense.getExpenseNumber() + "-ACR", expense.getExpenseDate(), "Expense accrual voucher");

        Map<String, Account> accounts = accounts(expense.getOrganizationId(), List.of("EXPENSE_PAYABLE"));
        Account expenseAccount = accountById(expense.getOrganizationId(), expenseAccountId);
        BigDecimal amount = safe(expense.getAmount());

        upsertEntry(voucher, expenseAccount, amount, BigDecimal.ZERO, "Expense accrual", null, null, null, null, expense.getId());
        upsertEntry(voucher, accounts.get("EXPENSE_PAYABLE"), BigDecimal.ZERO, amount, "Expense payable", null, null, null, null, expense.getId());
    }

    public void postExpensePaid(Expense expense, Long expenseAccountId) {
        Voucher voucher = ensureVoucher(expense.getOrganizationId(), expense.getBranchId(), "EXPENSE", EXPENSE_PAID_REF, expense.getId(),
                "VCH-" + expense.getExpenseNumber(), expense.getExpenseDate(), "Paid expense voucher");

        String settlementCode = paymentAccountCode(expense.getPaymentMethod());
        Map<String, Account> accounts = accounts(expense.getOrganizationId(), List.of(settlementCode));
        Account expenseAccount = accountById(expense.getOrganizationId(), expenseAccountId);
        BigDecimal amount = safe(expense.getAmount());

        upsertEntry(voucher, expenseAccount, amount, BigDecimal.ZERO, "Expense posting", null, null, null, null, expense.getId());
        upsertEntry(voucher, accounts.get(settlementCode), BigDecimal.ZERO, amount, "Expense payment", null, null, null, null, expense.getId());
    }

    public void settleExpensePayment(Expense expense) {
        Voucher voucher = ensureVoucher(expense.getOrganizationId(), expense.getBranchId(), "PAYMENT", EXPENSE_SETTLEMENT_REF, expense.getId(),
                "VCH-" + expense.getExpenseNumber() + "-PAY", expense.getPaidAt() == null ? LocalDate.now() : expense.getPaidAt().toLocalDate(), "Expense settlement voucher");

        String settlementCode = paymentAccountCode(expense.getPaymentMethod());
        Map<String, Account> accounts = accounts(expense.getOrganizationId(), List.of(settlementCode, "EXPENSE_PAYABLE"));
        BigDecimal amount = safe(expense.getAmount());

        upsertEntry(voucher, accounts.get("EXPENSE_PAYABLE"), amount, BigDecimal.ZERO, "Expense payable settlement", null, null, null, null, expense.getId());
        upsertEntry(voucher, accounts.get(settlementCode), BigDecimal.ZERO, amount, "Expense settlement", null, null, null, null, expense.getId());
    }

    public void postServiceReplacement(ServiceReplacement replacement, BigDecimal inventoryValue, SalesReturn salesReturn) {
        BigDecimal replacementCost = safe(inventoryValue);
        String replacementType = replacement.getReplacementType() == null ? "" : replacement.getReplacementType().trim().toUpperCase();
        if ("SALES_RETURN_REPLACEMENT".equals(replacementType)) {
            if (salesReturn == null) {
                throw new BusinessException("Sales-return replacement accounting requires a linked sales return");
            }
            Voucher voucher = ensureVoucher(
                    replacement.getOrganizationId(),
                    replacement.getBranchId(),
                    "SERVICE_REPLACEMENT",
                    SERVICE_REPLACEMENT_REF,
                    replacement.getId(),
                    "VCH-" + replacement.getReplacementNumber(),
                    replacement.getIssuedOn(),
                    "Sales return replacement voucher"
            );
            Map<String, Account> accounts = accounts(replacement.getOrganizationId(), List.of("AR", "SALES", "OUTPUT_GST", "COGS", "INVENTORY"));
            BigDecimal revenue = safe(salesReturn.getSubtotal());
            BigDecimal tax = safe(salesReturn.getTaxAmount());
            BigDecimal customerCreditConsumption = safe(salesReturn.getTotalAmount());

            if (customerCreditConsumption.compareTo(BigDecimal.ZERO) > 0) {
                upsertEntry(voucher, accounts.get("AR"), customerCreditConsumption, BigDecimal.ZERO,
                        "Customer credit consumed on replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
            }
            if (revenue.compareTo(BigDecimal.ZERO) > 0) {
                upsertEntry(voucher, accounts.get("SALES"), BigDecimal.ZERO, revenue,
                        "Sales restored on exchange replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
            }
            if (tax.compareTo(BigDecimal.ZERO) > 0) {
                upsertEntry(voucher, accounts.get("OUTPUT_GST"), BigDecimal.ZERO, tax,
                        "Output GST restored on exchange replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
            }
            if (replacementCost.compareTo(BigDecimal.ZERO) > 0) {
                upsertEntry(voucher, accounts.get("COGS"), replacementCost, BigDecimal.ZERO,
                        "COGS on exchange replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
                upsertEntry(voucher, accounts.get("INVENTORY"), BigDecimal.ZERO, replacementCost,
                        "Inventory issued on exchange replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
            }
            return;
        }
        if (replacementCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String expenseCode = switch (replacementType) {
            case "WARRANTY_REPLACEMENT" -> "WARRANTY_EXPENSE";
            case "GOODWILL_REPLACEMENT" -> "GOODWILL_EXPENSE";
            default -> throw new BusinessException("Unsupported service replacement accounting type: " + replacement.getReplacementType());
        };

        Voucher voucher = ensureVoucher(
                replacement.getOrganizationId(),
                replacement.getBranchId(),
                "SERVICE_REPLACEMENT",
                SERVICE_REPLACEMENT_REF,
                replacement.getId(),
                "VCH-" + replacement.getReplacementNumber(),
                replacement.getIssuedOn(),
                "Service replacement voucher"
        );

        Map<String, Account> accounts = accounts(replacement.getOrganizationId(), List.of(expenseCode, "INVENTORY"));
        upsertEntry(voucher, accounts.get(expenseCode), replacementCost, BigDecimal.ZERO,
                "Service replacement expense", replacement.getCustomerId(), null, null, null, null, replacement.getId());
        upsertEntry(voucher, accounts.get("INVENTORY"), BigDecimal.ZERO, replacementCost,
                "Inventory issued for service replacement", replacement.getCustomerId(), null, null, null, null, replacement.getId());
    }

    public BigDecimal estimateSalesCost(Long organizationId, Long warehouseId, Long productId, BigDecimal baseQuantity) {
        BigDecimal qty = safe(baseQuantity);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal avgCost = inventoryBalanceRepository.findByOrganizationIdAndProductIdAndWarehouseId(organizationId, productId, warehouseId).stream()
                .map(InventoryBalance::getAvgCost)
                .filter(v -> v != null)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        return avgCost.multiply(qty);
    }

    private Voucher ensureVoucher(Long organizationId, Long branchId, String voucherType, String referenceType, Long referenceId,
                                  String voucherNumber, LocalDate voucherDate, String remarks) {
        return voucherRepository.findByOrganizationIdAndReferenceTypeAndReferenceId(organizationId, referenceType, referenceId)
                .orElseGet(() -> {
                    Voucher voucher = new Voucher();
                    voucher.setOrganizationId(organizationId);
                    voucher.setBranchId(branchId);
                    voucher.setVoucherType(voucherType);
                    voucher.setReferenceType(referenceType);
                    voucher.setReferenceId(referenceId);
                    voucher.setVoucherNumber(voucherNumber);
                    voucher.setVoucherDate(voucherDate);
                    voucher.setRemarks(remarks);
                    voucher.setStatus("POSTED");
                    return voucherRepository.save(voucher);
                });
    }

    private void upsertEntry(Voucher voucher, Account account, BigDecimal debit, BigDecimal credit, String narrative,
                             Long customerId, Long supplierId, Long salesInvoiceId, Long purchaseReceiptId) {
        upsertEntry(voucher, account, debit, credit, narrative, customerId, supplierId, salesInvoiceId, purchaseReceiptId, null);
    }

    private void upsertEntry(Voucher voucher, Account account, BigDecimal debit, BigDecimal credit, String narrative,
                             Long customerId, Long supplierId, Long salesInvoiceId, Long purchaseReceiptId, Long expenseId) {
        upsertEntry(voucher, account, debit, credit, narrative, customerId, supplierId, salesInvoiceId, purchaseReceiptId, expenseId, null);
    }

    private void upsertEntry(Voucher voucher, Account account, BigDecimal debit, BigDecimal credit, String narrative,
                             Long customerId, Long supplierId, Long salesInvoiceId, Long purchaseReceiptId, Long expenseId, Long serviceReplacementId) {
        if (account == null) {
            throw new BusinessException("Missing finance account for voucher posting " + voucher.getVoucherNumber());
        }
        if (safe(debit).compareTo(BigDecimal.ZERO) == 0 && safe(credit).compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        LedgerEntry entry = new LedgerEntry();
        entry.setOrganizationId(voucher.getOrganizationId());
        entry.setBranchId(voucher.getBranchId());
        entry.setVoucherId(voucher.getId());
        entry.setAccountId(account.getId());
        entry.setEntryDate(voucher.getVoucherDate());
        entry.setDebitAmount(safe(debit));
        entry.setCreditAmount(safe(credit));
        entry.setNarrative(narrative);
        entry.setCustomerId(customerId);
        entry.setSupplierId(supplierId);
        entry.setSalesInvoiceId(salesInvoiceId);
        entry.setPurchaseReceiptId(purchaseReceiptId);
        entry.setExpenseId(expenseId);
        entry.setServiceReplacementId(serviceReplacementId);
        ledgerEntryRepository.save(entry);
    }

    private Account accountById(Long organizationId, Long accountId) {
        return accountRepository.findByIdAndOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new BusinessException("Finance account not configured for id " + accountId + " in organization " + organizationId));
    }

    private Map<String, Account> accounts(Long organizationId, List<String> codes) {
        Map<String, Account> result = new HashMap<>();
        for (String code : codes) {
            result.put(code, accountRepository.findByOrganizationIdAndCode(organizationId, code)
                    .orElseThrow(() -> new BusinessException("Finance account not configured for code " + code + " in organization " + organizationId)));
        }
        return result;
    }

    private String paymentAccountCode(String paymentMethod) {
        if (paymentMethod == null) {
            return "CASH";
        }
        return switch (paymentMethod.trim().toUpperCase()) {
            case "BANK", "CARD", "UPI", "NEFT", "RTGS", "IMPS", "CHEQUE" -> "BANK";
            default -> "CASH";
        };
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
