# Subscriptions and Tax UX Design

## Outcomes

- make plan visibility and organization entitlement easy for owners to understand
- allow upgrade and change-plan actions without hiding gated features
- keep tax registration and GST threshold settings close to organization setup
- help UI consistently render permission gating and subscription gating differently

## Primary Users

- owner
- admin with limited visibility
- platform admin for central subscription operations

## Information Architecture

Primary navigation placement:
- owner-facing inside organization settings
- platform-facing inside platform admin subscriptions

Owner-side sections:
- current subscription
- plan comparison
- subscription history
- tax registrations
- GSTIN lookup
- GST compliance drafts
- GST threshold settings

## Screen Inventory

### Subscription Overview

Purpose:
- show current entitlement and renewal state

### Plan Comparison and Change

Purpose:
- help users compare plans before upgrading or changing

### Subscription History

Purpose:
- readable lifecycle history of plans and statuses

### Tax Registration List

Purpose:
- manage business tax registrations and GST identity

### Tax Registration Detail or Edit

Purpose:
- inspect and maintain registration information

### GSTIN Lookup

Purpose:
- fetch business identity from configured provider when user enters GSTIN

### GST Compliance Drafts

Purpose:
- let finance or compliance users preview, submit, and sync e-invoice and e-way bill payloads for posted invoices

### GST Threshold Settings

Purpose:
- settings-oriented screen or card for threshold monitoring

## List Page Blueprint

### Subscription History

Columns:
- plan name
- status
- start date
- end date
- auto renew
- notes

Filters:
- active only
- status
- date range later if needed

Top actions:
- change plan
- cancel subscription if allowed

Row actions:
- view summary

### Tax Registration List

Columns:
- registration type
- GSTIN
- state
- active
- effective from
- effective to

Filters:
- registration type
- active
- state
- search by GSTIN

Top actions:
- add registration

Row actions:
- view
- edit

## Detail Page Blueprint

### Subscription Overview

Header summary:
- current plan
- status
- starts on
- ends on
- auto renew

Key cards:
- organizations used
- allowed organizations
- feature entitlement summary
- renewal summary

Recommended feature access section:
- feature name
- available or locked status
- lock reason
- upgrade guidance

Actions:
- change plan
- cancel plan if business rules allow
- open history

### Plan Comparison

Best presented as pricing or comparison cards, not a dropdown.

Each plan card should show:
- plan name
- billing mode if available
- allowed organization count
- unlimited organizations flag
- notable features
- current plan badge or upgrade CTA

Important UX rule:
- locked features visible but clearly disabled
- show why a plan matters, not only technical limits

### Tax Registration Detail

Header summary:
- registration type
- GSTIN
- state
- active

Sections:
- business identity
- tax number details
- effective dates
- notes or remarks

### GST Threshold Settings

Recommended as a compact but informative card:
- threshold amount
- alert enabled
- current status from backend
- explanatory message

Actions:
- edit threshold config

## Create or Edit Flow Blueprint

### Change Plan Flow

Step 1 current plan:
- current plan summary
- current usage context

Step 2 compare:
- show available plans side by side

Step 3 choose:
- selected plan
- effect summary

Step 4 confirm:
- summary of new limits
- start date effect if returned by backend

### Tax Registration Create or Edit

Sections:
- registration type
- GSTIN
- state
- effective dates
- active state

GST lookup is now available:
- GSTIN input
- fetch details CTA
- prefill preview
- if provider is unavailable, show fallback helper that UI may continue with non-GST path

### GST Compliance Draft Flow

Entry points:
- tax module
- invoice detail

Recommended steps:
- choose invoice
- choose document type
- for e-way bill optionally capture transporter and movement info
- generate draft
- show payload and warnings
- if backend says `eligibleForSubmission=false`, disable submission CTA and show blockers

### GST Threshold Settings Edit

Fields:
- threshold amount
- threshold alert enabled

Confirmation:
- show current backend-evaluated threshold state after save if available

## State Handling

Subscription:
- active
- cancelled
- expired
- pending change later if implemented

Plan comparison:
- current plan
- locked upgrade path
- downgrade warning later

Tax registration:
- none configured
- active registration
- inactive historical registration
- invalid GSTIN format handled in UI before submit where possible

GST lookup:
- invalid format
- provider unavailable
- lookup response found
- lookup response not found

GST compliance:
- draft eligible
- draft blocked by missing seller GSTIN or invoice status
- submitted to provider
- generated by provider
- provider unavailable

Provider configuration note:
- backend can now resolve compliance through pluggable provider implementations
- an HTTP provider adapter exists so a real GST partner can be configured later without changing tax or invoice screens

Gating rules:
- permissions control whether user can edit
- subscription controls whether module or action is available
- use different visual messages for each case

## CTA Map

Subscription overview:
- primary: Change plan
- secondary: View history

Plan comparison:
- primary: Select plan
- secondary: Back to current summary

Tax registration list:
- primary: Add registration
- row primary: View
- row secondary: Edit

GST threshold settings:
- primary: Edit settings

GSTIN lookup:
- primary: Fetch details
- secondary: Use entered details manually

GST compliance draft:
- primary: Generate draft
- secondary: Open invoice

GST compliance detail:
- primary: Submit to provider
- secondary: Sync status

## Desktop and Mobile Notes

Desktop:
- plan comparison works best as card grid or comparison table
- tax registration can use standard list-detail pattern

Mobile:
- use stacked plan cards with sticky selected-plan footer
- keep threshold settings lightweight and form-first
- keep GST compliance payload collapsed by default with warnings first
- keep provider response collapsed behind a second accordion so warnings stay visible first

## Cross-Module Navigation

- subscription overview to organization settings
- subscription gating messages to locked modules
- tax registration to invoice and reporting contexts
- GST lookup to organization, customer, and supplier create flows
- GST compliance draft/detail to invoice detail and PDF/send actions
- platform admin subscription console to store detail

## Key Forms And Backend Fields

### Subscription Activate Or Change Plan

- `planCode`
- `status`
- `startsOn`
- `endsOn`
- `autoRenew`
- `notes`

### Subscription Cancel

- `endsOn`
- `notes`

### Tax Registration Create Or Edit

- `branchId`
- `registrationName`
- `legalName`
- `gstin`
- `registrationStateCode`
- `registrationStateName`
- `effectiveFrom`
- `effectiveTo`
- `isDefault`
- `isActive`

### GST Threshold Settings

- `gstThresholdAlertEnabled`

Threshold note:
- threshold amount is backend-derived
- this screen should edit only the alert preference and show the derived threshold status from backend

### GST Compliance Draft

- `transporterName`
- `transporterId`
- `transportMode`
- `vehicleNumber`
- `distanceKm`
- `dispatchAddress`
- `shipToAddress`
- `notes`

GST compliance response fields to render:
- `status`
- `eligibleForSubmission`
- `warnings`
- `providerCode`
- `providerName`
- `externalReference`
- `acknowledgementNumber`
- `acknowledgementDateTime`
- `generatedAt`
- `submittedAt`
- `lastSyncedAt`
- `payload`
- `providerResponse`

Provider behavior note:
- UI should not assume there is only one provider implementation
- render provider code and provider name from backend response for support and audit clarity

## Screen To API Map

- Plan comparison:
  - `GET /api/erp/subscriptions/plans`
- Current subscription:
  - `GET /api/erp/subscriptions/current?organizationId=...`
- Subscription history:
  - `GET /api/erp/subscriptions/history?organizationId=...`
- Activate or change plan:
  - `POST /api/erp/subscriptions/organizations/{organizationId}/activate`
  - `POST /api/erp/subscriptions/organizations/{organizationId}/change-plan`
- Cancel subscription:
  - `POST /api/erp/subscriptions/organizations/{organizationId}/cancel`
- Tax registration list:
  - `GET /api/erp/tax/registrations?organizationId=...&branchId=...&documentDate=...`
- Tax registration create or update:
  - `POST /api/erp/tax/registrations?organizationId=...`
  - `PUT /api/erp/tax/registrations/{registrationId}?organizationId=...`
- GSTIN lookup:
  - `GET /api/erp/tax/gstin-lookup?gstin=...`
- Invoice compliance drafts:
  - `GET /api/erp/tax/compliance/invoices/{invoiceId}/documents`
  - `GET /api/erp/tax/compliance/documents/{documentId}`
  - `POST /api/erp/tax/compliance/invoices/{invoiceId}/drafts/e-invoice`
  - `POST /api/erp/tax/compliance/invoices/{invoiceId}/drafts/e-way-bill`
  - `POST /api/erp/tax/compliance/documents/{documentId}/submit`
  - `POST /api/erp/tax/compliance/documents/{documentId}/sync-status`
- GST threshold:
  - `GET /api/erp/tax/threshold-status?organizationId=...&asOfDate=...`
  - `PUT /api/erp/tax/threshold-settings?organizationId=...`

## GST Compliance UI Notes

- show document status as a lifecycle badge: `DRAFT`, `BLOCKED`, `SUBMITTED`, `GENERATED`, `PROVIDER_UNAVAILABLE`
- disable submit when `eligibleForSubmission=false`
- warnings should be shown before raw payload
- payload is for review and troubleshooting, not direct user editing
- provider response should show acknowledgement number, timestamps, and sandbox/live messaging clearly
