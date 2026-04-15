# Service, Warranty, and Agreements UX Design

## Outcomes

- give service teams one operational workspace to manage ticket, warranty, claim, replacement, and agreement context
- make ownership and invoice linkage visible so support staff can answer customer questions quickly
- keep warranty extension and AMC or service agreement flows discoverable from invoice and ownership journeys
- help stores manage post-sale service without forcing users to jump across unrelated screens

## Primary Users

- owner
- admin
- service desk staff
- technician
- warranty coordinator
- cashier or sales staff for lookup only

## Information Architecture

Primary navigation under `Service`:
- workspace
- service tickets
- warranty claims
- warranty extensions
- service agreements
- replacements
- ownership lookup

Recommended cross-entry points:
- invoice detail to ownership and service agreement action
- serial lookup to service ticket creation
- customer detail to service history

## Screen Inventory

### Service Workspace

Purpose:
- daily operations console for post-sale support

Widgets:
- open tickets
- overdue tickets
- pending warranty claims
- active agreements
- pending replacements

Quick actions:
- create ticket
- create claim
- extend warranty
- create service agreement

### Service Ticket Queue

Purpose:
- primary service operations list

### Service Ticket Detail

Recommended tabs:
- summary
- visits or updates
- warranty context
- claim
- agreement
- replacement
- activity

### Service Ticket Create

Purpose:
- create ticket from customer complaint, invoice, or ownership context

### Warranty Claim List

Purpose:
- claim processing queue

### Warranty Claim Detail

Recommended tabs:
- summary
- ownership and serial
- upstream references
- notes and actions
- activity

### Warranty Extension Screen

Purpose:
- create paid or promotional warranty extensions against ownership or invoice-linked assets

### Service Agreement List

Purpose:
- manage AMC or coverage contracts for customers

### Service Agreement Detail

Recommended tabs:
- summary
- linked customer and assets
- coverage
- schedule or visits later
- activity

### Replacement Flow

Purpose:
- controlled issue of replacement item from claim or service context

### Ownership Lookup

Purpose:
- search-first screen to find sold product ownership, serial, invoice, and warranty coverage

## List Page Blueprint

### Service Ticket Queue

Columns:
- ticket number
- customer
- linked product or ownership
- branch
- priority
- assigned technician
- status
- created date

Filters:
- branch
- status
- priority
- technician
- search by ticket, customer, serial, or phone

Top actions:
- create ticket

Row actions:
- view
- assign
- add update
- close if eligible

### Warranty Claim List

Columns:
- claim number
- customer
- product
- serial
- linked invoice
- claim date
- status
- branch

Filters:
- branch
- status
- claim date range
- search by claim, serial, invoice, or customer

Top actions:
- create claim

Row actions:
- view
- update status
- open ownership

### Service Agreement List

Columns:
- agreement number
- customer
- linked invoice or ownership
- start date
- end date
- status
- branch

Filters:
- branch
- status
- active only
- expiring soon
- search by agreement or customer

Top actions:
- create agreement

Row actions:
- view
- edit if allowed
- renew later

## Detail Page Blueprint

### Service Ticket Detail

Header summary:
- ticket number
- customer
- branch
- priority
- status
- assigned technician

Key cards:
- complaint summary
- product or ownership summary
- warranty coverage summary
- SLA or aging summary later

Summary tab:
- complaint description
- intake channel later
- linked invoice, serial, ownership, or claim

Visits or updates tab:
- chronological service notes
- technician updates
- parts or actions used later

Warranty context tab:
- warranty status
- standard warranty period
- extensions
- agreement coverage if any

Actions:
- assign technician
- add update
- create claim
- issue replacement
- close ticket

### Warranty Claim Detail

Header summary:
- claim number
- customer
- product
- serial
- status

Important sections:
- ownership and invoice context
- standard warranty summary
- extensions summary
- claim notes
- approval or rejection summary

Actions:
- update status
- approve or reject if workflow exists
- create replacement
- open source invoice

### Service Agreement Detail

Header summary:
- agreement number
- customer
- status
- coverage start
- coverage end

Important sections:
- linked invoice or ownership
- covered assets or services
- coverage terms
- contact details
- activity history

Actions:
- edit
- suspend later
- renew later

### Ownership Lookup Detail

Key sections:
- customer
- invoice
- product
- serial
- warranty status
- active agreement
- service history

Actions:
- create ticket
- create warranty extension
- create agreement

## Create or Edit Flow Blueprint

### Service Ticket Create

Step 1 customer and source:
- customer picker
- source type selector: invoice, ownership, serial, walk-in

Step 2 issue details:
- complaint summary
- detailed description
- priority
- branch

Step 3 assignment:
- assign technician now or later

Step 4 review:
- summary of linked context and issue

### Warranty Claim Create

Step 1 source:
- select ownership, serial, or invoice line

Step 2 product context:
- auto-load product, serial, and warranty dates

Step 3 claim details:
- issue
- claim date
- notes

Step 4 review:
- show warranty eligibility summary

### Warranty Extension Create

Step 1 anchor:
- choose ownership, serial, or invoice line

Step 2 extension details:
- extension type
- months added
- amount
- start and end date preview
- reason

Step 3 review:
- existing warranty timeline
- extension effect

### Service Agreement Create

Step 1 customer:
- customer picker

Step 2 linked context:
- optional invoice or ownership picker

Step 3 coverage:
- agreement type
- start date
- end date
- coverage notes

Step 4 commercial terms:
- fee if applicable
- notes

Step 5 review and create:
- final summary

### Replacement Flow

Step 1 source:
- select claim or ticket

Step 2 replacement item:
- product picker
- stock location
- serial if applicable

Step 3 review:
- stock impact
- customer context
- warranty implication

Step 4 issue:
- confirm replacement

## State Handling

Tickets:
- open
- assigned
- in progress
- awaiting customer
- closed

Claims:
- draft
- submitted
- approved
- rejected
- fulfilled

Warranty:
- active
- expired
- extended
- agreement-backed

Errors and validation:
- ownership not found
- invoice line missing ownership reference
- serial already linked to another active process
- replacement item unavailable

Permission states:
- view-only users can search and inspect
- service managers can assign and close
- warranty specialists can create claims and extensions

## CTA Map

Service workspace:
- primary: Create ticket
- secondary: Create claim
- tertiary: Create agreement

Ticket queue:
- primary: Create ticket
- row primary: View
- row secondary: Assign or Update

Claim list:
- primary: Create claim
- row primary: View
- row secondary: Open ownership

Agreement list:
- primary: Create agreement
- row primary: View

Ownership lookup:
- primary: Create ticket
- secondary: Extend warranty
- tertiary: Create agreement

## Desktop and Mobile Notes

Desktop:
- ticket detail should work as the main service workspace
- use tabs generously because service context is deep and multi-source

Mobile:
- prioritize lookup, status updates, and quick actions for technicians
- full agreement and replacement workflows are better on tablet or desktop

Tablet:
- strong fit for service desk counters and workshop use
- keep ticket summary and action bar always visible

## Cross-Module Navigation

- service ticket to customer detail
- service ticket to invoice detail
- claim detail to ownership lookup
- ownership lookup to product serial detail
- agreement detail to customer and invoice
- replacement flow to inventory stock lookup

## Key Forms And Backend Fields

### Service Ticket Create

- `organizationId`
- `branchId`
- `customerId`
- `salesInvoiceId`
- `salesReturnId`
- `sourceType`
- `priority`
- `complaintSummary`
- `issueDescription`
- `reportedOn`
- `assignedToUserId`
- `items[]`

Ticket item fields:
- `productId`
- `serialNumberId`
- `productOwnershipId`
- `symptomNotes`

### Assign Ticket

- `organizationId`
- `branchId`
- `assignedToUserId`
- `remarks`

### Service Visit

- `organizationId`
- `branchId`
- `technicianUserId`
- `scheduledAt`
- `startedAt`
- `completedAt`
- `visitStatus`
- `visitNotes`
- `partsUsedJson`
- `customerFeedback`

### Close Ticket

- `organizationId`
- `branchId`
- `resolutionStatus`
- `diagnosisNotes`
- `remarks`

### Warranty Claim Create

- `organizationId`
- `branchId`
- `serviceTicketId`
- `customerId`
- `productId`
- `serialNumberId`
- `productOwnershipId`
- `salesInvoiceId`
- `salesReturnId`
- `supplierId`
- `distributorId`
- `upstreamRouteType`
- `upstreamCompanyName`
- `upstreamReferenceNumber`
- `upstreamStatus`
- `routedOn`
- `claimType`
- `claimDate`
- `claimNotes`

### Warranty Extension Create

- `organizationId`
- `branchId`
- `extensionType`
- `monthsAdded`
- `startDate`
- `endDate`
- `reason`
- `referenceNumber`
- `amount`
- `remarks`

### Service Agreement Create

- `organizationId`
- `branchId`
- `customerId`
- `salesInvoiceId`
- `agreementType`
- `status`
- `serviceStartDate`
- `serviceEndDate`
- `laborIncluded`
- `partsIncluded`
- `preventiveVisitsIncluded`
- `visitLimit`
- `slaHours`
- `agreementAmount`
- `notes`
- `items[]`

### Replacement Create

- `organizationId`
- `branchId`
- `warehouseId`
- `serviceTicketId`
- `warrantyClaimId`
- `salesReturnId`
- `customerId`
- `originalProductId`
- `originalSerialNumberId`
- `originalProductOwnershipId`
- `replacementProductId`
- `replacementSerialNumberId`
- `replacementUomId`
- `replacementQuantity`
- `replacementBaseQuantity`
- `replacementType`
- `stockSourceBucket`
- `issuedOn`
- `warrantyStartDate`
- `warrantyEndDate`
- `notes`

## Screen To API Map

- Ticket queue and detail:
  - `GET /api/erp/service/tickets?organizationId=...`
  - `GET /api/erp/service/tickets/{id}?organizationId=...`
- Ticket actions:
  - `POST /api/erp/service/tickets`
  - `POST /api/erp/service/tickets/{id}/assign`
  - `POST /api/erp/service/tickets/{id}/visits`
  - `POST /api/erp/service/tickets/{id}/close`
- Warranty claims:
  - `GET /api/erp/service/warranty-claims?organizationId=...`
  - `GET /api/erp/service/warranty-claims/{id}?organizationId=...`
  - `POST /api/erp/service/warranty-claims`
  - `POST /api/erp/service/warranty-claims/{id}/status`
- Ownership warranty and extensions:
  - `GET /api/erp/service/ownership/{id}/warranty?organizationId=...`
  - `GET /api/erp/service/ownership/{id}/warranty-extensions?organizationId=...`
  - `POST /api/erp/service/ownership/{id}/warranty-extensions`
  - `POST /api/erp/service/warranty-extensions/{id}/cancel`
- Agreements:
  - `GET /api/erp/service/agreements?organizationId=...`
  - `GET /api/erp/service/agreements/{id}?organizationId=...`
  - `POST /api/erp/service/agreements`
- Replacements:
  - `GET /api/erp/service/replacements?organizationId=...`
  - `GET /api/erp/service/replacements/{id}?organizationId=...`
  - `POST /api/erp/service/replacements`
