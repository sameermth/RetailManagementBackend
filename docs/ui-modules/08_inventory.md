# Inventory UX Design

## Outcomes

- make stock position understandable at organization, branch, warehouse, bin, and serial level
- help operators move between overview, adjustment, transfer, and reservation workflows quickly
- reduce accidental stock mistakes by showing before-and-after impact clearly
- connect inventory screens cleanly with purchase, sales, service, and warranty flows

## Primary Users

- owner
- admin
- store manager
- purchase operator
- inventory operator
- technician for serial lookup

## Information Architecture

Primary navigation under `Inventory`:
- overview
- bins
- balances
- product stock
- warehouse stock
- replenishment planning
- adjustments
- transfers
- reservations
- serials and batches
- movements

Recommended entry points from other modules:
- purchase receipt detail to stock impact
- purchase receipt detail to putaway
- invoice detail to stock deduction
- service ticket to serial lookup
- order-to-invoice conversion to serial and batch selection screens

## Screen Inventory

### Inventory Overview

Purpose:
- daily stock command center

Widgets:
- stock value
- low stock items
- out of stock items
- expiring batches if supported
- open reservations
- pending transfers
- recent adjustments

Quick actions:
- adjust stock
- create transfer
- open low stock view
- search serial or batch

### Balance Explorer

Purpose:
- one entry screen where user can pivot between product-wise and warehouse-wise balances

Recommended view switch:
- by product
- by warehouse
- by bin

### Bin Location List

Purpose:
- maintain shelf, rack, and zone structure inside each warehouse

### Bin Stock Detail

Purpose:
- inspect what is stored in one physical bin and what recent movements changed it

### Product Stock Detail

Recommended tabs:
- summary
- warehouse balances
- serials
- batches
- reservations
- movements

### Warehouse Stock Detail

Recommended tabs:
- stock list
- bins
- low stock
- replenishment
- reservations
- movement history

### Replenishment Planning

Purpose:
- show low-stock products per warehouse
- suggest internal transfer first when another warehouse has excess
- fall back to supplier purchase recommendation when transfer is not suitable

Important backend rule:
- planning transfer suggestion currently applies only to non-serialized, non-batch products
- tracked items still appear in planning, but UI should show purchase recommendation or no shortcut rather than transfer CTA

Recommended table columns:
- product
- warehouse
- available qty
- min stock
- reorder level
- recommended replenishment qty
- transfer suggestion
- purchase suggestion
- open PO exists

Top actions:
- create transfer from suggestion
- create draft PO from suggestion
- open product stock detail

### Manual Adjustment

Purpose:
- guarded stock correction workflow

Important backend rule:
- adjustment can now target a warehouse bin
- if bin is selected, resulting inventory balance and movement are stored against that bin

### Stock Transfer

Purpose:
- transfer stock between warehouses or branches

Important backend rule:
- transfer lines can optionally include source and destination bins
- use this for shelf-to-shelf placement or destination putaway in addition to warehouse transfer

### Reservation Queue

Purpose:
- monitor and release reserved stock

### Serial and Batch Search

Purpose:
- fast lookup screen for service, warranty, and operations teams

### Movement History

Purpose:
- audit-like operational view of stock changes

### Receipt Putaway Support

Purpose:
- help warehouse teams place newly received stock into bins after the purchase receipt is already posted

Important backend rule:
- purchase receipt putaway creates bin-aware movements after receipt creation
- this keeps commercial receiving and physical storage as two separate steps

## List Page Blueprint

### Balance Explorer

Recommended default columns in product view:
- product name
- SKU
- category
- available qty
- reserved qty
- on hand qty
- average cost
- low stock flag

Filters:
- warehouse
- bin
- branch
- category
- product search
- low stock only
- serial tracked only

Top actions:
- adjust stock
- transfer stock

Row actions:
- open product stock detail
- open movement history
- open serials or batches

### Reservation List

Columns:
- reservation reference
- source document type
- source document number
- product
- warehouse
- reserved qty
- created at
- expires at
- status

Filters:
- warehouse
- status
- expiry soon
- product search

Top actions:
- release expired eligible reservations

Row actions:
- view
- release if allowed

### Movement History

Columns:
- movement date and time
- product
- warehouse
- movement type
- quantity
- reference type
- reference number
- performed by

Filters:
- product
- warehouse
- movement type
- date range
- reference search

## Detail Page Blueprint

### Product Stock Detail

Header summary:
- product name
- SKU
- inventory tracking mode
- total available
- total reserved
- total on hand

Key cards:
- stock summary
- low stock threshold
- open reservations
- linked serial or batch count

Warehouse balances table:
- warehouse
- bin optional
- available qty
- reserved qty
- on hand qty
- reorder level

Serial tab should show:
- serial number
- status
- current warehouse or ownership state
- linked invoice or ticket if present

Batch tab should show:
- batch number
- manufacturing date
- expiry date
- suggested sale price
- MRP
- available qty
- status

Movement tab should show:
- date
- movement type
- quantity
- bin when present
- reference
- actor

Actions:
- adjust stock
- transfer stock
- open reservations

Stock behavior note:
- one sales document can consume standard, batched, and serialized items together
- standard lines reserve warehouse balance
- batch lines reserve the selected batch balance
- serialized lines reserve the selected serial and then move that serial to sold state when invoice posts

### Warehouse Stock Detail

Header summary:
- warehouse
- branch
- total stock value
- low stock item count

Sections:
- current stock table
- bin summary
- low stock list
- reservation summary
- recent movements

Actions:
- create transfer
- adjust stock
- view movement history

### Serial Detail

Header summary:
- serial number
- product
- current status
- current location

Important sections:
- ownership status if sold
- warranty summary
- movement timeline
- linked service tickets

## Create or Edit Flow Blueprint

### Manual Adjustment

Step 1 location:
- organization
- branch
- warehouse
- bin optional

Step 2 item:
- product picker
- batch or serial picker if applicable

Step 3 adjustment:
- adjustment type
- quantity
- reason
- remarks

Step 4 confirmation:
- before quantity
- after quantity
- warning if negative or unusual

### Stock Transfer

Step 1 source and destination:
- from branch or warehouse
- to branch or warehouse
- source bin optional
- destination bin optional

Step 2 items:
- product picker
- quantity
- serials or batches if required

Step 3 review:
- availability check
- transfer summary

Step 4 create:
- create transfer and show tracking state

### Reservation Release

Single-item release:
- show source document, reserved qty, and consequence

Bulk release:
- filter eligible reservations first
- show confirmation count before release

## State Handling

Overview:
- no stock yet
- low stock alert state
- negative stock exception state if backend permits detection

Balances:
- no results for current filters
- warehouse unavailable

Adjustments:
- serial required but not selected
- batch expired warning
- quantity exceeds available for certain adjustment types

Transfers:
- insufficient stock
- destination same as source
- serial conflict
- tracked product transfer not supported by planning shortcut if product is serial or batch tracked

Putaway:
- receipt line already fully put away
- bin belongs to another warehouse
- entered putaway quantity exceeds remaining receipt quantity

Reservations:
- expired
- active
- released
- source document closed warning

Permission handling:
- disable adjustment and transfer buttons if user only has view access

## CTA Map

Inventory overview:
- primary: Adjust stock
- secondary: Transfer stock
- tertiary: Search serial

Balance explorer:
- primary: Adjust stock
- row primary: View stock detail
- row secondary: Movements

Product stock detail:
- primary: Transfer stock
- secondary: Adjust stock
- tertiary: Open reservations

Warehouse stock detail:
- primary: Transfer stock
- secondary: Adjust stock

Replenishment planning:
- primary: Create transfer
- secondary: Create draft PO
- row tertiary: View stock detail

Reservation list:
- primary: Release eligible
- row primary: View
- row secondary: Release

## Desktop and Mobile Notes

Desktop:
- stock tables need dense, spreadsheet-like layout
- use sticky filters and sticky summary rail

Mobile:
- prioritize serial search, overview cards, and product detail summary
- heavy stock tables should become stacked cards with expandable quantities

Tablet:
- useful for warehouse floor operations
- keep transfer and adjustment flows optimized for touch input

## Cross-Module Navigation

- purchase receipt detail to stock detail
- invoice detail to stock movement
- product detail to inventory detail
- serial detail to service ticket and warranty
- reservation row to source sales document
- batch detail to source purchase receipt and sales invoice
- receipt detail to bin movement trail

## Key Forms And Backend Fields

### Manual Stock Adjustment

- `organizationId`
- `branchId`
- `warehouseId`
- `binLocationId` optional
- `productId`
- `uomId`
- `quantityDelta`
- `baseQuantityDelta`
- `unitCost`
- `reason`

### Stock Transfer

- `organizationId`
- `branchId`
- `fromWarehouseId`
- `toWarehouseId`
- `lines[]`

Transfer line fields:
- `productId`
- `uomId`
- `fromBinLocationId` optional
- `toBinLocationId` optional
- `quantity`
- `baseQuantity`

### Replenishment Planning

- `organizationId`
- `branchId` optional filter
- `warehouseId` optional filter
- `actionableOnly`

Create draft PO:
- `organizationId`
- `branchId` optional
- `warehouseId` optional
- `storeProductId`
- `supplierId` optional override
- `quantity` optional override
- `remarks`

Create transfer:
- `organizationId`
- `branchId` optional
- `storeProductId`
- `sourceWarehouseId`
- `targetWarehouseId`
- `quantity` optional override
- `remarks`

### Reservation Release

- `releaseReason`

## Screen To API Map

- Bin locations:
  - `GET /api/erp/inventory-bins?organizationId=...&warehouseId=...&activeOnly=true`
  - `POST /api/erp/inventory-bins`
  - `PUT /api/erp/inventory-bins/{id}`
- Warehouse balances:
  - `GET /api/erp/inventory-balances/warehouse/{warehouseId}?organizationId=...`
- Product balances:
  - `GET /api/erp/inventory-balances/product/{productId}?organizationId=...`
- Bin balances:
  - `GET /api/erp/inventory-balances/bin/{binLocationId}?organizationId=...`
- Product batches:
  - `GET /api/erp/inventory-tracking/batches/product/{productId}?organizationId=...`
- Product serials:
  - `GET /api/erp/inventory-tracking/serials/product/{productId}?organizationId=...`
- Bin movements:
  - `GET /api/erp/stock-movements/bin/{binLocationId}?organizationId=...`
- Reservations:
  - `GET /api/erp/inventory-reservations?organizationId=...`
  - `POST /api/erp/inventory-reservations/expire`
  - `POST /api/erp/inventory-reservations/{id}/release`
- Manual adjustment:
  - `POST /api/erp/inventory-operations/adjustments/manual`
- Stock transfer:
  - `POST /api/erp/inventory-operations/transfers`
- Replenishment planning:
  - `GET /api/erp/inventory-planning/replenishment?organizationId=...&branchId=...&warehouseId=...&actionableOnly=true`
  - `POST /api/erp/inventory-planning/replenishment/draft-purchase-orders`
  - `POST /api/erp/inventory-planning/replenishment/transfers`
- Receipt putaway impact:
  - surfaced through `POST /api/erp/purchases/receipts/{id}/putaway`
