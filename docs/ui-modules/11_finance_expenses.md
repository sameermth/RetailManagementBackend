# Finance and Expenses UX Design

## Outcomes

- help owners and accountants move from operational documents into accounting context without losing traceability
- provide a strong chart-of-accounts and voucher foundation for future finance growth
- make report-style screens practical for daily review, not just export
- keep expenses easy for business users while preserving accounting drill-down

## Primary Users

- owner
- admin
- accountant
- finance operator

## Information Architecture

Primary navigation under `Finance`:
- overview
- chart of accounts
- vouchers
- ledgers
- outstanding
- bank reconciliation
- recurring journals
- expenses

Recommended secondary navigation:
- list
- detail
- reports
- settings where needed

## Screen Inventory

### Finance Overview

Purpose:
- finance command center for owners and accountants

Widgets:
- receivables summary
- payables summary
- cash and bank summary
- latest bank import batches
- expense summary
- recent vouchers
- overdue customer balances
- overdue supplier balances

Quick actions:
- create voucher
- create expense
- open outstanding
- open bank reconciliation

### Chart of Accounts

Purpose:
- maintain account master data

### Account Detail

Recommended sections:
- account summary
- parent hierarchy
- usage references later
- recent ledger movements later

### Voucher List

Purpose:
- operational list of posted and draft finance entries

### Voucher Detail

Recommended tabs:
- summary
- entries
- linked references
- activity

### Voucher Create

Purpose:
- create journal-style accounting entries

### Ledger and Outstanding Workspace

Purpose:
- report-style drill-down area

Views:
- daybook
- account ledger
- party ledger
- customer outstanding
- supplier outstanding
- cash and bank summary

### Expense List

Purpose:
- day-to-day expense management

### Expense Detail

Recommended tabs:
- summary
- payment
- accounting impact later
- activity

### Expense Create or Pay

Purpose:
- business-user-friendly expense entry with finance follow-up

### Bank Reconciliation

Purpose:
- match bank statement lines to finance entries

## List Page Blueprint

### Chart of Accounts

Columns:
- account code
- account name
- account type
- parent account
- active

Filters:
- account type
- active
- search by code or name

Top actions:
- create account

Row actions:
- view
- edit
- deactivate or activate
- delete if allowed

### Voucher List

Columns:
- voucher number
- voucher date
- voucher type
- branch
- total amount
- status
- narration

Filters:
- voucher type
- branch
- date range
- status
- search by voucher number or narration

Top actions:
- create voucher

Row actions:
- view
- edit if draft
- reverse later

### Expense List

Columns:
- expense number
- date
- category
- branch
- amount
- payment status
- created by

Filters:
- branch
- category
- payment status
- date range
- search by expense number or category

Top actions:
- create expense

Row actions:
- view
- edit if allowed
- pay expense

## Detail Page Blueprint

### Account Detail

Header summary:
- account code
- account name
- account type
- parent
- active status

Key sections:
- hierarchy summary
- description or notes
- recent usage later

Actions:
- edit
- activate or deactivate

### Voucher Detail

Header summary:
- voucher number
- date
- type
- branch
- total amount
- status

Entries table:
- ledger account
- debit
- credit
- branch if applicable
- remarks

Linked references section:
- related invoice
- related receipt
- related expense

Actions:
- edit if draft
- print later

### Expense Detail

Header summary:
- expense number
- date
- branch
- category
- amount
- payment status

Key sections:
- expense summary
- vendor or payee details if present
- payment details
- notes and attachment area later

Actions:
- edit
- pay expense
- open accounting entry later

### Bank Reconciliation Detail Experience

Recommended layout:
- left panel with bank statement lines
- center panel with candidate ledger matches
- right summary panel with reconciliation state

Row-level actions:
- match
- auto reconcile import batch
- split later
- mark unreconciled

## Create or Edit Flow Blueprint

### Chart of Account Create

Sections:
- account identity: code, name
- type and parent
- active state
- notes

### Voucher Create

Step 1 header:
- voucher date
- branch
- voucher type
- narration

Step 2 ledger entries:
- account picker
- debit
- credit
- remarks

Step 3 validation:
- total debit equals total credit
- missing account warnings

Step 4 review and save:
- summary
- save draft or post depending on backend behavior

### Expense Create

Step 1 basic info:
- date
- branch
- category
- amount

Step 2 payee and notes:
- supplier or payee if applicable
- remarks

Step 3 payment:
- unpaid now
- mark paid now if supported

Step 4 review:
- expense summary

### Expense Payment Flow

Step 1 select expense:
- preloaded from detail or picked from list

Step 2 payment details:
- payment date
- mode
- amount
- reference

Step 3 review and confirm:
- current outstanding
- new balance after payment

## State Handling

Accounts:
- empty chart-of-accounts state for fresh setup
- duplicate account code validation

Vouchers:
- draft
- posted
- imbalanced entry validation
- reference conflict warning later

Expenses:
- unpaid
- partially paid if supported
- paid
- approval pending if workflow is enabled later

Bank reconciliation:
- unreconciled
- matched
- ambiguous candidate matches
- import empty state
- import batch history empty state

Permission states:
- finance viewers should see detail and reports but not posting controls
- expense creators may not have voucher permissions

## CTA Map

Finance overview:
- primary: Create voucher
- secondary: Create expense
- tertiary: Open bank reconciliation

Chart of accounts:
- primary: Create account
- row primary: View
- row secondary: Edit

Voucher list:
- primary: Create voucher
- row primary: View
- row secondary: Edit if draft

Expense list:
- primary: Create expense
- row primary: View
- row secondary: Pay expense

Bank reconciliation:
- primary: Match entry
- secondary: Skip or mark unresolved

## Desktop and Mobile Notes

Desktop:
- finance reporting and reconciliation should be optimized for wide layouts
- side panel drill-down is very useful for voucher detail from ledger screens

Mobile:
- finance review is possible, but full reconciliation and voucher entry should be treated as desktop-first
- expense creation can be mobile-friendly

Tablet:
- good for expense entry and finance overview
- less ideal for dense ledger reports unless simplified

## Cross-Module Navigation

- invoice detail to customer outstanding
- supplier payment to supplier outstanding
- expense detail to voucher detail later
- voucher detail to source invoice, receipt, or expense
- finance overview to reports and bank reconciliation

## Key Forms And Backend Fields

### Account Create Or Update

- `organizationId`
- `code`
- `name`
- `accountType`
- `parentAccountId`
- `isSystem`
- `isActive`

### Voucher Create

- `organizationId`
- `branchId`
- `voucherDate`
- `voucherType`
- `referenceType`
- `referenceId`
- `remarks`
- `lines[]`

Voucher line fields:
- `accountId`
- `debitAmount`
- `creditAmount`
- `narrative`
- `customerId`
- `supplierId`
- `salesInvoiceId`
- `purchaseReceiptId`

### Expense Category Create

- `organizationId`
- `code`
- `name`
- `expenseAccountId`

### Expense Create

- `organizationId`
- `branchId`
- `expenseCategoryId`
- `expenseDate`
- `dueDate`
- `amount`
- `paymentMethod`
- `receiptUrl`
- `remarks`
- `markPaid`

### Expense Pay

- `paymentMethod`
- `paidDate`
- `remarks`

### Bank Statement Import

- `organizationId`
- `branchId`
- `accountId`
- `sourceType`
- `sourceReference`
- `sourceFileName`
- `remarks`
- `lines[]`

Statement line fields:
- `entryDate`
- `valueDate`
- `referenceNumber`
- `description`
- `debitAmount`
- `creditAmount`
- `remarks`

### Reconcile Statement Entry

- `ledgerEntryId`
- `remarks`

### Bank Import Batch Review

UI should use batch response fields:
- `importReference`
- `sourceType`
- `sourceReference`
- `sourceFileName`
- `statementFromDate`
- `statementToDate`
- `entryCount`
- `totalDebitAmount`
- `totalCreditAmount`
- `status`
- `importedAt`
- `entries[]`

## Screen To API Map

- Accounts:
  - `GET /api/erp/finance/accounts?organizationId=...`
  - `GET /api/erp/finance/accounts/{id}`
  - `POST /api/erp/finance/accounts`
  - `PUT /api/erp/finance/accounts/{id}`
  - `DELETE /api/erp/finance/accounts/{id}`
- Vouchers:
  - `GET /api/erp/finance/vouchers?organizationId=...`
  - `GET /api/erp/finance/vouchers/{id}`
  - `POST /api/erp/finance/vouchers`
- Ledgers and finance reports:
  - `GET /api/erp/finance/daybook?organizationId=...&fromDate=...&toDate=...`
  - `POST /api/erp/finance/party-ledger?organizationId=...`
  - `POST /api/erp/finance/account-ledger?organizationId=...`
  - `POST /api/erp/finance/outstanding?organizationId=...`
  - `POST /api/erp/finance/adjustments/review?organizationId=...`
  - `POST /api/erp/finance/cash-bank-summary?organizationId=...`
  - `POST /api/erp/finance/expense-summary?organizationId=...`
- Expense categories and expenses:
  - `GET /api/erp/expenses/categories?organizationId=...`
  - `POST /api/erp/expenses/categories`
  - `GET /api/erp/expenses?organizationId=...`
  - `GET /api/erp/expenses/{id}`
  - `POST /api/erp/expenses`
  - `POST /api/erp/expenses/{id}/pay`
- Bank reconciliation:
  - `POST /api/erp/finance/bank-reconciliation/statements`
  - `GET /api/erp/finance/bank-reconciliation/imports?organizationId=...`
  - `GET /api/erp/finance/bank-reconciliation/imports/{id}?organizationId=...`
  - `POST /api/erp/finance/bank-reconciliation/imports/{id}/auto-reconcile?organizationId=...`
  - `POST /api/erp/finance/bank-reconciliation/summary`
  - `GET /api/erp/finance/bank-reconciliation/statements/{id}/candidates`
  - `POST /api/erp/finance/bank-reconciliation/statements/{id}/reconcile`
  - `POST /api/erp/finance/bank-reconciliation/statements/{id}/unreconcile`
