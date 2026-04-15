# Returns UX Design

## Outcomes

- separate customer-return intake from final posting so operators do not make rushed stock decisions
- make financial and stock impact visible before posting a return
- keep sales returns and purchase returns parallel, but not merged into one confusing flow
- connect returns with invoice, receipt, supplier, and inventory screens clearly

## Primary Users

- owner
- admin
- cashier
- store manager
- accountant
- purchase operator for supplier-facing returns

## Information Architecture

Primary navigation under `Returns`:
- workspace
- sales returns
- purchase returns

Recommended sub-navigation inside each return type:
- list
- detail
- create
- inspection or posting

## Screen Inventory

### Returns Workspace

Purpose:
- operational dashboard for all pending return activity

Widgets:
- pending sales return inspections
- pending supplier return actions
- recently posted returns
- value of returns this period

Quick actions:
- create sales return
- create purchase return

### Sales Return List

Purpose:
- queue for customer-originated return cases

### Sales Return Detail

Recommended tabs:
- summary
- items
- inspection and posting
- financial impact
- activity

### Sales Return Create

Purpose:
- intake flow linked to source invoice

### Sales Return Inspection and Post

Purpose:
- operational decision screen for accepted, rejected, or partial returns

### Purchase Return List

Purpose:
- queue for returns back to suppliers

### Purchase Return Detail

Recommended tabs:
- summary
- items
- supplier context
- financial impact
- activity

### Purchase Return Create

Purpose:
- supplier return creation from source receipt

## List Page Blueprint

### Sales Return List

Columns:
- return number
- customer
- source invoice number
- branch
- return date
- status
- total amount
- posting state

Filters:
- branch
- status
- date range
- customer
- search by return number or invoice

Top actions:
- create sales return

Row actions:
- view
- inspect if pending
- post if eligible
- preview PDF later if available

### Purchase Return List

Columns:
- return number
- supplier
- source receipt number
- branch
- return date
- status
- total amount
- posting state

Filters:
- branch
- supplier
- status
- date range
- search by return number or receipt

Top actions:
- create purchase return

Row actions:
- view
- edit if draft
- post if eligible

## Detail Page Blueprint

### Sales Return Detail

Header summary:
- return number
- customer
- source invoice
- branch
- status
- total return amount

Key cards:
- return summary
- inspection summary
- financial impact summary

Items table:
- item
- invoiced qty
- returned qty requested
- accepted qty
- rejected qty
- unit value
- tax amount
- line impact

Inspection block should show:
- reason
- condition notes
- accepted or rejected decision
- stock action when available

Financial impact section:
- subtotal reversal
- CGST reversal
- SGST reversal
- IGST reversal
- total credit impact
- refund or adjustment status later

Actions:
- edit if draft
- inspect if pending
- post if ready
- open source invoice

### Purchase Return Detail

Header summary:
- return number
- supplier
- source receipt
- branch
- status
- total return amount

Sections:
- supplier summary
- return lines
- inventory impact
- payable impact

Actions:
- edit if draft
- post
- open source receipt
- open supplier detail

## Create or Edit Flow Blueprint

### Sales Return Create

Step 1 source invoice:
- invoice search
- customer auto-populated
- branch auto-populated

Step 2 select lines:
- show source invoice lines
- choose returnable qty
- capture return qty

Step 3 reason and notes:
- return reason
- remarks

Step 4 impact preview:
- expected value reversal
- GST reversal preview

Step 5 save:
- save as draft or submit for inspection

### Sales Return Inspection and Post

Step 1 inspect:
- per-line accepted qty
- rejected qty
- inspection notes

Step 2 outcome:
- restock, hold, or scrap intent when supported

Step 3 posting review:
- stock impact summary
- financial impact summary

Step 4 post:
- final irreversible confirmation

### Purchase Return Create

Step 1 source receipt:
- supplier search or receipt search

Step 2 lines:
- choose receipt lines and return qty

Step 3 reason:
- supplier return reason
- notes

Step 4 review:
- stock reduction summary
- supplier payable impact summary

Step 5 save or post:
- save draft
- submit or post depending on workflow

## State Handling

Sales returns:
- draft
- pending inspection
- partially accepted
- posted
- cancelled if supported later

Purchase returns:
- draft
- pending supplier action
- posted

Error and validation states:
- invoice has no returnable lines
- returned qty exceeds available returnable qty
- receipt has no eligible lines
- posting conflict due to stock or accounting state

Permission states:
- intake staff can create but not post
- accountant or manager can post

## CTA Map

Returns workspace:
- primary: Create sales return
- secondary: Create purchase return

Sales return list:
- primary: Create sales return
- row primary: View
- row secondary: Inspect or Post

Purchase return list:
- primary: Create purchase return
- row primary: View
- row secondary: Post

Sales return detail:
- primary: Inspect or Post
- secondary: Open source invoice

Purchase return detail:
- primary: Post
- secondary: Open source receipt

## Desktop and Mobile Notes

Desktop:
- inspection screen should allow line-by-line decisioning without excessive scrolling
- financial impact summary should remain visible during posting

Mobile:
- return creation is possible, but inspection and posting are better optimized for tablet or desktop
- use stacked line cards with clear accepted and rejected quantities

## Cross-Module Navigation

- sales return to source invoice
- purchase return to source receipt
- sales return to customer detail
- purchase return to supplier detail
- posted return to inventory impact
- posted return to finance review later

## Key Forms And Backend Fields

### Sales Return Create

- `organizationId`
- `branchId`
- `originalSalesInvoiceId`
- `returnDate`
- `reason`
- `remarks`
- `lines[]`

Sales return line fields:
- `originalSalesInvoiceLineId`
- `quantity`
- `baseQuantity`
- `serialNumberIds[]`
- `batchSelections[]`
- `disposition`
- `reason`

### Sales Return Inspection

- `inspectionNotes`
- `lines[]`

Inspection line fields:
- `salesReturnLineId`
- `disposition`
- `inspectionStatus`
- `inspectionNotes`

### Purchase Return Create

- `organizationId`
- `branchId`
- `originalPurchaseReceiptId`
- `returnDate`
- `reason`
- `remarks`
- `lines[]`

Purchase return line fields:
- `originalPurchaseReceiptLineId`
- `quantity`
- `baseQuantity`
- `serialNumberIds[]`
- `batchSelections[]`
- `reason`

## Screen To API Map

- Sales return list and detail:
  - `GET /api/erp/returns/sales?organizationId=...`
  - `GET /api/erp/returns/sales/{id}`
- Sales return create and inspect:
  - `POST /api/erp/returns/sales`
  - `POST /api/erp/returns/sales/{id}/inspect`
- Purchase return list and detail:
  - `GET /api/erp/returns/purchases?organizationId=...`
  - `GET /api/erp/returns/purchases/{id}`
- Purchase return create:
  - `POST /api/erp/returns/purchases`
