# Platform Admin UX Design

## Outcomes

- allow platform team to onboard stores end to end
- allow platform team to operate stores, subscriptions, support, and health centrally
- create room for future catalog governance and marketplace controls

## Primary Users

- platform administrator
- platform operations manager
- support and grievance team
- subscription and growth operations team

## Information Architecture

Top-level navigation:
- overview
- stores
- subscriptions
- store teams
- catalog governance
- support and grievances
- feedback
- notifications
- audit activity
- system health
- reports

Future reserved areas:
- catalog governance
- seller compliance
- complaints and recalls

## Screen Inventory

### Platform Overview

Purpose:
- command center for the platform

Widgets:
- total stores
- active stores
- owner accounts
- active users
- subscription plan mix
- open support items
- failed notifications
- active schedules

Quick actions:
- onboard store
- create plan
- inspect health

### Store List

Purpose:
- central store directory

### Store Detail

Tabs:
- overview
- store profile
- subscription
- branches and warehouses
- team
- support
- audit

### Existing Owner Store Create

Short flow for assigning new store to existing owner

### Full Store Onboarding

Stepper flow for:
- owner
- store
- branch
- subscription
- review

### Subscription Console

Purpose:
- cross-store subscription oversight

### Store Team Console

Purpose:
- cross-store employee management

### Catalog Governance Console

Purpose:
- control shared product status across stores
- see where a governed product is linked
- move from incident to restriction, discontinue, block, or recall action

### Support and Grievance Queue

Purpose:
- centralized work queue for service tickets and warranty claims

### Feedback, Notifications, Audit, System Health, Reports

Purpose:
- operational visibility and intervention

## List Page Blueprint: Store List

Columns:
- organization code
- store name
- owner login
- active
- branch count
- team count
- current plan
- subscription status

Filters:
- active or inactive
- plan
- subscription status
- search by store or owner

Top actions:
- onboard new store
- create store for existing owner

Row actions:
- view
- edit
- activate or deactivate
- change plan
- open team

## Detail Page Blueprint: Store Detail

Overview cards:
- store status
- owner
- active plan
- org count used by owner
- branch count
- team count

Tab expectations:
- overview for high-level summary
- profile for business details
- subscription for plan lifecycle
- branches and warehouses for structure
- team for people
- support for live operational issues
- audit for history

## Create or Edit Flow Blueprint

### Existing Owner Store Create

Sections:
1. owner search
2. store details
3. confirm

### Full Store Onboarding

Step 1 owner:
- login identifier
- password
- full name
- email
- phone
- active

Step 2 store:
- store name
- store code
- legal name
- phone
- email
- GSTIN
- threshold amount
- threshold alert enabled
- active

Step 3 first branch:
- optional
- branch code
- branch name
- phone
- email
- address

Step 4 subscription:
- optional
- plan picker
- status
- dates
- auto renew
- notes

Step 5 review:
- owner summary
- store summary
- branch summary
- subscription summary

Success page actions:
- open store detail
- manage subscription
- manage team
- start catalog setup later

## State Handling

Store list:
- no stores yet
- no results

Onboarding:
- duplicate owner login
- duplicate owner email
- duplicate owner phone
- duplicate store code
- invalid plan code

Support queue:
- empty queue
- high-priority items warning state

Catalog governance:
- no governed products match filters
- product has no linked stores
- incident exists but no linked catalog product
- blocked or recalled state warning banner on impact view

System health:
- degraded notification state
- failed schedule state

## CTA Map

Overview:
- primary: Onboard new store
- secondary: Create subscription plan

Store list:
- primary: Onboard store
- secondary: Create store for existing owner

Store detail:
- primary: Edit store
- secondary: Change subscription

Support queue:
- row CTA: Assign
- row secondary CTA: Close or update status

Catalog governance:
- primary: Apply governance action
- secondary: View linked stores
- tertiary: Open related incidents

## Desktop and Mobile Notes

Desktop:
- use console-style layout with summary cards and dense tables
- store detail should use persistent tabs

Mobile:
- platform admin is likely secondary use case
- support responsive behavior, but optimize for tablet and desktop first

## Permission and Gating

- entire module gated by `platform.manage`

## Cross-Module Navigation

- store detail to subscription and team
- support queue to service and claim detail
- notifications and audit to affected store
- catalog governance to impacted store products and incidents

## Key Forms And Backend Fields

### Full Store Onboarding

Owner block:
- `loginIdentifier`
- `password`
- `fullName`
- `email`
- `phone`
- `active`

Store block:
- `name`
- `code`
- `legalName`
- `phone`
- `email`
- `gstin`
- `gstThresholdAmount`
- `gstThresholdAlertEnabled`
- `isActive`

Optional branch block:
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

Optional subscription block:
- `planCode`
- `status`
- `startsOn`
- `endsOn`
- `autoRenew`
- `notes`

### Existing Owner Store Create

- same store block as above
- plus `ownerAccountId`

### Store Team Update

- `fullName`
- `email`
- `phone`
- `roleCode`
- `employeeCode`
- `defaultBranchId`
- `branchIds`
- `active`

### Subscription Plan Create Or Update

- `code`
- `name`
- `description`
- `billingPeriod`
- `maxOrganizations`
- `unlimitedOrganizations`
- `active`

### Incident Governance Action

- `actionType`
- `governanceReason`
- `resolutionNotes`
- `incidentStatus`

## Screen To API Map

- Platform overview:
  - `GET /api/platform-admin/overview`
- Store list:
  - `GET /api/platform-admin/stores`
- Owner picker:
  - `GET /api/platform-admin/owner-accounts?query=...`
- Store detail:
  - `GET /api/platform-admin/stores/{organizationId}`
- Create store for existing owner:
  - `POST /api/platform-admin/stores`
- Full onboarding:
  - `POST /api/platform-admin/stores/onboard`
- Store update:
  - `PUT /api/platform-admin/stores/{organizationId}`
- Store status:
  - `PUT /api/platform-admin/stores/{organizationId}/status`
- Subscription console:
  - `GET /api/platform-admin/subscriptions`
  - `POST /api/platform-admin/subscriptions/organizations/{organizationId}/change-plan`
  - `POST /api/platform-admin/subscriptions/organizations/{organizationId}/cancel`
- Catalog governance:
  - `GET /api/platform-admin/catalog/products?query=...&governanceStatus=...`
  - `GET /api/platform-admin/catalog/products/{productId}/impact`
  - `PUT /api/platform-admin/catalog/products/{productId}/governance`
  - `GET /api/platform-admin/incidents?status=...&subjectType=...&organizationId=...`
  - `POST /api/platform-admin/incidents`
  - `PUT /api/platform-admin/incidents/{incidentId}/status`
  - `POST /api/platform-admin/incidents/{incidentId}/apply-governance`
- Plans and features:
  - `GET /api/platform-admin/plans-features`
  - `POST /api/platform-admin/plans-features/plans`
  - `PUT /api/platform-admin/plans-features/plans/{planId}`
  - `PUT /api/platform-admin/plans-features/plans/{planId}/features`
- Store teams:
  - `GET /api/platform-admin/store-teams`
  - `GET /api/platform-admin/store-teams/{userId}`
  - `PUT /api/platform-admin/store-teams/{userId}`
  - `PUT /api/platform-admin/store-teams/{userId}/status`
- Support and oversight:
  - `GET /api/platform-admin/support-grievances`
  - `POST /api/platform-admin/support-grievances/service-tickets/{ticketId}/assign`
  - `POST /api/platform-admin/support-grievances/service-tickets/{ticketId}/close`
  - `POST /api/platform-admin/support-grievances/warranty-claims/{claimId}/status`
  - `GET /api/platform-admin/feedback`
  - `GET /api/platform-admin/notifications`
  - `GET /api/platform-admin/audit-activity`
  - `GET /api/platform-admin/system-health`
  - `GET /api/platform-admin/reports`
