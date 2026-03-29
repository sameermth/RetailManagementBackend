# ERP approval bridge patch

This patch adds a shared ERP approval workflow on top of the service+warranty bridge.

## What it adds
- `approval_rule`, `approval_request`, `approval_history` ERP entities
- repositories, DTOs, service, and controller
- generic request / approve / reject / cancel workflow
- entity status transitions for:
  - `purchase_order`
  - `stock_adjustment`
  - `sales_invoice`
- immutable audit event writes for approval actions

## New endpoints
- `GET /api/erp/approvals/rules?organizationId=...&entityType=...`
- `POST /api/erp/approvals/rules?organizationId=...&branchId=...`
- `GET /api/erp/approvals/requests?organizationId=...&status=...`
- `GET /api/erp/approvals/requests/{id}`
- `POST /api/erp/approvals/requests?organizationId=...&branchId=...`
- `POST /api/erp/approvals/requests/{id}/approve?organizationId=...`
- `POST /api/erp/approvals/requests/{id}/reject?organizationId=...`
- `POST /api/erp/approvals/requests/{id}/cancel?organizationId=...`

## Notes
- This bridge keeps role linkage as raw IDs / snapshots to avoid coupling too early to the legacy auth model.
- The service currently updates ERP entity statuses directly for the main bridge modules.
