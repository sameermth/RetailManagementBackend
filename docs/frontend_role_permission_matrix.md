# Frontend Role, Permission, and Demo User Matrix

This document is the UI handoff reference for role-based rendering in the retail management frontend.

## Auth Response Contract

After login or organization switch, the backend now returns:

- `roles`
- `permissions`
- `memberships`
- active org context fields such as `organizationId`, `organizationCode`, and `organizationName`

Reference:
- [JwtResponse.java](/Users/sameerkhan/Projects/retail-management-backend/src/main/java/com/retailmanagement/modules/auth/dto/response/JwtResponse.java)
- [AuthServiceImpl.java](/Users/sameerkhan/Projects/retail-management-backend/src/main/java/com/retailmanagement/modules/auth/service/impl/AuthServiceImpl.java)

Recommended frontend rule:
- use `permissions` as the primary gate for pages, nav items, actions, and lazy-loaded modules
- use `roles` only for broad labels, dashboards, or UX hints
- keep backend `@PreAuthorize(...)` as the final enforcement layer

## Permission Catalog

Seed source:
- [002_seed_erp_v4_master_data.sql](/Users/sameerkhan/Projects/retail-management-backend/src/main/resources/db/changelog/002_seed_erp_v4_master_data.sql)

Permissions currently seeded:

- `dashboard.view`
- `masters.view`
- `masters.manage`
- `sales.view`
- `sales.create`
- `sales.cancel`
- `sales.return`
- `purchases.view`
- `purchases.create`
- `purchases.approve`
- `purchases.receive`
- `inventory.view`
- `inventory.adjust`
- `inventory.transfer`
- `payments.customer`
- `payments.supplier`
- `expenses.view`
- `expenses.create`
- `expenses.approve`
- `reports.view`
- `service.view`
- `service.manage`
- `service.claims`
- `settings.manage`
- `users.manage`
- `approvals.manage`

## Role -> Permission Matrix

### `OWNER`
- Full access to dashboard, masters, sales, purchases, inventory, payments, expenses, reports, service, settings, users, approvals

Permissions:
- `dashboard.view`
- `masters.view`
- `masters.manage`
- `sales.view`
- `sales.create`
- `sales.cancel`
- `sales.return`
- `purchases.view`
- `purchases.create`
- `purchases.approve`
- `purchases.receive`
- `inventory.view`
- `inventory.adjust`
- `inventory.transfer`
- `payments.customer`
- `payments.supplier`
- `expenses.view`
- `expenses.create`
- `expenses.approve`
- `reports.view`
- `service.view`
- `service.manage`
- `service.claims`
- `settings.manage`
- `users.manage`
- `approvals.manage`

### `ADMIN`
- Almost full operational access except settings ownership areas

Permissions:
- `dashboard.view`
- `masters.view`
- `masters.manage`
- `sales.view`
- `sales.create`
- `sales.cancel`
- `sales.return`
- `purchases.view`
- `purchases.create`
- `purchases.approve`
- `purchases.receive`
- `inventory.view`
- `inventory.adjust`
- `inventory.transfer`
- `payments.customer`
- `payments.supplier`
- `expenses.view`
- `expenses.create`
- `expenses.approve`
- `reports.view`
- `service.view`
- `service.manage`
- `service.claims`
- `users.manage`
- `approvals.manage`

### `ACCOUNTANT`
- Finance-heavy access with reporting and transactional visibility

Permissions:
- `dashboard.view`
- `masters.view`
- `sales.view`
- `sales.create`
- `purchases.view`
- `purchases.create`
- `purchases.receive`
- `inventory.view`
- `payments.customer`
- `payments.supplier`
- `expenses.view`
- `expenses.create`
- `reports.view`

### `STORE_MANAGER`
- Store operations, stock, service, and limited purchase receiving

Permissions:
- `dashboard.view`
- `masters.view`
- `sales.view`
- `sales.create`
- `purchases.view`
- `purchases.receive`
- `inventory.view`
- `inventory.adjust`
- `inventory.transfer`
- `reports.view`
- `service.view`
- `service.manage`

### `CASHIER`
- Counter sales and customer receipt collection

Permissions:
- `dashboard.view`
- `sales.view`
- `sales.create`
- `payments.customer`
- `inventory.view`

### `PURCHASE_OPERATOR`
- Purchase creation and goods receiving

Permissions:
- `dashboard.view`
- `purchases.view`
- `purchases.create`
- `purchases.receive`
- `inventory.view`

### `TECHNICIAN`
- Service and inventory visibility for after-sales work

Permissions:
- `dashboard.view`
- `service.view`
- `service.manage`
- `inventory.view`

### `VIEWER`
- Read-only operational visibility

Permissions:
- `dashboard.view`
- `masters.view`
- `sales.view`
- `purchases.view`
- `inventory.view`
- `expenses.view`
- `reports.view`
- `service.view`

## Suggested UI Module Gates

### Dashboard
- Show if user has `dashboard.view`

### Masters
- Show list pages if `masters.view`
- Show create/edit actions if `masters.manage`

### Sales
- Show quote/order/invoice screens if `sales.view`
- Show create/convert/post flows if `sales.create`
- Show cancel action if `sales.cancel`
- Show returns if `sales.return`

### Purchases
- Show PO/receipt/payment views if `purchases.view`
- Show PO creation if `purchases.create`
- Show PO approval actions if `purchases.approve`
- Show receipt posting if `purchases.receive`

### Inventory
- Show balances, tracking, movements if `inventory.view`
- Show stock adjustment screens if `inventory.adjust`
- Show stock transfer screens if `inventory.transfer`

### Finance / Payments
- Show customer receipt screens if `payments.customer`
- Show supplier payment screens if `payments.supplier`

### Expenses
- Show expense views if `expenses.view`
- Show expense create/post if `expenses.create`
- Show expense approval UI if `expenses.approve`

### Reports
- Show reports and dashboard analytics if `reports.view`

### Service / Warranty
- Show tickets and claims if `service.view`
- Show manage/update ticket actions if `service.manage`
- Show warranty claim-specific actions if `service.claims`

### Users / Settings / Approval Center
- Show settings if `settings.manage`
- Show user management if `users.manage`
- Show approval queue and rule management if `approvals.manage`

## Suggested Navigation By Role

### Owner
- Dashboard
- Masters
- Sales
- Purchases
- Inventory
- Finance
- Expenses
- Reports
- Service
- Users
- Approvals
- Settings

### Admin
- Dashboard
- Masters
- Sales
- Purchases
- Inventory
- Finance
- Expenses
- Reports
- Service
- Users
- Approvals

### Accountant
- Dashboard
- Sales
- Purchases
- Inventory
- Customer Receipts
- Supplier Payments
- Expenses
- Reports

### Store Manager
- Dashboard
- Masters
- Sales
- Purchases Receive
- Inventory
- Reports
- Service

### Cashier
- Dashboard
- POS / Sales
- Customer Receipts
- Inventory lookup

### Purchase Operator
- Dashboard
- Purchases
- Inventory lookup

### Technician
- Dashboard
- Service
- Inventory lookup

## Seeded Demo Users

Seed source:
- [003_seed_erp_v4_sample_data.sql](/Users/sameerkhan/Projects/retail-management-backend/src/main/resources/db/changelog/003_seed_erp_v4_sample_data.sql)

All demo users use password:
- `secret123`

### Shakti Power Centre (`SPC`)
- `SPC-OWN-01` — Owner
- `SPC-OWN-02` — Owner
- `SPC-ADM-01` — Admin
- `SPC-ACC-01` — Accountant
- `SPC-MGR-01` — Store Manager
- `SPC-CAS-01` — Cashier
- `SPC-PUR-01` — Purchase Operator
- `SPC-TEC-01` — Technician

### Urban Home Lights (`UHL`)
- `UHL-OWN-01` — Owner
- `UHL-OWN-02` — Owner
- `UHL-ADM-01` — Admin
- `UHL-ACC-01` — Accountant
- `UHL-MGR-01` — Store Manager
- `UHL-CAS-01` — Cashier
- `UHL-TEC-01` — Technician

## Best Demo Logins By Scenario

### Full owner demo
- `SPC-OWN-01 / secret123`
- `UHL-OWN-01 / secret123`

### Finance demo
- `SPC-ACC-01 / secret123`
- `UHL-ACC-01 / secret123`

### Sales counter demo
- `SPC-CAS-01 / secret123`
- `UHL-CAS-01 / secret123`

### Purchase demo
- `SPC-PUR-01 / secret123`

### Service / warranty demo
- `SPC-TEC-01 / secret123`
- `UHL-TEC-01 / secret123`

### Store operations demo
- `SPC-MGR-01 / secret123`
- `UHL-MGR-01 / secret123`

## Frontend Implementation Recommendation

On login:
- store `token`
- store `roles`
- store `permissions`
- store `memberships`
- store active org context

For rendering:
- gate menu groups by `permissions`
- lazy-load modules only when the required permission exists
- hide page-level actions when write permission is missing
- still handle `403` from backend gracefully, because backend remains the final authority

Example logic:

```ts
const canViewSales = permissions.includes("sales.view");
const canCreateSales = permissions.includes("sales.create");
const canManageApprovals = permissions.includes("approvals.manage");
```

This is the safest model because it stays aligned with backend `@PreAuthorize(...)` checks.
