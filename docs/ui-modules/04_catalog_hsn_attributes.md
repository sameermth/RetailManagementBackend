# Catalog, HSN, and Attributes UX Design

## Outcomes

- make product creation fast enough for store operations
- reduce raw ID entry and tax mistakes
- support dynamic product attributes and future variant complexity
- present catalog pricing as the store default, not as the permanent truth for all future stock
- support bundle or kit products that sell as one item while consuming component stock

## Primary Users

- owner
- admin
- store manager
- purchase operator in assisted product setup

## Information Architecture

- products
  - product list
  - product detail
  - create product
  - link from shared catalog
- catalog setup
  - dynamic attributes
  - tax and HSN lookup
- product pricing
- product suppliers
- bundle builder

## Screen Inventory

### Product List

### Product Detail

Tabs:
- overview
- attributes
- pricing
- bundle components
- suppliers
- stock

### Product Create Wizard

### Shared Catalog Discovery

### Pricing Manager

### Product Supplier Mapping

### Bundle Builder

### Attribute Definition Manager

### Product Import Workspace

## List Page Blueprint: Product List

Columns:
- SKU
- product name
- category
- brand
- HSN
- tax group
- UOM
- tracking mode
- active

Filters:
- category
- brand
- active
- inventory tracking mode
- service item toggle
- search

Top actions:
- create product
- link shared product
- create bundle

Row actions:
- view
- edit
- prices
- bundle
- suppliers
- stock

## Detail Page Blueprint: Product Detail

Overview cards:
- base identity
- tax summary
- stock summary
- supplier count

Important fields to show:
- category
- brand
- HSN
- tax group
- UOM
- tracking flags
- service or product type

Tab behavior:
- attributes tab for dynamic fields
- pricing tab for price rows
- bundle components tab for bundle composition
- suppliers tab for sourcing
- stock tab for inventory drilldown

## Create or Edit Flow Blueprint

Step 1 identity:
- name
- SKU
- category picker
- brand picker
- UOM picker

Step 2 tax and inventory:
- HSN type-ahead
- tax group suggestion
- tax group picker
- tracking mode
- serial tracking
- batch tracking
- expiry tracking
- fractional quantity flag

Step 3 dynamic attributes:
- backend-driven fields
- support text, number, option, unit label

Step 4 pricing:
- optional initial price creation
- capture default sale price and default MRP as store-level defaults
- explain in UI copy that receipt stock can later carry newer MRP and suggested sale price

Step 4A bundle setup when applicable:
- `isBundle` toggle
- bundle pricing mode
- component rows with product picker and quantity
- warning that nested bundles are not allowed
- warning that bundles must remain standard tracked and cannot be serial or batch tracked

Step 5 suppliers:
- optional supplier attach

Review:
- identity
- tax
- attributes
- pricing summary

## State Handling

- first product empty state
- no shared catalog matches
- HSN not found state with manual tax group fallback
- attribute definitions unavailable state
- duplicate SKU or validation error
- import history empty state
- partial import state with row error review and failed-row export
- platform-governed product warning or blocked state from shared catalog

## CTA Map

Product list:
- primary: New product
- secondary: Link from catalog

Product detail:
- primary: Edit product
- secondary: Add price
- tertiary: Manage bundle or suppliers

Shared catalog:
- row CTA: Link to store

## Desktop and Mobile Notes

Desktop:
- wizard with sticky review summary works well
- product detail can use tabs

Mobile:
- step-based flow with bottom CTA bar
- avoid large inline editable tables

## Cross-Module Navigation

- product to inventory
- product to supplier preferences
- product to sales and purchases later
- product list to bulk import workspace

Platform governance note:
- shared catalog products can be `RESTRICTED`, `DISCONTINUED`, `BLOCKED`, or `RECALLED`
- `DISCONTINUED` or explicit `blockNewStoreAdoption` prevents linking into a new store catalog
- `BLOCKED` and `RECALLED` should be shown as hard-stop states because downstream purchase, sales, recurring, and service entry points will reject them

## Bulk Import UX Blueprint

Use a dedicated import workspace for products:
- template download
- file upload
- preview
- validation table
- import execution
- recent jobs
- failed-row export

Preview table should show:
- row number
- action
- validation status
- SKU
- name
- category
- brand
- UOM
- tax group or HSN issue
- messages

Important UI rule:
- product import can auto-create missing category and brand during execution
- import execution returns `importJobId`
- recent jobs should show import status and let the user open detail or download failed rows

## Key Forms And Backend Fields

### Store Product Create

- `organizationId`
- `productId`: optional when linking to shared product
- `categoryId`
- `brandId`
- `baseUomId`
- `taxGroupId`
- `sku`
- `name`
- `description`
- `hsnCode`
- `inventoryTrackingMode`
- `serialTrackingEnabled`
- `batchTrackingEnabled`
- `expiryTrackingEnabled`
- `fractionalQuantityAllowed`
- `attributes[]`
- `minStockBaseQty`
- `reorderLevelBaseQty`
- `defaultSalePrice`
- `defaultMrp`
- `defaultWarrantyMonths`
- `warrantyTerms`
- `isBundle`
- `bundlePricingMode`
- `isServiceItem`
- `isActive`

Catalog pricing note:
- `defaultSalePrice` and `defaultMrp` are defaults used for store-product display and quote or order pricing
- invoice pricing can later resolve from batch, serial, or other stock-layer pricing captured during purchase receipt

Bundle note:
- bundle products are configured at store-product level
- current backend expands bundles into component lines during quote, order, and invoice persistence
- UI should treat bundle setup as catalog configuration, not as a separate stock movement

### Bundle Components

- `components[]`
- per component:
  - `componentStoreProductId`
  - `componentQuantity`
  - `componentBaseQuantity`
  - `sortOrder`

### Shared Product Link

- `organizationId`
- `productId`
- `categoryId`
- `brandId`
- `taxGroupId`
- `sku`
- `name`
- `description`
- `attributes[]`
- `minStockBaseQty`
- `reorderLevelBaseQty`
- `defaultSalePrice`
- `defaultMrp`
- `defaultWarrantyMonths`
- `warrantyTerms`
- `isBundle`
- `bundlePricingMode`
- `isActive`

### Dynamic Attribute Definition

- `organizationId`
- `code`
- `label`
- `description`
- `dataType`
- `inputType`
- `isRequired`
- `isActive`
- `unitLabel`
- `placeholder`
- `helpText`
- `sortOrder`
- `options[]`
- `scopes[]`

### Product Price

- `priceType`
- `customerSegment`
- `price`
- `minQuantity`
- `effectiveFrom`
- `effectiveTo`
- `isDefault`
- `isActive`

## Screen To API Map

- Product list:
  - `GET /api/erp/products?organizationId=...`
- Product detail:
  - `GET /api/erp/products/{id}`
- Product create or update:
  - `POST /api/erp/products`
  - `PUT /api/erp/products/{id}`
- Shared catalog:
  - `GET /api/erp/products/catalog?query=...`
  - `GET /api/erp/products/catalog/{id}`
  - `GET /api/erp/products/discover?organizationId=...&query=...`
  - `POST /api/erp/products/link`
- Product scan:
  - `GET /api/erp/products/scan?organizationId=...&warehouseId=...&query=...`
- HSN and tax helper:
  - `GET /api/erp/hsn?query=...&effectiveDate=...`
  - `GET /api/erp/hsn/{hsnCode}?effectiveDate=...`
  - `GET /api/erp/products/tax-group-suggestion?organizationId=...&hsnCode=...&effectiveDate=...`
- Reference pickers:
  - `GET /api/erp/catalog/categories?organizationId=...&query=...`
  - `GET /api/erp/catalog/brands?organizationId=...&query=...`
  - `GET /api/erp/catalog/uoms?query=...`
  - `GET /api/erp/catalog/tax-groups?organizationId=...&query=...`
- Dynamic attributes:
  - `GET /api/erp/catalog/attributes?organizationId=...&categoryId=...&brandId=...`
  - `GET /api/erp/catalog/attributes/{id}?organizationId=...`
  - `GET /api/erp/catalog/attributes/ui-config?organizationId=...`
  - `POST /api/erp/catalog/attributes`
  - `PUT /api/erp/catalog/attributes/{id}`
- Product suppliers and prices:
  - `GET /api/erp/products/{id}/suppliers?organizationId=...`
  - `PUT /api/erp/products/{id}/suppliers?organizationId=...`
  - `GET /api/erp/products/{id}/prices?organizationId=...`
  - `POST /api/erp/products/{id}/prices?organizationId=...`
  - `PUT /api/erp/products/{id}/prices/{priceId}?organizationId=...`
- Bundle builder:
  - `GET /api/erp/products/{id}/bundle?organizationId=...`
  - `PUT /api/erp/products/{id}/bundle?organizationId=...`
- Product import:
  - `GET /api/erp/imports/products/template`
  - `POST /api/erp/imports/products/preview?organizationId=...&updateExisting=...`
  - `POST /api/erp/imports/products?organizationId=...&updateExisting=...`
- Shared import job history:
  - `GET /api/erp/imports/history?organizationId=...&entityType=PRODUCTS`
  - `GET /api/erp/imports/history/{id}?organizationId=...`
  - `GET /api/erp/imports/history/{id}/failed-rows?organizationId=...`
