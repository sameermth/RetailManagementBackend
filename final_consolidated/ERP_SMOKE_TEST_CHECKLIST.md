# ERP smoke test checklist

Use this after loading:
1. `001_create_erp_v4_schema.sql`
2. `002_seed_erp_v4_master_data.sql`
3. `003_seed_erp_v4_sample_data.sql`

## 1) System ping
- `GET /api/erp/system/ping`

## 2) Organizations
- `GET /api/v1/organizations`
- `GET /api/erp/system/ping`

## 3) Product/catalog
- `GET /api/v1/products?organizationId=1`
- `GET /api/erp/inventory-tracking/batches/product/{productId}?organizationId=1`
- `GET /api/erp/inventory-tracking/serials/product/{productId}?organizationId=1`

## 4) Inventory
- `GET /api/erp/inventory-balances?organizationId=1&warehouseId=1`
- `GET /api/erp/stock-movements/warehouse/1?organizationId=1`
- `POST /api/erp/inventory-operations/adjustments/manual`
- `POST /api/erp/inventory-operations/transfers`

## 5) Sales
- `GET /api/erp/sales/invoices?organizationId=1`
- `POST /api/erp/sales/invoices`
- `GET /api/erp/sales/receipts?organizationId=1`
- `POST /api/erp/sales/receipts`
- `POST /api/erp/sales/receipts/{id}/allocate`

## 6) Purchases
- `GET /api/erp/purchases/orders?organizationId=1`
- `POST /api/erp/purchases/orders`
- `GET /api/erp/purchases/receipts?organizationId=1`
- `POST /api/erp/purchases/receipts`
- `POST /api/erp/purchases/supplier-payments`
- `POST /api/erp/purchases/supplier-payments/{id}/allocate`

## 7) Service + warranty
- `GET /api/erp/service/tickets?organizationId=1`
- `POST /api/erp/service/tickets`
- `POST /api/erp/service/tickets/{id}/assign`
- `POST /api/erp/service/tickets/{id}/visits`
- `POST /api/erp/service/tickets/{id}/close`
- `GET /api/erp/service/warranty-claims?organizationId=1`
- `POST /api/erp/service/warranty-claims`

## 8) Approvals
- `GET /api/erp/approvals/rules?organizationId=1&entityType=purchase_order`
- `POST /api/erp/approvals/requests?organizationId=1&branchId=1`
- `POST /api/erp/approvals/requests/{id}/approve?organizationId=1`

## 9) Finance
- `GET /api/erp/finance/accounts?organizationId=1`
- `POST /api/erp/finance/vouchers`
- `GET /api/erp/finance/daybook?organizationId=1&fromDate=2026-01-01&toDate=2026-12-31`
- `POST /api/erp/finance/party-ledger?organizationId=1`

## 10) Audit
- `GET /api/erp/audit-events?entityType=sales_invoice&entityId=1`

## Notes
- Use the sample data IDs first; then create new documents through ERP APIs.
- Validate that stock movements and audit events are created after sales, purchase, adjustment, transfer, receipt, and service actions.
