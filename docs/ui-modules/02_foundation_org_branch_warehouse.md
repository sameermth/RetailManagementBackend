# Organization, Branch, Warehouse UX Design

## Outcomes

- let owner finish structural setup fast
- keep organization structure understandable
- make branch and warehouse setup reusable for later scaling

## Primary Users

- onboarding owner
- owner
- admin
- platform admin in central management mode

## Information Architecture

- setup
  - organization
  - branches
  - warehouses
- settings
  - business information
  - branch structure
  - warehouse structure

## Screen Inventory

### Organization List

Used for:
- org switcher
- owner multi-org management

### Organization Detail

Recommended tabs:
- overview
- business info
- branches
- warehouses
- team
- tax
- subscription

### Organization Create or Edit

Use as:
- onboarding stepper for first org
- normal form for later org creation

### Branch List

Should sit inside organization context but may also have standalone entry

### Branch Detail

Recommended tabs:
- overview
- warehouses
- employees

### Branch Create or Edit

### Warehouse List

### Warehouse Detail

Recommended tabs:
- overview
- stock balances
- stock movements

### Warehouse Create or Edit

## List Page Blueprint: Organization List

Columns or cards:
- org code
- org name
- owner plan
- active status
- branch count
- warehouse count
- team count

Filters:
- active
- current plan
- search by code or name

Actions:
- switch
- view
- edit
- create organization if allowed

## Detail Page Blueprint: Organization Detail

Overview cards:
- organization status
- active plan
- number of branches
- number of warehouses
- GST threshold state

Tab details:
- business info tab for core company data
- branches tab for branch list plus create button
- warehouses tab for warehouse list plus create button
- tax and subscription as linked setup areas

## Create or Edit Flow Blueprint

Organization form groups:

1. business identity
- name
- code
- legal name

2. contact
- phone
- email

3. compliance
- GSTIN
- threshold amount
- threshold alert toggle

4. status
- active

Branch form groups:
- branch code and name
- contact
- address
- active

Warehouse form groups:
- warehouse code and name
- linked branch
- contact or description later
- active

## State Handling

Organization creation:
- code already exists
- subscription limit reached

Branch and warehouse pages:
- first branch empty state
- first warehouse empty state
- inactive row visualization

## CTA Map

Organization list:
- primary: Add organization
- row CTA: Switch
- secondary: View

Organization detail:
- primary: Edit organization
- branch tab CTA: Add branch
- warehouse tab CTA: Add warehouse

Branch list:
- primary: Add branch

Warehouse list:
- primary: Add warehouse

## Desktop and Mobile Notes

Desktop:
- org detail should use tabbed layout
- branch and warehouse lists can be split with detail pane

Mobile:
- branch and warehouse detail should use stacked cards
- avoid wide multi-column tables

## Cross-Module Navigation

- org detail to subscription
- org detail to tax
- branch detail to employees
- warehouse detail to inventory balances and movements

## Key Forms And Backend Fields

### Organization Create Or Edit

- `name`
- `code`
- `legalName`
- `phone`
- `email`
- `gstin`
- `gstThresholdAlertEnabled`
- `isActive`

Organization tax note:
- threshold amount is no longer a normal editable org field
- UI should show threshold status from the tax module instead of asking the user to type a threshold amount

### Branch Create

- `organizationId`
- `code`
- `name`
- `phone`
- `email`
- `addressLine1`
- `addressLine2`
- `city`
- `state`
- `postalCode`
- `country`
- `isActive`

### Branch Update

- same as branch create except `organizationId` is supplied in query context

### Warehouse Create

- `organizationId`
- `branchId`
- `code`
- `name`
- `isPrimary`
- `isActive`

### Warehouse Update

- `code`
- `name`
- `isPrimary`
- `isActive`

## Screen To API Map

- Organization list:
  - `GET /api/erp/organizations`
- Organization detail:
  - `GET /api/erp/organizations/{id}`
- Organization create:
  - `POST /api/erp/organizations`
- Organization update:
  - `PUT /api/erp/organizations/{id}`
- Branch list:
  - `GET /api/erp/branches?organizationId=...`
- Branch detail:
  - `GET /api/erp/branches/{id}?organizationId=...`
- Branch create:
  - `POST /api/erp/branches`
- Branch update:
  - `PUT /api/erp/branches/{id}?organizationId=...`
- Warehouse list:
  - `GET /api/erp/warehouses?organizationId=...&branchId=...`
- Warehouse detail:
  - `GET /api/erp/warehouses/{id}?organizationId=...`
- Warehouse create:
  - `POST /api/erp/warehouses`
- Warehouse update:
  - `PUT /api/erp/warehouses/{id}?organizationId=...`
