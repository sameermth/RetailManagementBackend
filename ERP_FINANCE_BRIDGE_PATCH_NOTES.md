# ERP finance-lite bridge patch

This patch adds the finance-lite layer on top of the ERP bridge.

## What it adds
- finance accounts
- vouchers
- ledger entries
- daybook API
- party ledger API
- manual voucher posting with balanced double-entry validation
- immutable audit events for finance operations

## Endpoints
- `GET /api/erp/finance/accounts?organizationId=...&accountType=...`
- `POST /api/erp/finance/accounts`
- `GET /api/erp/finance/vouchers?organizationId=...`
- `GET /api/erp/finance/vouchers/{id}?organizationId=...`
- `POST /api/erp/finance/vouchers`
- `GET /api/erp/finance/daybook?organizationId=...&fromDate=...&toDate=...`
- `POST /api/erp/finance/party-ledger?organizationId=...`

## Notes
- This bridge intentionally uses `voucher + ledger_entry` because the ERP schema does not include a separate `voucher_line` table.
- Supplier/customer-specific ledgers are derived from tagged `ledger_entry` rows.
