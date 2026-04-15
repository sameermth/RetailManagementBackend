# Customers and Suppliers UX Design

## Outcomes

- give commercial teams fast party lookup
- keep terms separate from core identity
- make supplier-product sourcing visible and editable

## Primary Users

- owner
- admin
- accountant
- cashier for customer use
- purchase operator for supplier use

## Information Architecture

- parties
  - customers
  - suppliers
- customer terms
- supplier terms
- supplier catalog and product links

## Screen Inventory

### Customer List

### Customer Detail

Tabs:
- overview
- contact and address
- terms
- sales history later
- service history later

### Customer Create or Edit

### Customer Terms Editor

### Supplier List

### Supplier Detail

Tabs:
- overview
- terms
- product links
- purchasable catalog

### Supplier Create or Edit

### Supplier Product Link Manager

### Party Import Workspace

## List Page Blueprint: Customer List

Columns:
- customer code
- full name
- phone
- email
- GSTIN
- type
- credit limit
- status

Filters:
- type
- status
- search by code, name, phone, GSTIN

Top actions:
- add customer

Row actions:
- view
- edit
- terms
- new sale later

## List Page Blueprint: Supplier List

Columns:
- supplier name
- phone
- email
- GSTIN
- status
- linked products count later

Filters:
- status
- search

Top actions:
- add supplier

Row actions:
- view
- edit
- terms
- products

## Create or Edit Flow Blueprint

Customer form groups:
- identity
- GST and business
- contact
- billing and shipping addresses
- notes and credit

Supplier form groups:
- identity
- GST and business
- contact
- address
- notes

Terms editor groups:
- credit or payment terms
- limits and reminders
- store-specific notes

Supplier product link form:
- store product
- supplier product code or name
- lead time
- preference flags

## State Handling

- first customer empty state with sales CTA
- first supplier empty state with purchase CTA
- no product links yet for supplier
- duplicate validation messages
- import history empty state
- partial import state with failed-row download CTA

## CTA Map

Customer list:
- primary: Add customer

Customer detail:
- primary: Edit customer
- secondary: Edit terms

Supplier list:
- primary: Add supplier

Supplier detail:
- primary: Edit supplier
- secondary: Manage product links

## Desktop and Mobile Notes

Desktop:
- customer and supplier lists can be table-first

Mobile:
- use cards with key details and quick-call action
- terms editors should be single-column forms

## Cross-Module Navigation

- customer to sales and service
- supplier to purchases and product sourcing
- customer and supplier lists to bulk import workspace

## Bulk Import UX Blueprint

Use one shared import workspace with entity tabs:
- customers
- suppliers

Recommended layout:
- template download
- file upload
- preview summary
- row-level validation table
- import action
- recent import jobs
- failed-row download

Recent import jobs table:
- started at
- source file name
- entity type
- total rows
- imported rows
- failed rows
- status
- actions: view detail, download failed rows

Important UI rule:
- import preview does not create history
- import execution returns `importJobId`
- history/detail screens should link back to the same entity list page

## Key Forms And Backend Fields

### Customer Create Or Edit

- `customerCode`
- `fullName`
- `customerType`
- `legalName`
- `tradeName`
- `phone`
- `email`
- `gstin`
- `linkedOrganizationId`
- `billingAddress`
- `shippingAddress`
- `state`
- `stateCode`
- `contactPersonName`
- `contactPersonPhone`
- `contactPersonEmail`
- `creditLimit`
- `isPlatformLinked`
- `notes`
- `status`

### Customer Terms

- `customerSegment`
- `creditLimit`
- `creditDays`
- `loyaltyEnabled`
- `loyaltyPointsBalance`
- `priceTier`
- `discountPolicy`
- `isPreferred`
- `isActive`
- `contractStart`
- `contractEnd`
- `remarks`

### Supplier Create Or Edit

- `supplierCode`
- `name`
- `legalName`
- `tradeName`
- `phone`
- `email`
- `gstin`
- `linkedOrganizationId`
- `billingAddress`
- `shippingAddress`
- `state`
- `stateCode`
- `contactPersonName`
- `contactPersonPhone`
- `contactPersonEmail`
- `paymentTerms`
- `isPlatformLinked`
- `notes`
- `status`

### Supplier Terms

- `paymentTerms`
- `creditLimit`
- `creditDays`
- `isPreferred`
- `isActive`
- `contractStart`
- `contractEnd`
- `orderViaEmail`
- `orderViaWhatsapp`
- `remarks`

### Supplier Product Link

- `productId`
- `supplierProductCode`
- `supplierProductName`
- `priority`
- `isPreferred`
- `isActive`

## Screen To API Map

- Customer list:
  - `GET /api/erp/customers?organizationId=...`
- Customer create or update:
  - `POST /api/erp/customers?organizationId=...&branchId=...`
  - `PUT /api/erp/customers/{customerId}?organizationId=...`
- Customer terms:
  - `GET /api/erp/customers/{customerId}/terms?organizationId=...`
  - `PUT /api/erp/customers/{customerId}/terms?organizationId=...`
- Supplier list:
  - `GET /api/erp/suppliers?organizationId=...`
- Supplier create or update:
  - `POST /api/erp/suppliers?organizationId=...&branchId=...`
  - `PUT /api/erp/suppliers/{supplierId}?organizationId=...`
- Supplier catalog and terms:
  - `GET /api/erp/suppliers/{supplierId}/catalog?organizationId=...`
  - `GET /api/erp/suppliers/{supplierId}/terms?organizationId=...`
  - `PUT /api/erp/suppliers/{supplierId}/terms?organizationId=...`
- Supplier product links:
  - `GET /api/erp/suppliers/{supplierId}/products?organizationId=...`
  - `POST /api/erp/suppliers/{supplierId}/products?organizationId=...`
  - `PUT /api/erp/suppliers/{supplierId}/products/{supplierProductId}?organizationId=...`
- Store product supplier preference:
  - `GET /api/erp/suppliers/product-preferences/{storeProductId}?organizationId=...`
  - `PUT /api/erp/suppliers/product-preferences/{storeProductId}?organizationId=...`
- Customer import:
  - `GET /api/erp/imports/customers/template`
  - `POST /api/erp/imports/customers/preview?organizationId=...&branchId=...&updateExisting=...`
  - `POST /api/erp/imports/customers?organizationId=...&branchId=...&updateExisting=...`
- Supplier import:
  - `GET /api/erp/imports/suppliers/template`
  - `POST /api/erp/imports/suppliers/preview?organizationId=...&branchId=...&updateExisting=...`
  - `POST /api/erp/imports/suppliers?organizationId=...&branchId=...&updateExisting=...`
- Shared import job history:
  - `GET /api/erp/imports/history?organizationId=...&entityType=CUSTOMERS|SUPPLIERS`
  - `GET /api/erp/imports/history/{id}?organizationId=...`
  - `GET /api/erp/imports/history/{id}/failed-rows?organizationId=...`
