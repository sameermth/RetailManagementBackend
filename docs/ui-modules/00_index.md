# UI UX Design Pack

This folder is the UI design reference for the retail management frontend.

The goal of this pack is to help the UI team design:

- page hierarchy
- module navigation
- list and detail views
- create and edit flows
- actions and decision points
- empty, loading, disabled, and error states
- permission-aware and subscription-aware rendering
- desktop and mobile behavior

## How To Use This Pack

For each module document, read in this order:

1. outcomes and user roles
2. information architecture
3. screen inventory
4. list page blueprint
5. detail page blueprint
6. create or edit flow blueprint
7. state handling
8. CTA map
9. responsive notes
10. cross-module navigation
11. key forms and backend fields
12. screen-to-API mapping

## Document Order

1. [Auth and Identity](./01_auth_identity.md)
2. [Organization, Branch, Warehouse](./02_foundation_org_branch_warehouse.md)
3. [Platform Admin](./03_platform_admin.md)
4. [Catalog, HSN, and Attributes](./04_catalog_hsn_attributes.md)
5. [Customers and Suppliers](./05_customers_suppliers.md)
6. [Purchases](./06_purchases.md)
7. [Sales](./07_sales.md)
8. [Inventory](./08_inventory.md)
9. [Returns](./09_returns.md)
10. [Service, Warranty, and Agreements](./10_service_warranty_agreements.md)
11. [Finance and Expenses](./11_finance_expenses.md)
12. [Subscriptions and Tax](./12_subscriptions_and_tax.md)
13. [Approvals, Workflow, and Audit](./13_approvals_workflow_audit.md)
14. [Dashboard, Reports, and Notifications](./14_dashboard_reports_notifications.md)
15. [UI Handoff Bundle](./15_ui_handoff_bundle.md)

## Shared UX Principles

- Use `permissions` as the primary gate for menus, pages, tabs, actions, and editability.
- If user lacks permission, prefer disabled controls with explanation instead of silently hiding important capabilities.
- If subscription blocks a feature, show the feature disabled with upgrade guidance.
- Keep active organization context visible in header or top rail at all times.
- Long flows should use a stepper or guided sections, not one oversized form.
- Documents like invoice, estimate, PO, receipt should be rendered from backend PDFs and previewed inside UI where possible.
- Related modules should be linked contextually. Example: invoice to receipts, product to stock, service ticket to warranty, store to subscription.

## Current Cross-Module Backend Rules

- Quote and order pricing come from store-product defaults and commercial master data.
- Invoice pricing, MRP, and tax are backend-derived and stock-aware when actual inward layers exist.
- In normal sales flows, line discount is the editable pricing input; unit price and tax should not be free text.
- Payment request status and gateway-provider status are different and both should be shown when present.
- Receipt posting remains the accounting source of truth even when payment links or provider sync states change.
- Purchase receipts capture inward commercial snapshots like unit cost, suggested sale price, and MRP.
- Supplier dispatch collaboration is a pre-receipt commitment layer; supplier portal updates do not post stock.
- GST compliance follows draft, submit, and sync lifecycle and should be surfaced as a status-driven workflow in UI.

## Shared State Handling Rules

Every module should define UI for:

- first-use empty state
- no-search-result state
- loading state
- partial data state
- permission denied state
- subscription denied state
- server validation error state
- action success feedback

## Existing References

- role and permission matrix: [../frontend_role_permission_matrix.md](../frontend_role_permission_matrix.md)
- backend data flow blueprint: [../backend_data_flow_blueprint.html](../backend_data_flow_blueprint.html)
- OpenAPI source of truth: `/v3/api-docs`
