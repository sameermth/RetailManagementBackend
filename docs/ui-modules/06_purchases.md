# Purchases UX Design

## Outcomes

- support procurement from planning to payment
- let suppliers collaborate on dispatch and backorder updates before receipt
- make document handling operationally easy
- keep payment allocation understandable
- capture inward-stock pricing snapshots that can later drive invoice pricing and MRP validation
- support receipt putaway so warehouse teams can place inward stock into bins after receiving

## Primary Users

- owner
- admin
- purchase operator
- accountant
- store manager for goods receipt visibility

## Information Architecture

- purchases workspace
- purchase orders
- supplier dispatch updates
- purchase receipts
- receipt putaway
- supplier payments
- payment allocations

## Screen Inventory

### Purchases Workspace

### Purchase Order List

### Purchase Order Detail

Tabs:
- header and supplier
- items
- supplier dispatches
- related receipts
- document preview

### Supplier Dispatch Review

### Supplier Portal Purchase Order View

### Purchase Order Create

### Purchase Receipt List

### Purchase Receipt Detail

Tabs:
- items
- inventory impact
- putaway
- linked payments
- document preview

### Purchase Receipt Create

### Purchase Receipt Putaway

### Supplier Payment List

### Supplier Payment Detail

### Payment Allocation Screen

## List Page Blueprint: Purchase Order List

Columns:
- PO number
- supplier
- branch
- date
- status
- total

Filters:
- supplier
- branch
- status
- date range
- search by PO number

Top actions:
- create PO

Row actions:
- view
- download PDF
- send
- generate supplier access link
- create receipt if eligible

## Detail Page Blueprint: Purchase Receipt Detail

Header summary:
- receipt number
- supplier
- linked PO
- branch
- total
- putaway status

Tab expectations:
- items with qty and tax
- supplier dispatches showing what supplier committed, what is in transit, and what is still pending
- inventory impact showing stock-in summary
- putaway tab showing quantity still waiting to be placed into bins and the bin used per line
- pricing snapshot showing unit cost, suggested sale price, and MRP when captured
- linked payments showing allocated and outstanding amount
- PDF preview or download panel

## Create Flow Blueprint

Purchase order create:

1. supplier and branch
2. items
3. charges, tax, totals
4. notes
5. review

Supplier dispatch review:

1. supplier link generated from PO detail
2. supplier submits one or more dispatch notices
3. internal team reviews dispatched qty, expected delivery, and remaining ETA
4. receipt is created only after actual inward

Purchase receipt create:

1. source PO optional
2. supplier and branch
3. received items
4. stock, pricing snapshot, and finance summary
5. review

Purchase receipt putaway:

1. open receipt detail
2. review remaining putaway quantity per line
3. choose bin
4. enter putaway quantity
5. review remaining unplaced quantity
6. save

Supplier payment create:

1. supplier
2. payment details
3. allocation optional
4. review

## State Handling

- no PO yet
- no receipts yet
- no payments yet
- supplier has not responded yet
- partially dispatched order
- backordered remainder with ETA
- partially received order
- putaway pending
- partially put away
- putaway completed
- partially allocated payment

## CTA Map

Workspace:
- primary: New purchase order
- secondary: New receipt
- tertiary: Record supplier payment

PO detail:
- primary: Create receipt
- secondary: Generate supplier link
- tertiary: PDF

Receipt detail:
- primary: Put away stock
- secondary: PDF
- tertiary: Open inventory impact

Supplier dispatch review:
- primary: Create receipt from arrived goods
- secondary: Open supplier portal copy link
- tertiary: Review dispatch history

Payment detail:
- primary: Allocate payment
- secondary: PDF

## Desktop and Mobile Notes

Desktop:
- item-heavy flows should use wide editable tables

Mobile:
- use accordion item editors instead of dense rows
- payment allocation should be a focused full-screen step

## Cross-Module Navigation

- PO to supplier detail
- PO to supplier dispatch review
- receipt to inventory movements
- payment to finance summaries later

## Key Forms And Backend Fields

### Purchase Order Create

- `organizationId`
- `branchId`
- `supplierId`
- `poDate`
- `placeOfSupplyStateCode`
- `remarks`
- `lines[]`

PO line fields:
- `productId`
- `supplierProductId`
- `uomId`
- `quantity`
- `baseQuantity`
- `unitPrice`
- `taxRate`

### Supplier Dispatch Notice Create

Supplier-facing fields:
- `dispatchDate`
- `expectedDeliveryDate`
- `supplierReferenceNumber`
- `transporterName`
- `vehicleNumber`
- `trackingNumber`
- `remarks`
- `lines[]`

Supplier dispatch line fields:
- `purchaseOrderLineId`
- `quantity`
- `baseQuantity`
- `expectedRemainingDispatchOn`
- `remarks`

Supplier dispatch note:
- supplier can submit full or partial dispatch
- backend validates remaining open quantity per PO line
- supplier dispatch does not post stock
- supplier dispatch is a commitment and ETA layer that helps backorder and fulfillment promises

### Purchase Receipt Create

- `organizationId`
- `branchId`
- `warehouseId`
- `purchaseOrderId`
- `supplierId`
- `receiptDate`
- `dueDate`
- `placeOfSupplyStateCode`
- `remarks`
- `lines[]`

Receipt line fields:
- `purchaseOrderLineId`
- `productId`
- `supplierProductId`
- `uomId`
- `quantity`
- `baseQuantity`
- `unitCost`
- `suggestedSalePrice`
- `mrp`
- `taxRate`
- `serialNumbers[]`
- `batchEntries[]`

Receipt pricing note:
- receipt-level `suggestedSalePrice` and `mrp` should be captured whenever known
- these values become the commercial snapshot for inward stock
- old and new inward stock can therefore coexist with different selling defaults and MRP

### Purchase Receipt Putaway

- `lines[]`
- per line:
  - `purchaseReceiptLineId`
  - `binLocationId`
  - `quantity`
  - `baseQuantity`

### Supplier Payment Create

- `organizationId`
- `branchId`
- `supplierId`
- `paymentDate`
- `paymentMethod`
- `referenceNumber`
- `amount`
- `remarks`

### Supplier Payment Allocation

- `allocations[]`
- per allocation:
  - `purchaseReceiptId`
  - `allocatedAmount`

## Screen To API Map

- Purchase order list:
  - `GET /api/erp/purchases/orders?organizationId=...`
- Purchase order detail:
  - `GET /api/erp/purchases/orders/{id}`
- Purchase order supplier link:
  - `POST /api/erp/purchases/orders/{id}/supplier-access-link`
- Purchase order supplier dispatch list:
  - `GET /api/erp/purchases/orders/{id}/supplier-dispatches`
- Purchase order create:
  - `POST /api/erp/purchases/orders`
- Purchase order PDF and send:
  - `GET /api/erp/purchases/orders/{id}/pdf`
  - `POST /api/erp/purchases/orders/{id}/send`
- Purchase receipt list:
  - `GET /api/erp/purchases/receipts?organizationId=...`
- Purchase receipt detail:
  - `GET /api/erp/purchases/receipts/{id}`
- Purchase receipt create:
  - `POST /api/erp/purchases/receipts`
- Purchase receipt putaway:
  - `POST /api/erp/purchases/receipts/{id}/putaway`
- Purchase receipt PDF and send:
  - `GET /api/erp/purchases/receipts/{id}/pdf`
  - `POST /api/erp/purchases/receipts/{id}/send`
- Supplier payment list:
  - `GET /api/erp/purchases/supplier-payments?organizationId=...`
- Supplier payment create:
  - `POST /api/erp/purchases/supplier-payments`

### Supplier Portal Screen To API Map

- Supplier portal PO view:
  - `GET /api/supplier-portal/purchase-orders/{accessToken}`
- Supplier dispatch submit:
  - `POST /api/supplier-portal/purchase-orders/{accessToken}/dispatch-notices`

## UX Notes For UI Team

- purchase receipt should not be exposed to suppliers
- supplier portal should be token-link based and not require app login initially
- PO detail should show:
  - total ordered base quantity
  - total supplier-dispatched base quantity
  - pending base quantity
  - latest expected delivery date
  - latest expected remaining dispatch date
- when supplier partially dispatches, UI should keep the PO open and highlight remaining quantity plus ETA instead of treating it as fully received
- receipt detail should surface `putawayStatus` prominently because receiving and physical storage are separate operational steps now
- putaway should use warehouse bin pickers, not free-text shelf fields
- Supplier payment allocate:
  - `POST /api/erp/purchases/supplier-payments/{id}/allocate`
- Supplier payment PDF and send:
  - `GET /api/erp/purchases/supplier-payments/{id}/pdf`
  - `POST /api/erp/purchases/supplier-payments/{id}/send`
