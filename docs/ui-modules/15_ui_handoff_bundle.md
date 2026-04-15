# UI Handoff Bundle

This document is the cross-module handoff summary for design, product, and frontend implementation.

Use this file when the audience does not want to read every module doc in detail but still needs a concrete design direction.

For field-level details and exact API wiring, use the linked module documents.

## What This Bundle Covers

- top-level information architecture
- primary user journeys
- recommended app shell behavior
- reusable UX patterns
- desktop and mobile design rules
- module-by-module screen hierarchy
- implementation handoff guidance

## Recommended App Shell

### Header

Always visible:
- active organization name
- active branch if branch-scoped workflow is open
- global search later
- notifications entry
- profile menu
- organization switcher

### Left Navigation

Recommended order for ERP users:
1. Dashboard
2. Sales
3. Purchases
4. Inventory
5. Returns
6. Service
7. Catalog
8. Customers
9. Suppliers
10. Imports
11. Finance
12. Reports
13. Settings

Recommended order for platform admin:
1. Overview
2. Stores
3. Subscriptions
4. Store Teams
5. Support and Grievances
6. Feedback
7. Notifications
8. Audit Activity
9. System Health
10. Reports

### Right-Side Utility Patterns

Use side drawers for:
- quick entity preview
- audit or activity trail
- linked allocations
- history panels

Use full pages for:
- document detail
- create or edit flows
- approval decisions
- service workspace
- bank reconciliation

## Primary User Journeys

### Fresh Signup To Working Dashboard

1. User signs up
2. Backend returns onboarding response
3. UI opens organization registration instead of dashboard
4. User creates first organization
5. UI switches organization
6. User enters setup-oriented dashboard

Reference:
- [Auth and Identity](./01_auth_identity.md)
- [Organization, Branch, Warehouse](./02_foundation_org_branch_warehouse.md)

### Platform Admin Onboards Store

1. Platform admin opens store onboarding
2. Creates owner identity
3. Creates store
4. Optionally seeds first branch
5. Optionally activates plan
6. Lands on store detail
7. Continues to team, subscription, or catalog setup

Reference:
- [Platform Admin](./03_platform_admin.md)

### Product To Purchase To Sale To Service

1. Product created or linked into store catalog
2. Bundle components configured when the sellable item is a kit
3. Supplier mapping added
4. Inventory planning can suggest internal transfer or draft PO when stock falls below threshold
5. Purchase order created
6. Supplier may send dispatch ETA before goods arrive
7. Purchase receipt increases stock
8. Receipt putaway places stock into bins
9. Sales invoice reduces stock and may create ownership
10. Pick, pack, and dispatch track fulfillment after invoice
11. Receipt allocates payment
12. Service ticket or warranty claim uses ownership or serial context

Reference:
- [Catalog](./04_catalog_hsn_attributes.md)
- [Purchases](./06_purchases.md)
- [Sales](./07_sales.md)
- [Inventory](./08_inventory.md)
- [Service](./10_service_warranty_agreements.md)

### Bulk Data Onboarding

1. User downloads template for customers, suppliers, or products
2. UI uploads file for preview
3. UI shows row-level valid and invalid results
4. User executes import
5. Backend returns `importJobId`
6. UI opens import-job detail or recent history
7. User downloads failed rows and retries rejected records

Reference:
- [Catalog](./04_catalog_hsn_attributes.md)
- [Customers and Suppliers](./05_customers_suppliers.md)

## Reusable UX Patterns

### Pattern 1: Searchable Reference Picker

Use for:
- customers
- suppliers
- products
- categories
- brands
- UOM
- tax groups
- roles
- owner accounts

Behavior:
- async search
- show secondary metadata in result rows
- allow empty state with create action when business rules permit

### Pattern 2: Document Workspace

Use for:
- quote
- order
- invoice
- PO
- supplier dispatch review
- purchase receipt
- returns

Layout:
- header summary
- item table
- totals and GST summary
- linked entities
- external-collaboration timeline when applicable
- PDF actions
- activity or audit

### Pattern 3: Allocation Drawer

Use for:
- invoice payment requests
- customer receipts against invoices
- supplier payments against receipts

Layout:
- payment summary at top
- open documents table
- editable allocation inputs
- live remaining amount

### Pattern 4: Guided Stepper

Use for:
- store onboarding
- product creation
- invoice creation
- purchase receipt creation
- service agreement creation

### Pattern 5: Bulk Import Workspace

Use for:
- customers
- suppliers
- products

Layout:
- template download
- file upload
- preview summary
- row validation grid
- import execute CTA
- recent import jobs
- failed-row export

Behavior:
- preview is read-only and does not create history
- import execution returns `importJobId`
- recent import jobs should support detail view and failed-row CSV download

### Pattern 6: Disabled But Visible Gating

Use when:
- permission missing
- subscription feature missing

Permission message example:
- `You do not have access to perform this action.`

Subscription message example:
- `Upgrade to unlock this feature for this store.`

## Role-Based Dashboard Guidance

### Owner

Priority widgets:
- sales today
- dues summary
- profitability
- low stock
- replenishment recommendations
- service alerts

### Cashier

Priority widgets:
- active POS session
- quick checkout
- open invoices
- receipts today
- customer lookup
- pending dispatch invoices
- in-transit dispatches

### Accountant

Priority widgets:
- receivables
- payables
- overdue invoices
- bank reconciliation
- tax summary

### Store Manager

Priority widgets:
- branch performance
- stock alerts
- returns queue
- service queue

### Platform Admin

Priority widgets:
- active stores
- plan mix
- open support items
- failed notifications
- system health

## Module Hierarchy

### 1. Auth and Identity

Screens:
- login
- signup
- onboarding organization redirect
- organization switcher
- profile
- employee list
- employee detail
- employee create and edit

Reference:
- [01_auth_identity.md](./01_auth_identity.md)

### 2. Organization, Branch, Warehouse

Screens:
- organization list
- organization detail
- organization create and edit
- branch list and detail
- branch create and edit
- warehouse list and detail
- warehouse create and edit

Reference:
- [02_foundation_org_branch_warehouse.md](./02_foundation_org_branch_warehouse.md)

### 3. Platform Admin

Screens:
- platform overview
- store list
- store detail
- store onboarding
- store create for existing owner
- subscription console
- store teams
- support queue
- feedback
- notifications
- audit
- health

Reference:
- [03_platform_admin.md](./03_platform_admin.md)

### 4. Catalog

Screens:
- product list
- product detail
- product create wizard
- shared catalog discovery
- product pricing
- bundle builder
- supplier mapping
- attribute definition manager

Reference:
- [04_catalog_hsn_attributes.md](./04_catalog_hsn_attributes.md)

### 5. Customers and Suppliers

Screens:
- customer list and detail
- customer form
- customer terms
- supplier list and detail
- supplier form
- supplier terms
- supplier product links

Reference:
- [05_customers_suppliers.md](./05_customers_suppliers.md)

### 6. Purchases

Screens:
- purchases workspace
- PO list and detail
- PO create
- supplier dispatch review
- supplier portal purchase-order view
- receipt list and detail
- receipt create
- receipt putaway
- supplier payment list and detail
- supplier payment allocation

Reference:
- [06_purchases.md](./06_purchases.md)

### 7. Sales

Screens:
- sales workspace
- POS counter workspace
- POS session detail
- quote list and detail
- quote create
- order list and detail
- order create
- invoice list and detail
- invoice create
- payment request list and detail
- payment gateway provider picker and provider-status sync
- dispatch list and detail
- dispatch pick and pack actions
- receipt list and detail
- receipt create and allocate

Reference:
- [07_sales.md](./07_sales.md)

### 8. Inventory

Screens:
- inventory overview
- bin location list
- balance explorer
- bin stock detail
- product stock detail
- warehouse stock detail
- manual adjustment
- stock transfer
- stock count sessions
- replenishment planning
- reservations
- serial and batch search

Reference:
- [08_inventory.md](./08_inventory.md)

### 9. Returns

Screens:
- returns workspace
- sales return list and detail
- sales return intake
- sales return inspection and posting
- purchase return list and detail
- purchase return create

Reference:
- [09_returns.md](./09_returns.md)

### 10. Service, Warranty, Agreements

Screens:
- service workspace
- ticket queue and detail
- ticket create
- warranty claim list and detail
- warranty extension
- agreement list and detail
- replacement flow
- ownership lookup

Reference:
- [10_service_warranty_agreements.md](./10_service_warranty_agreements.md)

### 11. Finance and Expenses

Screens:
- finance overview
- chart of accounts
- voucher list and detail
- voucher create
- ledgers and outstanding
- expense list and detail
- expense create and pay
- bank reconciliation
- bank import batch history

Reference:
- [11_finance_expenses.md](./11_finance_expenses.md)

### 12. Subscriptions and Tax

Screens:
- subscription overview
- plan comparison
- subscription history
- tax registration list and form
- GSTIN lookup helper
- GST compliance draft and provider-status panel
- configurable GST provider-backed compliance flow
- GST threshold settings

Reference:
- [12_subscriptions_and_tax.md](./12_subscriptions_and_tax.md)

### 13. Approvals, Workflow, Audit

Screens:
- approval queue
- approval detail
- rule manager
- workflow trigger monitor
- audit log

Reference:
- [13_approvals_workflow_audit.md](./13_approvals_workflow_audit.md)

### 14. Dashboard, Reports, Notifications

Screens:
- dashboard home
- reports landing
- report runner
- report schedule list
- report schedule form
- notification inbox
- notification detail
- template manager

Reference:
- [14_dashboard_reports_notifications.md](./14_dashboard_reports_notifications.md)

## Desktop Design Rules

- prefer table-first layouts for high-volume operations
- keep filters sticky where users repeatedly refine lists
- keep totals sticky on document create screens
- use tabs when one entity has multiple operational subcontexts
- use drawers instead of route changes for lightweight previews

## Mobile Design Rules

- prioritize lookup, summary, approval, and capture workflows
- keep complex accounting and reconciliation desktop-first
- replace dense item tables with expandable cards
- use full-screen pickers for search-heavy selection
- keep primary CTA fixed at bottom for multi-step flows

## Build Order Recommendation For UI Team

1. auth and org onboarding
2. org, branch, warehouse
3. customers and suppliers
4. catalog
5. purchases
6. sales
7. inventory
8. service and warranty
9. finance
10. subscriptions and tax
11. approvals
12. dashboard and notifications
13. platform admin

## Handoff Checklist

Before UI implementation starts on a module:

- read the module doc
- confirm primary role and entry point
- confirm list filters and row actions
- confirm create flow shape
- confirm permission gating
- confirm subscription gating if applicable
- wire APIs from the module appendix
- confirm PDF preview expectations

## Shareable Format Note

This machine does not currently have a local PDF renderer such as `pandoc` or `wkhtmltopdf`.

Until that is installed, use:
- this markdown bundle
- the module markdown docs
- the HTML companion file

HTML companion:
- [15_ui_handoff_bundle.html](./15_ui_handoff_bundle.html)
