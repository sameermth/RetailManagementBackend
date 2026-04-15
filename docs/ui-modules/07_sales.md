# Sales UX Design

## Outcomes

- help sales staff move cleanly from quote to order to invoice to receipt
- make source-document relationships obvious so users do not lose business context
- support partial payment handling without confusing the invoice screen
- make tax breakup, PDF preview, and customer communication first-class in the sales journey
- make post-invoice dispatch and delivery progress visible without sending users into inventory screens
- make pick and pack progress visible before dispatch so warehouse handling is not hidden
- keep price, MRP, and tax derived by backend so UI only captures supported commercial inputs

## Primary Users

- owner
- admin
- cashier
- accountant
- store manager
- service desk staff for invoice lookup later

## Information Architecture

Primary navigation under `Sales`:
- workspace
- pos
- quotes
- orders
- invoices
- payment requests
- dispatches
- customer receipts

Recommended cross-links:
- customer detail should expose sales summary
- invoice detail should open related receipts
- invoice detail should expose payment-request summary and payment-request history
- invoice detail should expose dispatch summary and dispatch history
- quote and order detail should show downstream conversions

Core backend behavior to reflect in UX:
- quote and order lines price from store product defaults
- invoice lines price from stock context when stock context exists
- only line discount is editable in normal sales flows
- POS lookup returns both `storeProductId` and shared `productId`, but POS checkout must submit `storeProductId`
- batch and serialized items require tracked selections at invoice time
- quote validity and order fulfillment windows are enforced by backend
- dispatch is a post-invoice fulfillment document and does not create a second stock deduction
- dispatch now supports explicit pick -> pack -> dispatch -> deliver stages
- payment requests are invoice-linked collection prompts; they do not post accounting by themselves
- payment requests now sit behind a provider abstraction, so UI can choose a gateway provider without changing invoice flow later
- POS can create or reuse a walk-in customer automatically when cashier does not choose a named customer

## Screen Inventory

### Sales Workspace

Purpose:
- a daily command center for commercial activity

Widgets:
- draft or active quotes
- open orders
- pending invoices
- pending dispatch invoices
- invoices with open payment requests
- partially paid invoices
- receipts collected today
- overdue receivables

Quick actions:
- create quote
- create order
- create invoice
- create payment request
- create dispatch from invoice
- pick dispatch
- pack dispatch
- record receipt

Recommended lower sections:
- recent documents
- payment follow-up queue
- recent cancellations or exceptions

### POS Counter Workspace

Purpose:
- give cashier or owner a fast counter-selling surface without going through the full invoice form

Recommended layout:
- session banner with terminal, warehouse, open time, and opening cash
- left side item lookup or scanner field
- center basket with quantity, discount, and tracked-item selection state
- right side summary with customer, payment, and totals

Important UI rules:
- open or resume one POS session before checkout
- lookup should search by SKU, barcode, serial, or batch text
- use `storeProductId` returned by lookup when building checkout lines
- show tracked-item state inline:
  - serialized item: selected serial
  - batched item: selected batch and quantity
  - standard item: no tracked selector unless backend later recommends lot split
- allow `Walk-In Customer` as the default customer path

Suggested actions:
- open session
- resume active session
- add item by scan
- void basket
- checkout and collect payment
- close session

### Quote List

Purpose:
- manage pre-sales pipeline and document generation

### Quote Detail

Recommended tabs:
- summary
- items and totals
- source and conversion history
- document preview
- activity

### Quote Create or Edit

Use a guided, sectioned form rather than a flat page.

### Order List

Purpose:
- operational follow-up of customer commitments

### Order Detail

Recommended tabs:
- summary
- items
- invoice links
- document preview
- activity

### Order Create or Edit

Should feel similar to quote creation with stronger fulfillment context.

### Invoice List

Purpose:
- accounting-facing and cashier-facing working queue

### Invoice Detail

Recommended tabs:
- summary
- items and GST
- payment requests
- dispatches
- payments and receipts
- source references
- document preview
- activity

### Invoice Create

Should support three clear entry modes:
- direct invoice
- from quote
- from order

### POS Session Detail

Recommended tabs:
- session summary
- invoices
- receipts
- cash summary
- close session

### Dispatch List

Purpose:
- manage packed, dispatched, and delivered sales fulfillment documents

### Dispatch Detail

Recommended tabs:
- summary
- dispatched items
- transport and tracking
- linked invoice
- document preview
- activity

### Customer Receipt List

Purpose:
- show all incoming payments and their allocation state

### Customer Receipt Detail

Recommended tabs:
- summary
- allocations
- source invoices
- document preview
- activity

### Receipt Create or Allocation Screen

Should support:
- immediate allocation during receipt creation
- allocation later from receipt detail

## List Page Blueprint

### Quote List

Columns:
- quote number
- customer
- branch
- quote date
- validity date
- status
- total amount
- created by

Filters:
- branch
- customer
- status
- date range
- search by quote number or customer

Top actions:
- create quote
- export list later

Row actions:
- view
- edit if editable
- cancel
- preview PDF
- convert to order
- convert to invoice

### Order List

Columns:
- order number
- customer
- branch
- order date
- status
- total amount
- invoiced amount
- pending amount

Filters:
- branch
- customer
- status
- date range
- search by order number

Top actions:
- create order

Row actions:
- view
- edit if allowed
- cancel
- preview PDF
- convert to invoice

### Invoice List

Columns:
- invoice number
- customer
- branch
- invoice date
- payment status
- dispatch status
- total amount
- paid amount
- outstanding amount
- source type

Filters:
- branch
- customer
- payment status
- date range
- overdue only
- search by invoice number or customer

Top actions:
- create invoice
- record receipt

Row actions:
- view
- preview PDF
- open dispatches
- open receipts
- record payment
- cancel if backend allows

### Dispatch List

Columns:
- dispatch number
- invoice number
- dispatch date
- expected delivery date
- status
- transporter
- tracking number

Filters:
- branch
- dispatch status
- dispatch date range
- search by dispatch number, invoice number, or tracking number

Top actions:
- create dispatch from invoice detail

Row actions:
- view
- mark dispatched
- mark delivered
- preview PDF

### Customer Receipt List

Columns:
- receipt number
- customer
- date
- mode
- total received
- allocated amount
- unallocated amount
- status

Filters:
- customer
- date range
- allocation status
- payment mode
- search by receipt number

Top actions:
- record receipt

Row actions:
- view
- allocate
- preview PDF

## Detail Page Blueprint

### Quote Detail

Header summary:
- quote number
- customer
- branch
- status
- total
- validity
- seller GST and place of supply

Key cards:
- commercial summary
- GST summary
- conversion status

Items section:
- line number
- item name
- quantity
- unit price
- discount
- taxable amount
- CGST
- SGST
- IGST
- total

Sidebar or top rail actions:
- edit
- cancel
- convert to order
- convert to invoice
- preview PDF

### Order Detail

Header summary:
- order number
- customer
- branch
- status
- total
- expected fulfillment by

Important sections:
- order items
- source quote if any
- linked invoices
- order notes

Actions:
- edit
- cancel
- create invoice
- preview PDF

### Invoice Detail

This should be one of the richest screens in the system.

Header summary:
- invoice number
- customer
- branch
- invoice date
- payment status
- total
- outstanding

Top cards:
- amount summary
- GST breakup summary
- source summary
- payment summary
- dispatch summary
- compliance summary for GST draft readiness

Items table should show:
- item name
- quantity
- unit price
- discount
- taxable amount
- CGST amount
- SGST amount
- IGST amount
- line total

Totals block should clearly show:
- subtotal before tax
- CGST total
- SGST total
- IGST total
- cess if applicable

Compliance panel should show:
- e-invoice draft status
- e-way bill draft status later when generated
- latest warnings from tax module
- submit and sync actions when finance users have tax permissions
- acknowledgement number and provider timestamps when available
- grand total
- total paid
- outstanding

Related section:
- quote reference if generated from quote
- order reference if generated from order
- payment request history table
- dispatch history table
- related receipts table

Actions:
- preview PDF
- download PDF
- create payment request
- create dispatch
- open dispatch history
- record receipt
- open related customer
- open related receipts

Dispatch summary card should show:
- `NOT_DISPATCHED`
- `PARTIALLY_DISPATCHED`
- `DISPATCHED`
- `DELIVERED`
- dispatch count
- dispatched quantity
- pending quantity
- last dispatch date

### Customer Receipt Detail

Header summary:
- receipt number
- customer
- date
- mode
- total
- allocated
- unallocated

Key sections:
- payment details
- allocations table
- linked invoices table
- remarks and attachments later

Actions:
- allocate amount
- re-open invoice link
- preview PDF

### Dispatch Detail

Header summary:
- dispatch number
- linked invoice
- dispatch date
- expected delivery date
- status
- transporter
- tracking number

Key sections:
- shipment summary
- dispatched items
- pick and pack progress
- linked invoice summary
- remarks and proof-of-delivery later

Actions:
- pick
- pack
- mark dispatched
- mark delivered
- preview PDF

## Create or Edit Flow Blueprint

### Quote Create

Step 1 customer and branch:
- customer picker
- branch picker
- quote date
- validity date
- remarks

Step 2 items:
- searchable product or service picker
- quantity
- read-only unit price
- editable line discount
- read-only tax preview

Quote rule:
- UI should not expose editable unit price or editable tax on quote lines
- quote pricing should be shown as derived from store-product defaults and tax setup
- if a selected item is a bundle, backend currently expands it into component lines and UI should visually group those lines using the returned remark

Step 3 totals and notes:
- subtotal
- GST breakup
- round off if allowed
- terms and notes

Step 4 review:
- preview summary
- save draft or finalize

### Order Create

Entry choices:
- create from quote
- create manually

If from quote:
- preload customer and items
- allow controlled edits before save

If manual:
- follow quote-like flow with order-specific status and notes

Order rule:
- UI should show and allow editing `expectedFulfillmentBy`
- order pricing follows the same derived-price model as quote
- expired orders must appear blocked for invoice conversion

### Invoice Create

Entry choices:
- direct invoice
- from quote
- from order

Step 1 source selection:
- source picker or direct mode toggle

Step 2 customer, branch, invoice meta:
- customer
- branch
- invoice date
- due date if applicable

Step 3 items:
- direct entry or preloaded lines
- standard items can be invoiced without tracked selection
- batch-tracked items must collect batch selection before submit
- serialized items must collect serial selection before submit
- unit price, MRP, and tax are read-only derived values
- discount remains editable per line

Step 4 totals:
- subtotal
- GST breakup
- additional charges or discount if backend supports

Step 5 payment option:
- no payment yet
- create payment request
- record immediate payment

Step 6 review and create:
- final summary
- create invoice
- optionally open receipt flow

Invoice rule:
- invoice is the stock-aware document
- mixed documents are supported, including standard, batched, and serialized lines in one invoice
- for quote or order conversion, `trackedLines[]` only needs entries for tracked products
- if stock spans inward lots with different MRP or suggested selling prices, UI should guide the user to split the item into separate lines

### Dispatch Create

Entry point:
- from invoice detail after invoice is posted

Step 1 shipment meta:
- dispatch date
- expected delivery date
- transporter name
- transporter id if available
- vehicle number
- tracking number
- delivery address
- remarks

Step 2 items:
- show invoice lines with already-dispatched quantity and remaining quantity
- allow selecting only the remaining dispatchable quantity per invoice line
- keep quantity read-only derived from base-quantity conversion if you want to avoid mismatch errors

Step 3 review:
- linked invoice summary
- dispatch quantities
- transport details
- create dispatch

Dispatch rules:
- dispatch is only available for posted or financially active invoices
- dispatch does not create a second stock deduction
- partial pick and partial pack are supported
- partial dispatch is supported
- once all invoice quantity is covered by active dispatches, invoice summary should show `DISPATCHED`
- once all active dispatches are delivered, invoice summary should show `DELIVERED`

### Customer Receipt Create

Step 1 customer and payment:
- customer picker
- receipt date
- payment mode
- reference number
- amount received

Step 2 allocation:
- show all open invoices for same customer
- allow full or partial allocation
- keep unallocated amount visible live

Step 3 review:
- payment summary
- invoice allocation summary
- save receipt

## State Handling

Quotes:
- no quotes yet
- no search results
- quote already converted warning
- quote cancelled state

Orders:
- order not invoiced yet
- order partially invoiced
- order cancelled

Invoices:
- unpaid
- partially paid
- fully paid
- overpaid or unallocated amount warning if ever supported
- not dispatched
- pick pending
- partially picked
- picked
- partially packed
- partially dispatched
- dispatched
- delivered

Receipts:
- fully allocated
- partially allocated
- unallocated

Payment requests:
- requested
- partially paid
- paid
- expired
- cancelled

Error and validation states:
- source document no longer eligible for conversion
- customer missing mandatory fields
- line item stock or validity issue from backend
- tax group mismatch warning
- quote expired and cannot convert
- order expired and cannot convert
- batch selection required for tracked line
- serial selection required for tracked line
- mixed lot pricing conflict requiring separate lines

Permission and subscription states:
- disable cancel or edit if user lacks access
- disable premium workflows with upgrade hint where applicable

## CTA Map

Sales workspace:
- primary: Create invoice
- secondary: Create quote
- tertiary: Record receipt

Quote list:
- primary: Create quote
- row primary: View
- row secondary: Convert

Order list:
- primary: Create order
- row primary: View
- row secondary: Create invoice

Invoice list:
- primary: Create invoice
- secondary: Record receipt
- tertiary: Create payment request
- row primary: View
- row secondary: Open receipts

Receipt list:
- primary: Record receipt
- row primary: View
- row secondary: Allocate

Dispatch list:
- primary: Create dispatch from invoice detail
- row primary: View
- row secondary: Update status

## Desktop and Mobile Notes

Desktop:
- use wide line-item tables for quote, order, and invoice detail
- keep totals and actions sticky on create screens
- use side drawer for customer quick view where possible

Mobile:
- collapse line items into expandable cards
- make amount summary sticky near bottom for create flows
- keep invoice detail action bar fixed with `PDF`, `Receipt`, and `Customer`

Tablet:
- a very good fit for cashier and owner workflows
- support two-column detail layout where possible

## Cross-Module Navigation

- quote detail to customer detail
- order detail to originating quote
- invoice detail to source quote or order
- invoice detail to dispatch list
- invoice detail to receipt list
- invoice line to product detail
- receipt detail to invoice detail
- customer detail to invoices, receipts, and service history later

## Key Forms And Backend Fields

### Quote Create

- `organizationId`
- `branchId`
- `warehouseId`
- `customerId`
- `quoteType`
- `quoteDate`
- `validUntil`
- `placeOfSupplyStateCode`
- `remarks`
- `lines[]`

Quote line fields:
- `productId`
- `uomId`
- `quantity`
- `baseQuantity`
- `discountAmount`
- `remarks`

Backend-derived quote response fields:
- `unitPrice`
- `mrp`
- `taxableAmount`
- `taxRate`
- `cgstRate`
- `cgstAmount`
- `sgstRate`
- `sgstAmount`
- `igstRate`
- `igstAmount`
- `cessRate`
- `cessAmount`
- `lineAmount`

### Order Create

- `organizationId`
- `branchId`
- `warehouseId`
- `customerId`
- `orderDate`
- `expectedFulfillmentBy`
- `placeOfSupplyStateCode`
- `remarks`
- `lines[]`

Order line request fields:
- `productId`
- `uomId`
- `quantity`
- `baseQuantity`
- `discountAmount`
- `remarks`

### Invoice Create

- `organizationId`
- `branchId`
- `warehouseId`
- `customerId`
- `priceListId`
- `invoiceDate`
- `dueDate`
- `placeOfSupplyStateCode`
- `remarks`
- `lines[]`

Invoice line fields:
- `productId`
- `uomId`
- `quantity`
- `baseQuantity`
- `discountAmount`
- `serialNumberIds[]`
- `batchSelections[]`
- `warrantyMonths`

Backend-derived invoice response fields:
- `unitPrice`
- `mrp`
- `taxableAmount`
- `taxRate`
- `cgstRate`
- `cgstAmount`
- `sgstRate`
- `sgstAmount`
- `igstRate`
- `igstAmount`
- `cessRate`
- `cessAmount`
- `lineAmount`

### POS Session Open

- `organizationId`
- `branchId`
- `warehouseId`
- `terminalName`
- `openingCashAmount`
- `openingNotes`

POS session response fields UI should use:
- `id`
- `sessionNumber`
- `terminalName`
- `status`
- `openedAt`
- `openingCashAmount`
- `invoiceCount`
- `receiptCount`
- `grossSalesAmount`
- `totalCollectedAmount`
- `expectedClosingCashAmount`

### POS Catalog Lookup

- `query`
- optional `customerId`

POS lookup response fields UI should use:
- `storeProductId`
- `productId`
- `sku`
- `name`
- `baseUomId`
- `inventoryTrackingMode`
- `matchedBy`
- `matchedSerialId`
- `matchedSerialNumber`
- `matchedBatchId`
- `matchedBatchNumber`
- `unitPrice`
- `mrp`
- `pricingSource`
- `pricingWarning`
- `availableBaseQuantity`
- `lotSelectionRecommended`
- `lots[]`

Lookup rule:
- use `storeProductId` from lookup when building POS checkout payload

### POS Catalog Search

- `query`
- optional `customerId`
- optional `limit`

POS search response fields UI should use:
- `storeProductId`
- `productId`
- `sku`
- `name`
- `baseUomId`
- `inventoryTrackingMode`
- `serialTrackingEnabled`
- `batchTrackingEnabled`
- `serviceItem`
- `unitPrice`
- `mrp`
- `availableBaseQuantity`
- `pricingSource`
- `exactSkuMatch`
- `exactNameMatch`

Search rules:
- use this endpoint for cashier type-ahead and quick keyboard search
- stocked items with zero available quantity are excluded from results
- service items can still appear even without warehouse stock
- for tracked items or mixed-lot pricing, call POS lookup after selection before checkout

### POS Checkout

- optional `customerId`
- optional `useWalkInCustomer`
- `invoiceDate`
- `remarks`
- `lines[]`
- optional `payment`

POS checkout line fields:
- `storeProductId`
- `uomId`
- `quantity`
- `baseQuantity`
- `discountAmount`
- `serialNumberIds[]`
- `batchSelections[]`
- `warrantyMonths`

POS payment fields:
- `paymentMethod`
- optional `amount`
- `referenceNumber`
- `remarks`
- `autoAllocate`

POS checkout response fields UI should use:
- `session.id`
- `customerCode`
- `customerName`
- `invoice.id`
- `invoice.invoiceNumber`
- `invoice.status`
- `invoice.totalAmount`
- `receipt.id`
- `receipt.receiptNumber`
- `receipt.status`
- `receipt.amount`

### POS Session Close

- `organizationId`
- `branchId`
- optional `countedClosingCashAmount`
- `closingNotes`

### Quote Or Order Conversion

- `organizationId`
- `branchId`
- `targetDate`
- `remarks`
- `trackedLines[]`

Tracked line fields:
- `productId`
- `serialNumberIds[]`
- `batchSelections[]`

Tracked conversion note:
- quote-to-order does not need tracked selections
- quote-to-invoice and order-to-invoice can send `trackedLines[]`
- standard products should be omitted from `trackedLines[]`
- batched and serialized products should each have one tracked entry in the conversion request

### Cancel Quote Or Order

- `organizationId`
- `branchId`
- `reason`

### Customer Receipt Create

- `organizationId`
- `branchId`
- `customerId`
- `receiptDate`
- `paymentMethod`
- `referenceNumber`
- `amount`
- `remarks`

### Payment Request Create

- `organizationId`
- `branchId`
- `requestDate`
- `dueDate`
- `expiresOn`
- `requestedAmount`
- `channel`
- `remarks`

Payment-request response fields UI should use:
- `requestNumber`
- `requestedAmount`
- `invoiceAllocatedAmount`
- `invoiceOutstandingAmount`
- `providerCode`
- `providerName`
- `providerReference`
- `providerStatus`
- `paymentLinkToken`
- `paymentLinkUrl`
- `status`
- `providerCreatedAt`
- `providerLastSyncedAt`
- `providerPayload`
- `expiresOn`

Invoice response fields UI should use:
- `paymentRequestSummary.status`
- `paymentRequestSummary.activeRequestCount`
- `paymentRequestSummary.lastRequestedOn`
- `paymentRequestSummary.lastExpiresOn`
- `paymentRequestSummary.latestPaymentLinkUrl`
- `paymentRequestSummary.latestProviderCode`
- `paymentRequestSummary.latestProviderStatus`

### Receipt Allocation

- `allocations[]`
- per allocation:
  - `salesInvoiceId`
  - `allocatedAmount`

### Dispatch Create

- `organizationId`
- `branchId`
- `dispatchDate`
- `expectedDeliveryDate`
- `transporterName`
- `transporterId`
- `vehicleNumber`
- `trackingNumber`
- `deliveryAddress`
- `remarks`
- `lines[]`

Dispatch line fields:
- `salesInvoiceLineId`
- `quantity`
- `baseQuantity`
- `remarks`

Dispatch response fields UI should use:
- `dispatchNumber`
- `status`
- `pickedAt`
- `packedAt`
- `dispatchedAt`
- `deliveredAt`
- `cancelledAt`
- `invoiceNumber`
- `lines[].pickedQuantity`
- `lines[].pickedBaseQuantity`
- `lines[].pickedBinLocationId`
- `lines[].packedQuantity`
- `lines[].packedBaseQuantity`

Invoice response fields UI should use:
- `dispatchSummary.status`
- `dispatchSummary.dispatchCount`
- `dispatchSummary.totalDispatchedBaseQuantity`
- `dispatchSummary.totalDeliveredBaseQuantity`
- `dispatchSummary.pendingBaseQuantity`
- `dispatchSummary.lastDispatchDate`

## Screen To API Map

- Quote list and detail:
  - `GET /api/erp/sales/quotes?organizationId=...`
  - `GET /api/erp/sales/quotes/{id}`
- Quote create and actions:
  - `POST /api/erp/sales/quotes`
  - `POST /api/erp/sales/quotes/{id}/cancel`
  - `POST /api/erp/sales/quotes/{id}/convert-to-order`
  - `POST /api/erp/sales/quotes/{id}/convert-to-invoice`
  - `GET /api/erp/sales/quotes/{id}/pdf`
  - `POST /api/erp/sales/quotes/{id}/send`
- Order list and detail:
  - `GET /api/erp/sales/orders?organizationId=...`
  - `GET /api/erp/sales/orders/{id}`
- Order create and actions:
  - `POST /api/erp/sales/orders`
  - `POST /api/erp/sales/orders/{id}/cancel`
  - `POST /api/erp/sales/orders/{id}/convert-to-invoice`
  - `GET /api/erp/sales/orders/{id}/pdf`
  - `POST /api/erp/sales/orders/{id}/send`
- Invoice list and detail:
  - `GET /api/erp/sales/invoices?organizationId=...`
  - `GET /api/erp/sales/invoices/{id}`
  - `POST /api/erp/sales/invoices`
  - `GET /api/erp/sales/invoices/{id}/pdf`
  - `POST /api/erp/sales/invoices/{id}/send`
- POS session and checkout:
  - `GET /api/erp/pos/sessions?organizationId=...&branchId=...&warehouseId=...`
  - `GET /api/erp/pos/sessions/active?organizationId=...&branchId=...&warehouseId=...`
  - `GET /api/erp/pos/sessions/{id}`
  - `POST /api/erp/pos/sessions`
  - `POST /api/erp/pos/sessions/{id}/close`
  - `GET /api/erp/pos/sessions/{id}/catalog/search?query=...&customerId=...&limit=...`
  - `GET /api/erp/pos/sessions/{id}/catalog/lookup?query=...`
  - `POST /api/erp/pos/sessions/{id}/checkout`
- Payment requests:
  - `GET /api/erp/sales/payment-gateway/providers`
  - `GET /api/erp/sales/payment-requests?organizationId=...`
  - `GET /api/erp/sales/payment-requests/{id}`
  - `GET /api/erp/sales/invoices/{id}/payment-requests`
  - `POST /api/erp/sales/invoices/{id}/payment-requests`
  - `POST /api/erp/sales/payment-requests/{id}/sync-provider-status`
  - `POST /api/erp/sales/payment-requests/{id}/cancel`

### Payment Gateway UI Notes

- provider picker should be optional and default to the configured backend provider
- payment request status and provider status are different and both should be shown
- provider status is operational gateway state like `LINK_CREATED` or `LINK_ACTIVE`
- payment request status is ERP collection state like `REQUESTED`, `PARTIALLY_PAID`, `PAID`, `EXPIRED`
- receipt posting remains the source of accounting truth even if gateway status changes

- Dispatch list and actions:
  - `GET /api/erp/sales/dispatches?organizationId=...`
  - `GET /api/erp/sales/dispatches?salesInvoiceId=...`
  - `GET /api/erp/sales/dispatches/{id}`
  - `POST /api/erp/sales/invoices/{id}/dispatches`
  - `POST /api/erp/sales/dispatches/{id}/pick`
  - `POST /api/erp/sales/dispatches/{id}/pack`
  - `POST /api/erp/sales/dispatches/{id}/status`
  - `GET /api/erp/sales/dispatches/{id}/pdf`
  - `POST /api/erp/sales/dispatches/{id}/send`

- Receipt list and actions:
  - `GET /api/erp/sales/receipts?organizationId=...`
  - `POST /api/erp/sales/receipts`
  - `POST /api/erp/sales/receipts/{id}/allocate`
  - `GET /api/erp/sales/receipts/{id}/pdf`
  - `POST /api/erp/sales/receipts/{id}/send`

### Dispatch Fulfillment UI Notes

- show pick and pack progress before shipment leaves the warehouse
- use `pickedBinLocationId` to render source-bin visibility when available
- do not treat pick or pack as a second stock deduction; they are fulfillment progress markers only
